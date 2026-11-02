package com.marchepro.offre.service;

import com.marchepro.appeloffre.dto.DocumentObligatoireSuiviDto;
import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.appeloffre.service.DocumentObligatoireService;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.document.DocumentDto;
import com.marchepro.common.document.DocumentService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ConflictException;
import com.marchepro.common.exception.ForbiddenException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.evaluation.repository.EvaluationRepository;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.service.FournisseurService;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.offre.dto.LigneOffreRequest;
import com.marchepro.offre.dto.OffreDto;
import com.marchepro.offre.dto.OffreRequest;
import com.marchepro.offre.entity.LigneOffre;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OffreService {

    private final OffreRepository offreRepository;
    private final AppelOffreRepository appelOffreRepository;
    private final FournisseurService fournisseurService;
    private final EvaluationRepository evaluationRepository;
    private final DocumentService documentService;
    private final DocumentObligatoireService documentObligatoireService;
    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    public OffreService(OffreRepository offreRepository,
                        AppelOffreRepository appelOffreRepository,
                        FournisseurService fournisseurService,
                        EvaluationRepository evaluationRepository,
                        DocumentService documentService,
                        DocumentObligatoireService documentObligatoireService,
                        NotificationService notificationService,
                        UtilisateurRepository utilisateurRepository,
                        AuditService auditService) {
        this.offreRepository = offreRepository;
        this.appelOffreRepository = appelOffreRepository;
        this.fournisseurService = fournisseurService;
        this.evaluationRepository = evaluationRepository;
        this.documentService = documentService;
        this.documentObligatoireService = documentObligatoireService;
        this.notificationService = notificationService;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
    }

    @Transactional
    public OffreDto creerBrouillon(OffreRequest request) {
        Fournisseur fournisseur = fournisseurService.fournisseurCourant();
        AppelOffre appelOffre = appelOffreRepository.findById(request.appelOffreId())
                .orElseThrow(() -> new ResourceNotFoundException("Appel d'offres introuvable"));
        if (appelOffre.getStatut() != StatutAppelOffre.PUBLIE) {
            throw new BusinessException("Cet appel d'offres n'est pas ouvert aux soumissions");
        }
        if (appelOffre.getDateLimite().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La date limite de soumission est dépassée");
        }
        if (offreRepository.findByAppelOffreIdAndFournisseurId(appelOffre.getId(), fournisseur.getId()).isPresent()) {
            throw new ConflictException("Vous avez déjà une offre pour cet appel d'offres");
        }

        Offre offre = new Offre();
        offre.setAppelOffre(appelOffre);
        offre.setFournisseur(fournisseur);
        appliquer(offre, request);
        offre.setStatut(StatutOffre.BROUILLON);
        offreRepository.save(offre);
        auditService.enregistrer("CREATION_OFFRE_BROUILLON", "OFFRE", offre.getId(),
                "Brouillon pour " + appelOffre.getReference());
        return mapper(offre);
    }

    @Transactional
    public OffreDto modifierBrouillon(Long id, OffreRequest request) {
        Offre offre = chargerAutorise(id);
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new BusinessException("Une offre soumise ne peut plus être modifiée");
        }
        appliquer(offre, request);
        offreRepository.save(offre);
        auditService.enregistrer("MODIFICATION_OFFRE", "OFFRE", id, "Brouillon mis à jour");
        return mapper(offre);
    }

    @Transactional
    public OffreDto soumettre(Long id) {
        Offre offre = chargerAutorise(id);
        Fournisseur fournisseur = offre.getFournisseur();
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new BusinessException("Cette offre a déjà été soumise");
        }
        if (fournisseur.getStatutValidation() != StatutValidation.VALIDE) {
            throw new ForbiddenException("Votre compte fournisseur doit être validé avant de soumettre une offre");
        }
        AppelOffre appelOffre = offre.getAppelOffre();
        if (appelOffre.getStatut() != StatutAppelOffre.PUBLIE) {
            throw new BusinessException("L'appel d'offres n'est plus ouvert");
        }
        if (appelOffre.getDateLimite().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La date limite de soumission est dépassée : l'offre est verrouillée");
        }
        if (offre.getMontant() == null || offre.getMontant().signum() <= 0) {
            throw new BusinessException("Le montant de l'offre doit être renseigné");
        }
        List<DocumentDto> documents = documentService.lister("OFFRE", offre.getId());
        documentObligatoireService.verifierCompletion(appelOffre.getId(), documents);
        verifierImagesMateriels(offre);

        offre.setReference(genererReference());
        offre.setReferenceAccuse("ACC-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        offre.setStatut(StatutOffre.SOUMISE);
        offre.setDateSoumission(java.time.Instant.now());
        offreRepository.save(offre);

        notificationService.envoyerAuRole(Role.MEMBRE_COMMISSION, "Nouvelle offre reçue",
                "L'offre " + offre.getReference() + " de " + fournisseur.getRaisonSociale()
                        + " a été soumise pour " + appelOffre.getReference() + ".",
                "OFFRE", "/commission/evaluations");
        notificationService.envoyer(fournisseur.getUtilisateur(), "Accusé de réception d'offre",
                "Votre offre " + offre.getReference() + " a bien été enregistrée. Accusé : "
                        + offre.getReferenceAccuse() + ".",
                "OFFRE", "/fournisseur/offres");
        auditService.enregistrer("SOUMISSION_OFFRE", "OFFRE", id,
                offre.getReference() + " - " + offre.getMontant() + " FCFA");
        return mapper(offre);
    }

    @Transactional
    public OffreDto disqualifier(Long id, DecisionRequest request) {
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("Le motif de disqualification est obligatoire");
        }
        Offre offre = charger(id);
        if (offre.getStatut() == StatutOffre.BROUILLON) {
            throw new BusinessException("Une offre en brouillon ne peut pas être disqualifiée");
        }
        if (offre.getStatut() == StatutOffre.RETENUE) {
            throw new BusinessException("Une offre retenue ne peut pas être disqualifiée");
        }
        offre.setStatut(StatutOffre.DISQUALIFIEE);
        offre.setMotifRejet(request.motif());
        offreRepository.save(offre);
        notificationService.envoyer(offre.getFournisseur().getUtilisateur(), "Offre disqualifiée",
                "Votre offre " + offre.getReference() + " a été disqualifiée. Motif : " + request.motif(),
                "OFFRE", "/fournisseur/offres");
        auditService.enregistrer("DISQUALIFICATION_OFFRE", "OFFRE", id, request.motif());
        return mapper(offre);
    }

    @Transactional(readOnly = true)
    public List<OffreDto> mesOffres() {
        Fournisseur fournisseur = fournisseurService.fournisseurCourant();
        return offreRepository.findByFournisseurIdOrderByDateCreationDesc(fournisseur.getId())
                .stream().map(this::mapper).toList();
    }

    @Transactional(readOnly = true)
    public List<OffreDto> offresParAppelOffre(Long appelOffreId) {
        return offreRepository.findByAppelOffreIdOrderByMontantAsc(appelOffreId)
                .stream().filter(o -> o.getStatut() != StatutOffre.BROUILLON)
                .map(this::mapper).toList();
    }

    @Transactional(readOnly = true)
    public OffreDto parId(Long id) {
        Offre offre = charger(id);
        if (SecurityUtils.hasRole(Role.FOURNISSEUR)) {
            Fournisseur fournisseur = fournisseurService.fournisseurCourant();
            if (!offre.getFournisseur().getId().equals(fournisseur.getId())) {
                throw new ForbiddenException("Cette offre ne vous appartient pas");
            }
        }
        return mapper(offre);
    }

    @Transactional(readOnly = true)
    public Offre charger(Long id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));
    }

    @Transactional
    public OffreDto ajouterImageLigne(Long offreId, Long ligneId, MultipartFile fichier) {
        Offre offre = chargerAutorise(offreId);
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new BusinessException("Les images des matériels ne peuvent être modifiées que sur un brouillon");
        }
        LigneOffre ligne = offre.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ligne d'offre introuvable"));
        documentService.stockerImage(fichier, "LIGNE_OFFRE", ligne.getId());
        auditService.enregistrer("UPLOAD_IMAGE_MATERIEL", "LIGNE_OFFRE", ligne.getId(),
                "Image ajoutée pour le matériel " + ligne.getDesignation());
        return mapper(offre);
    }

    public OffreDto mapper(Offre offre) {
        long evaluations = evaluationRepository.findByOffreId(offre.getId()).size();
        List<DocumentDto> documents = documentService.lister("OFFRE", offre.getId());
        List<DocumentObligatoireSuiviDto> documentsObligatoires =
                documentObligatoireService.suivre(offre.getAppelOffre().getId(), documents);
        Map<Long, DocumentDto> imagesParLigne = new HashMap<>();
        for (LigneOffre ligne : offre.getLignes()) {
            documentService.lister("LIGNE_OFFRE", ligne.getId()).stream().findFirst()
                    .ifPresent(image -> imagesParLigne.put(ligne.getId(), image));
        }
        return OffreDto.from(offre, evaluations, documents, documentsObligatoires, imagesParLigne);
    }

    private Offre chargerAutorise(Long id) {
        Offre offre = charger(id);
        Fournisseur fournisseur = fournisseurService.fournisseurCourant();
        if (!offre.getFournisseur().getId().equals(fournisseur.getId())) {
            throw new ForbiddenException("Cette offre ne vous appartient pas");
        }
        return offre;
    }

    private void appliquer(Offre offre, OffreRequest request) {
        offre.setMontant(request.montant());
        offre.setDelaiLivraisonJours(request.delaiLivraisonJours());
        offre.setInformationsTechniques(request.informationsTechniques());
        offre.setObservationsFournisseur(request.observationsFournisseur());

        List<LigneOffre> lignesExistantes = new ArrayList<>(offre.getLignes());
        if (request.lignes() != null) {
            for (LigneOffreRequest ligneRequest : request.lignes()) {
                LigneOffre ligne = retirerLigneCorrespondante(lignesExistantes, ligneRequest);
                if (ligne == null) {
                    ligne = new LigneOffre();
                    offre.ajouterLigne(ligne);
                }
                ligne.setDesignation(ligneRequest.designation().trim());
                ligne.setQuantite(ligneRequest.quantite());
                ligne.setPrixUnitaire(ligneRequest.prixUnitaire());
                ligne.setCaracteristiques(ligneRequest.caracteristiques());
            }
        }
        offre.getLignes().removeAll(lignesExistantes);
    }

    private LigneOffre retirerLigneCorrespondante(List<LigneOffre> lignes, LigneOffreRequest request) {
        LigneOffre ligne = null;
        if (request.id() != null) {
            ligne = lignes.stream().filter(l -> l.getId().equals(request.id())).findFirst().orElse(null);
        }
        if (ligne == null) {
            ligne = lignes.stream()
                    .filter(l -> l.getDesignation().equalsIgnoreCase(request.designation().trim()))
                    .findFirst().orElse(null);
        }
        if (ligne != null) {
            lignes.remove(ligne);
        }
        return ligne;
    }

    private void verifierImagesMateriels(Offre offre) {
        if (offre.getLignes().isEmpty()) {
            throw new BusinessException("L'offre doit contenir au moins un matériel");
        }
        List<String> sansImage = offre.getLignes().stream()
                .filter(ligne -> documentService.lister("LIGNE_OFFRE", ligne.getId()).isEmpty())
                .map(LigneOffre::getDesignation)
                .toList();
        if (!sansImage.isEmpty()) {
            throw new BusinessException("Une image est obligatoire pour chaque matériel. "
                    + "Images manquantes : " + String.join(", ", sansImage));
        }
    }

    private String genererReference() {
        int annee = Year.now().getValue();
        long compteur = offreRepository.count() + 1;
        String reference;
        do {
            reference = String.format("OFF-%d-%03d", annee, compteur++);
        } while (offreRepository.existsByReference(reference));
        return reference;
    }
}
