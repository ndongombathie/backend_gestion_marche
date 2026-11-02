package com.marchepro.auth.service;

import com.marchepro.auth.dto.*;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ConflictException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.security.JwtService;
import com.marchepro.security.SecurityUtils;
import com.marchepro.security.UserPrincipal;
import com.marchepro.utilisateur.dto.UtilisateurDto;
import com.marchepro.utilisateur.dto.UtilisateurMapper;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UtilisateurMapper utilisateurMapper;
    private final AuditService auditService;
    private final long expirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UtilisateurRepository utilisateurRepository,
                       FournisseurRepository fournisseurRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UtilisateurMapper utilisateurMapper,
                       AuditService auditService,
                       @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.authenticationManager = authenticationManager;
        this.utilisateurRepository = utilisateurRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.utilisateurMapper = utilisateurMapper;
        this.auditService = auditService;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.motDePasse()));
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (utilisateur.getStatut() == StatutUtilisateur.INACTIF) {
            throw new BusinessException("Ce compte est désactivé. Contactez l'administrateur.", HttpStatus.FORBIDDEN);
        }
        utilisateur.setDerniereConnexion(Instant.now());
        utilisateurRepository.save(utilisateur);
        UserPrincipal principal = new UserPrincipal(utilisateur);
        String token = jwtService.genererToken(principal);
        Fournisseur fournisseur = fournisseurRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        auditService.enregistrer("CONNEXION", "UTILISATEUR", utilisateur.getId(), "Connexion réussie");
        return new LoginResponse(token, "Bearer", expirationMs, utilisateurMapper.toDto(utilisateur, fournisseur));
    }

    @Transactional
    public Map<String, Object> registerFournisseur(RegisterFournisseurRequest request) {
        if (!request.motDePasse().equals(request.confirmationMotDePasse())) {
            throw new BusinessException("Les mots de passe ne correspondent pas");
        }
        String email = request.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        if (fournisseurRepository.existsByNineaIgnoreCase(request.ninea().trim())) {
            throw new ConflictException("Un fournisseur existe déjà avec ce NINEA");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.representant().trim());
        utilisateur.setPrenom(request.raisonSociale().trim());
        utilisateur.setEmail(email);
        utilisateur.setTelephone(request.telephone());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setRole(Role.FOURNISSEUR);
        utilisateur.setStatut(StatutUtilisateur.EN_ATTENTE);
        utilisateurRepository.save(utilisateur);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setRaisonSociale(request.raisonSociale().trim());
        fournisseur.setNinea(request.ninea().trim());
        fournisseur.setRccm(request.rccm());
        fournisseur.setAdresse(request.adresse());
        fournisseur.setTelephone(request.telephone());
        fournisseur.setEmail(email);
        fournisseur.setRepresentant(request.representant().trim());
        fournisseur.setStatutValidation(StatutValidation.EN_ATTENTE);
        fournisseur.setUtilisateur(utilisateur);
        fournisseurRepository.save(fournisseur);

        auditService.enregistrer("INSCRIPTION_FOURNISSEUR", "FOURNISSEUR", fournisseur.getId(),
                "Demande d'inscription de " + fournisseur.getRaisonSociale());

        return Map.of(
                "message", "Votre demande d'inscription a été enregistrée. Elle sera examinée par l'administrateur.",
                "fournisseurId", fournisseur.getId(),
                "statut", fournisseur.getStatutValidation().name()
        );
    }

    @Transactional
    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte n'est associé à cet email"));
        String token = UUID.randomUUID().toString().replace("-", "");
        utilisateur.setResetToken(token);
        utilisateur.setResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
        utilisateurRepository.save(utilisateur);
        auditService.enregistrer("DEMANDE_REINITIALISATION", "UTILISATEUR", utilisateur.getId(),
                "Demande de réinitialisation du mot de passe");
        return Map.of(
                "message", "Un lien de réinitialisation a été généré. En mode démonstration, le token est affiché ci-dessous.",
                "resetToken", token,
                "expireDansMinutes", 60
        );
    }

    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByResetToken(request.token())
                .orElseThrow(() -> new BusinessException("Token de réinitialisation invalide"));
        if (utilisateur.getResetTokenExpiry() == null || utilisateur.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BusinessException("Ce token de réinitialisation a expiré");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(request.nouveauMotDePasse()));
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiry(null);
        utilisateurRepository.save(utilisateur);
        auditService.enregistrer("REINITIALISATION_MOT_DE_PASSE", "UTILISATEUR", utilisateur.getId(),
                "Mot de passe réinitialisé");
        return Map.of("message", "Votre mot de passe a été réinitialisé avec succès");
    }

    @Transactional(readOnly = true)
    public UtilisateurDto me() {
        Utilisateur utilisateur = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Fournisseur fournisseur = fournisseurRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        return utilisateurMapper.toDto(utilisateur, fournisseur);
    }

    @Transactional
    public UtilisateurDto updateProfile(UpdateProfileRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        String email = request.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(utilisateur.getEmail())
                && utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Cet email est déjà utilisé par un autre compte");
        }
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setTelephone(request.telephone());

        if (request.nouveauMotDePasse() != null && !request.nouveauMotDePasse().isBlank()) {
            if (request.motDePasseActuel() == null || request.motDePasseActuel().isBlank()) {
                throw new BusinessException("Le mot de passe actuel est requis pour changer de mot de passe");
            }
            if (!passwordEncoder.matches(request.motDePasseActuel(), utilisateur.getMotDePasse())) {
                throw new BusinessException("Le mot de passe actuel est incorrect");
            }
            utilisateur.setMotDePasse(passwordEncoder.encode(request.nouveauMotDePasse()));
        }
        utilisateurRepository.save(utilisateur);
        Fournisseur fournisseur = fournisseurRepository.findByUtilisateurId(utilisateur.getId()).orElse(null);
        if (fournisseur != null) {
            fournisseur.setEmail(email);
            fournisseur.setTelephone(request.telephone());
            fournisseurRepository.save(fournisseur);
        }
        auditService.enregistrer("MODIFICATION_PROFIL", "UTILISATEUR", utilisateur.getId(),
                "Mise à jour du profil");
        return utilisateurMapper.toDto(utilisateur, fournisseur);
    }
}
