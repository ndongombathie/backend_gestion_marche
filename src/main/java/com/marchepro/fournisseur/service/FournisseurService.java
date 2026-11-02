package com.marchepro.fournisseur.service;

import com.marchepro.common.audit.AuditService;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.fournisseur.dto.FournisseurDto;
import com.marchepro.fournisseur.dto.FournisseurUpdateRequest;
import com.marchepro.fournisseur.dto.ValidationFournisseurRequest;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public FournisseurService(FournisseurRepository fournisseurRepository,
                              UtilisateurRepository utilisateurRepository,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.fournisseurRepository = fournisseurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Fournisseur fournisseurCourant() {
        return fournisseurRepository.findByUtilisateurId(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Profil fournisseur introuvable"));
    }

    @Transactional(readOnly = true)
    public FournisseurDto monProfil() {
        return FournisseurDto.from(fournisseurCourant());
    }

    @Transactional
    public FournisseurDto mettreAJourProfil(FournisseurUpdateRequest request) {
        Fournisseur fournisseur = fournisseurCourant();
        fournisseur.setRaisonSociale(request.raisonSociale().trim());
        fournisseur.setRccm(request.rccm());
        fournisseur.setAdresse(request.adresse());
        fournisseur.setTelephone(request.telephone());
        fournisseur.setRepresentant(request.representant().trim());
        fournisseurRepository.save(fournisseur);
        auditService.enregistrer("MODIFICATION_FOURNISSEUR", "FOURNISSEUR", fournisseur.getId(),
                "Mise à jour du profil fournisseur");
        return FournisseurDto.from(fournisseur);
    }

    @Transactional(readOnly = true)
    public PageResponse<FournisseurDto> lister(StatutValidation statut, String q, Pageable pageable) {
        return PageResponse.from(fournisseurRepository.rechercher(statut, q, pageable), FournisseurDto::from);
    }

    @Transactional(readOnly = true)
    public List<FournisseurDto> enAttente() {
        return fournisseurRepository.rechercher(StatutValidation.EN_ATTENTE, null, Pageable.unpaged())
                .stream().map(FournisseurDto::from).toList();
    }

    @Transactional(readOnly = true)
    public FournisseurDto parId(Long id) {
        return FournisseurDto.from(fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable")));
    }

    @Transactional
    public FournisseurDto valider(Long id, ValidationFournisseurRequest request) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable"));
        if (request.statut() == StatutValidation.EN_ATTENTE) {
            throw new BusinessException("Le statut EN_ATTENTE ne peut pas être appliqué manuellement");
        }
        if (request.statut() == StatutValidation.REJETE
                && (request.motif() == null || request.motif().isBlank())) {
            throw new BusinessException("Un motif est obligatoire pour rejeter un fournisseur");
        }
        fournisseur.setStatutValidation(request.statut());
        fournisseur.setMotifRejet(request.statut() == StatutValidation.REJETE ? request.motif() : null);
        fournisseur.getUtilisateur().setStatut(
                request.statut() == StatutValidation.VALIDE ? StatutUtilisateur.ACTIF : StatutUtilisateur.INACTIF);
        utilisateurRepository.save(fournisseur.getUtilisateur());
        fournisseurRepository.save(fournisseur);

        String titre = request.statut() == StatutValidation.VALIDE
                ? "Compte fournisseur validé"
                : "Compte fournisseur rejeté";
        String message = request.statut() == StatutValidation.VALIDE
                ? "Votre compte " + fournisseur.getRaisonSociale() + " est validé. Vous pouvez désormais soumettre des offres."
                : "Votre demande d'inscription a été rejetée. Motif : " + request.motif();
        notificationService.envoyer(fournisseur.getUtilisateur(), titre, message, "VALIDATION", "/fournisseur");

        auditService.enregistrer("VALIDATION_FOURNISSEUR", "FOURNISSEUR", fournisseur.getId(),
                request.statut() + (request.motif() != null ? " - " + request.motif() : ""));
        return FournisseurDto.from(fournisseur);
    }
}
