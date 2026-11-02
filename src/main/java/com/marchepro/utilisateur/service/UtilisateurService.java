package com.marchepro.utilisateur.service;

import com.marchepro.common.audit.AuditService;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ConflictException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.dto.CreateUtilisateurRequest;
import com.marchepro.utilisateur.dto.UtilisateurDto;
import com.marchepro.utilisateur.dto.UtilisateurMapper;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurMapper utilisateurMapper;
    private final AuditService auditService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              FournisseurRepository fournisseurRepository,
                              PasswordEncoder passwordEncoder,
                              UtilisateurMapper utilisateurMapper,
                              AuditService auditService) {
        this.utilisateurRepository = utilisateurRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.passwordEncoder = passwordEncoder;
        this.utilisateurMapper = utilisateurMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurDto> lister(Role role, StatutUtilisateur statut, String q, Pageable pageable) {
        return PageResponse.from(utilisateurRepository.rechercher(role, statut, q, pageable), u ->
                utilisateurMapper.toDto(u, fournisseurRepository.findByUtilisateurId(u.getId()).orElse(null)));
    }

    @Transactional(readOnly = true)
    public UtilisateurDto parId(Long id) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return utilisateurMapper.toDto(utilisateur,
                fournisseurRepository.findByUtilisateurId(id).orElse(null));
    }

    @Transactional
    public UtilisateurDto creer(CreateUtilisateurRequest request) {
        if (request.role() == Role.FOURNISSEUR) {
            throw new BusinessException("Les comptes fournisseurs sont créés via le formulaire d'inscription public");
        }
        String email = request.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setTelephone(request.telephone());
        utilisateur.setRole(request.role());
        utilisateur.setStatut(StatutUtilisateur.ACTIF);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateurRepository.save(utilisateur);
        auditService.enregistrer("CREATION_UTILISATEUR", "UTILISATEUR", utilisateur.getId(),
                "Création du compte " + utilisateur.getEmail() + " (" + utilisateur.getRole() + ")");
        return utilisateurMapper.toDto(utilisateur);
    }

    @Transactional
    public UtilisateurDto changerStatut(Long id, StatutUtilisateur statut) {
        if (id.equals(SecurityUtils.currentUserId()) && statut != StatutUtilisateur.ACTIF) {
            throw new BusinessException("Vous ne pouvez pas désactiver votre propre compte");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (utilisateur.getRole() == Role.FOURNISSEUR) {
            throw new BusinessException("Le statut des fournisseurs se gère depuis la validation des inscriptions");
        }
        utilisateur.setStatut(statut);
        utilisateurRepository.save(utilisateur);
        auditService.enregistrer("CHANGEMENT_STATUT_UTILISATEUR", "UTILISATEUR", id, "Nouveau statut : " + statut);
        return utilisateurMapper.toDto(utilisateur);
    }
}
