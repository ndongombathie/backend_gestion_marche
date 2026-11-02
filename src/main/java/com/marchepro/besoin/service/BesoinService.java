package com.marchepro.besoin.service;

import com.marchepro.besoin.dto.*;
import com.marchepro.besoin.entity.ExpressionBesoin;
import com.marchepro.besoin.entity.LigneBesoin;
import com.marchepro.besoin.entity.StatutBesoin;
import com.marchepro.besoin.repository.ExpressionBesoinRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ForbiddenException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;

@Service
public class BesoinService {

    private final ExpressionBesoinRepository besoinRepository;
    private final MarcheRepository marcheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public BesoinService(ExpressionBesoinRepository besoinRepository,
                         MarcheRepository marcheRepository,
                         UtilisateurRepository utilisateurRepository,
                         NotificationService notificationService,
                         AuditService auditService) {
        this.besoinRepository = besoinRepository;
        this.marcheRepository = marcheRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public ExpressionBesoinDto creer(ExpressionBesoinRequest request) {
        Utilisateur createur = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        ExpressionBesoin besoin = new ExpressionBesoin();
        besoin.setReference(genererReference());
        appliquer(besoin, request);
        besoin.setCreateur(createur);
        besoin.setStatut(StatutBesoin.BROUILLON);
        besoinRepository.save(besoin);
        auditService.enregistrer("CREATION_BESOIN", "BESOIN", besoin.getId(), besoin.getReference());
        return mapper(besoin);
    }

    @Transactional
    public ExpressionBesoinDto modifier(Long id, ExpressionBesoinRequest request) {
        ExpressionBesoin besoin = chargerAutorise(id);
        if (besoin.getStatut() != StatutBesoin.BROUILLON && besoin.getStatut() != StatutBesoin.REJETEE) {
            throw new BusinessException("Seul un brouillon ou une expression rejetée peut être modifié");
        }
        appliquer(besoin, request);
        besoin.setMotifRejet(null);
        besoinRepository.save(besoin);
        auditService.enregistrer("MODIFICATION_BESOIN", "BESOIN", id, besoin.getReference());
        return mapper(besoin);
    }

    @Transactional
    public ExpressionBesoinDto soumettre(Long id) {
        ExpressionBesoin besoin = chargerAutorise(id);
        if (besoin.getStatut() != StatutBesoin.BROUILLON && besoin.getStatut() != StatutBesoin.REJETEE) {
            throw new BusinessException("Cette expression de besoin a déjà été soumise");
        }
        if (besoin.getLignes().isEmpty()) {
            throw new BusinessException("Ajoutez au moins un matériel avant de soumettre");
        }
        besoin.setStatut(StatutBesoin.SOUMISE);
        besoin.setDateSoumission(Instant.now());
        besoin.setMotifRejet(null);
        besoinRepository.save(besoin);
        notificationService.envoyerAuRole(Role.DIRECTEUR, "Nouvelle expression de besoin",
                besoin.getReference() + " - " + besoin.getObjet() + " est en attente de validation.",
                "BESOIN", "/directeur/besoins");
        auditService.enregistrer("SOUMISSION_BESOIN", "BESOIN", id, besoin.getReference());
        return mapper(besoin);
    }

    @Transactional
    public ExpressionBesoinDto valider(Long id, DecisionRequest request) {
        ExpressionBesoin besoin = charger(id);
        if (besoin.getStatut() != StatutBesoin.SOUMISE && besoin.getStatut() != StatutBesoin.EN_VALIDATION) {
            throw new BusinessException("Cette expression de besoin n'est pas en attente de validation");
        }
        besoin.setStatut(StatutBesoin.VALIDEE);
        besoin.setDateValidation(Instant.now());
        besoin.setMotifRejet(null);
        besoinRepository.save(besoin);
        notificationService.envoyer(besoin.getCreateur(), "Expression de besoin validée",
                besoin.getReference() + " a été validée. Un marché peut désormais être créé.",
                "BESOIN", "/responsable/besoins/" + besoin.getId());
        auditService.enregistrer("VALIDATION_BESOIN", "BESOIN", id, besoin.getReference());
        return mapper(besoin);
    }

    @Transactional
    public ExpressionBesoinDto rejeter(Long id, DecisionRequest request) {
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        ExpressionBesoin besoin = charger(id);
        if (besoin.getStatut() != StatutBesoin.SOUMISE && besoin.getStatut() != StatutBesoin.EN_VALIDATION) {
            throw new BusinessException("Cette expression de besoin n'est pas en attente de validation");
        }
        besoin.setStatut(StatutBesoin.REJETEE);
        besoin.setMotifRejet(request.motif());
        besoinRepository.save(besoin);
        notificationService.envoyer(besoin.getCreateur(), "Expression de besoin rejetée",
                besoin.getReference() + " a été rejetée. Motif : " + request.motif(),
                "BESOIN", "/responsable/besoins/" + besoin.getId());
        auditService.enregistrer("REJET_BESOIN", "BESOIN", id, request.motif());
        return mapper(besoin);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpressionBesoinDto> lister(StatutBesoin statut, String q, Pageable pageable) {
        Long createurId = SecurityUtils.hasRole(Role.RESPONSABLE_SERVICE) ? SecurityUtils.currentUserId() : null;
        return PageResponse.from(besoinRepository.rechercher(createurId, statut, q, pageable), this::mapper);
    }

    @Transactional(readOnly = true)
    public List<ExpressionBesoinDto> validees() {
        return besoinRepository.findValidees(Pageable.unpaged()).stream().map(this::mapper).toList();
    }

    @Transactional(readOnly = true)
    public ExpressionBesoinDto parId(Long id) {
        ExpressionBesoin besoin = charger(id);
        if (SecurityUtils.hasRole(Role.RESPONSABLE_SERVICE)
                && !besoin.getCreateur().getId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Cette expression de besoin ne vous appartient pas");
        }
        return mapper(besoin);
    }

    private ExpressionBesoin charger(Long id) {
        return besoinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expression de besoin introuvable"));
    }

    private ExpressionBesoin chargerAutorise(Long id) {
        ExpressionBesoin besoin = charger(id);
        if (!besoin.getCreateur().getId().equals(SecurityUtils.currentUserId())
                && !SecurityUtils.hasRole(Role.ADMINISTRATEUR)) {
            throw new ForbiddenException("Cette expression de besoin ne vous appartient pas");
        }
        return besoin;
    }

    private void appliquer(ExpressionBesoin besoin, ExpressionBesoinRequest request) {
        besoin.setObjet(request.objet().trim());
        besoin.setDescription(request.description());
        besoin.setServiceDemandeur(request.serviceDemandeur().trim());
        besoin.setBudgetEstimatif(request.budgetEstimatif());
        besoin.setDateSouhaitee(request.dateSouhaitee());
        besoin.setJustification(request.justification());
        besoin.getLignes().clear();
        for (LigneBesoinRequest ligneRequest : request.lignes()) {
            LigneBesoin ligne = new LigneBesoin();
            ligne.setDesignation(ligneRequest.designation().trim());
            ligne.setQuantite(ligneRequest.quantite());
            ligne.setCaracteristiques(ligneRequest.caracteristiques());
            besoin.ajouterLigne(ligne);
        }
    }

    private ExpressionBesoinDto mapper(ExpressionBesoin besoin) {
        Long marcheId = marcheRepository.findByExpressionBesoinId(besoin.getId())
                .map(m -> m.getId()).orElse(null);
        return ExpressionBesoinDto.from(besoin, marcheId);
    }

    private String genererReference() {
        int annee = Year.now().getValue();
        long compteur = besoinRepository.count() + 1;
        String reference;
        do {
            reference = String.format("EB-%d-%03d", annee, compteur++);
        } while (besoinRepository.findByReference(reference).isPresent());
        return reference;
    }
}
