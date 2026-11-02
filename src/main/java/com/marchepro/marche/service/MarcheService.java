package com.marchepro.marche.service;

import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.attribution.entity.Attribution;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.besoin.entity.ExpressionBesoin;
import com.marchepro.besoin.entity.StatutBesoin;
import com.marchepro.besoin.repository.ExpressionBesoinRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ConflictException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.service.FournisseurService;
import com.marchepro.marche.dto.*;
import com.marchepro.marche.entity.*;
import com.marchepro.livraison.entity.StatutLivraison;
import com.marchepro.livraison.repository.LivraisonRepository;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.List;

@Service
public class MarcheService {

    private final MarcheRepository marcheRepository;
    private final ExpressionBesoinRepository besoinRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AppelOffreRepository appelOffreRepository;
    private final AttributionRepository attributionRepository;
    private final OffreRepository offreRepository;
    private final LivraisonRepository livraisonRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final FournisseurService fournisseurService;

    public MarcheService(MarcheRepository marcheRepository,
                         ExpressionBesoinRepository besoinRepository,
                         UtilisateurRepository utilisateurRepository,
                         AppelOffreRepository appelOffreRepository,
                         AttributionRepository attributionRepository,
                         OffreRepository offreRepository,
                         LivraisonRepository livraisonRepository,
                         NotificationService notificationService,
                         AuditService auditService,
                         FournisseurService fournisseurService) {
        this.marcheRepository = marcheRepository;
        this.besoinRepository = besoinRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.appelOffreRepository = appelOffreRepository;
        this.attributionRepository = attributionRepository;
        this.offreRepository = offreRepository;
        this.livraisonRepository = livraisonRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.fournisseurService = fournisseurService;
    }

    @Transactional
    public MarcheDto creer(MarcheRequest request) {
        Utilisateur createur = utilisateurRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        ExpressionBesoin besoin = null;
        if (request.expressionBesoinId() != null) {
            besoin = besoinRepository.findById(request.expressionBesoinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expression de besoin introuvable"));
            if (besoin.getStatut() != StatutBesoin.VALIDEE) {
                throw new BusinessException("Seule une expression de besoin validée peut servir de base à un marché");
            }
            if (marcheRepository.findByExpressionBesoinId(besoin.getId()).isPresent()) {
                throw new ConflictException("Un marché existe déjà pour cette expression de besoin");
            }
        }

        Marche marche = new Marche();
        marche.setReference(genererReference());
        appliquer(marche, request);
        marche.setExpressionBesoin(besoin);
        marche.setCreateur(createur);
        marche.setStatut(StatutMarche.EN_PREPARATION);
        marcheRepository.save(marche);
        auditService.enregistrer("CREATION_MARCHE", "MARCHE", marche.getId(), marche.getReference());
        return mapper(marche);
    }

    @Transactional
    public MarcheDto modifier(Long id, MarcheRequest request) {
        Marche marche = charger(id);
        if (marche.getStatut() != StatutMarche.EN_PREPARATION || marche.getDateValidation() != null) {
            throw new BusinessException("Un marché déjà validé ou publié ne peut plus être modifié");
        }
        if (request.expressionBesoinId() != null
                && (marche.getExpressionBesoin() == null
                || !request.expressionBesoinId().equals(marche.getExpressionBesoin().getId()))) {
            ExpressionBesoin besoin = besoinRepository.findById(request.expressionBesoinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expression de besoin introuvable"));
            if (besoin.getStatut() != StatutBesoin.VALIDEE) {
                throw new BusinessException("Seule une expression de besoin validée peut être associée");
            }
            marche.setExpressionBesoin(besoin);
        }
        appliquer(marche, request);
        marcheRepository.save(marche);
        auditService.enregistrer("MODIFICATION_MARCHE", "MARCHE", id, marche.getReference());
        return mapper(marche);
    }

    @Transactional
    public MarcheDto soumettre(Long id) {
        Marche marche = charger(id);
        if (marche.getStatut() != StatutMarche.EN_PREPARATION || marche.getDateValidation() != null) {
            throw new BusinessException("Ce marché a déjà été soumis ou validé");
        }
        System.out.println("la marche est en validation");
        verifierCompletude(marche);
        marche.setStatut(StatutMarche.EN_VALIDATION);
        System.out.println("etat de la marché"+marche.getStatut());
        marcheRepository.save(marche);
        notificationService.envoyerAuRole(Role.DIRECTEUR, "Marché à valider",
                marche.getReference() + " - " + marche.getObjet() + " est en attente de validation.",
                "MARCHE", "/directeur/marches");
        auditService.enregistrer("SOUMISSION_MARCHE", "MARCHE", id, marche.getReference());
        return mapper(marche);
    }

    @Transactional
    public MarcheDto valider(Long id, DecisionRequest request) {
        Marche marche = charger(id);
        if (marche.getStatut() != StatutMarche.EN_VALIDATION) {
            throw new BusinessException("Ce marché n'est pas en attente de validation");
        }
        verifierCompletude(marche);
        marche.setDateValidation(Instant.now());
        marche.setStatut(StatutMarche.EN_PREPARATION);
        marcheRepository.save(marche);
        notificationService.envoyer(marche.getCreateur(), "Marché validé",
                marche.getReference() + " est validé. Vous pouvez créer et publier l'appel d'offres.",
                "MARCHE", "/marches/" + marche.getId());
        auditService.enregistrer("VALIDATION_MARCHE", "MARCHE", id, marche.getReference());
        return mapper(marche);
    }

    @Transactional
    public MarcheDto rejeter(Long id, DecisionRequest request) {
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        Marche marche = charger(id);
        if (marche.getStatut() != StatutMarche.EN_VALIDATION) {
            throw new BusinessException("Ce marché n'est pas en attente de validation");
        }
        marche.setStatut(StatutMarche.EN_PREPARATION);
        marche.setObservations(request.motif());
        marcheRepository.save(marche);
        notificationService.envoyer(marche.getCreateur(), "Marché rejeté",
                marche.getReference() + " a été rejeté. Motif : " + request.motif(),
                "MARCHE", "/marches/" + marche.getId());
        auditService.enregistrer("REJET_MARCHE", "MARCHE", id, request.motif());
        return mapper(marche);
    }

    @Transactional
    public MarcheDto annuler(Long id, DecisionRequest request) {
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("Le motif d'annulation est obligatoire");
        }
        Marche marche = charger(id);
        if (marche.getStatut() == StatutMarche.CLOTURE || marche.getStatut() == StatutMarche.ANNULE) {
            throw new BusinessException("Ce marché ne peut plus être annulé");
        }
        marche.setStatut(StatutMarche.ANNULE);
        marche.setMotifAnnulation(request.motif());
        marcheRepository.save(marche);
        notificationService.envoyer(marche.getCreateur(), "Marché annulé",
                marche.getReference() + " a été annulé. Motif : " + request.motif(),
                "MARCHE", "/marches/" + marche.getId());
        auditService.enregistrer("ANNULATION_MARCHE", "MARCHE", id, request.motif());
        return mapper(marche);
    }

    @Transactional
    public MarcheDto cloturer(Long id) {
        Marche marche = charger(id);
        if (marche.getStatut() != StatutMarche.EN_EXECUTION && marche.getStatut() != StatutMarche.ATTRIBUE) {
            throw new BusinessException("Seul un marché attribué ou en exécution peut être clôturé");
        }
        boolean livraisonsEnAttente = livraisonRepository.findByMarcheIdOrderByDatePrevueAsc(id).stream()
                .anyMatch(l -> l.getStatut() != StatutLivraison.RECEPTIONNEE);
        if (livraisonsEnAttente) {
            throw new BusinessException("Des livraisons sont encore en attente de réception");
        }
        marche.setStatut(StatutMarche.CLOTURE);
        marche.setDateCloture(Instant.now());
        marcheRepository.save(marche);
        auditService.enregistrer("CLOTURE_MARCHE", "MARCHE", id, marche.getReference());
        return mapper(marche);
    }

    @Transactional(readOnly = true)
    public PageResponse<MarcheDto> lister(StatutMarche statut, String q, Instant debut, Instant fin,
                                          Pageable pageable) {
        return PageResponse.from(marcheRepository.rechercher(statut, q, debut, fin, pageable), this::mapper);
    }

    @Transactional(readOnly = true)
    public MarcheDto parId(Long id) {
        return mapper(charger(id));
    }

    @Transactional(readOnly = true)
    public List<MarcheDto> mesMarches() {
        Fournisseur fournisseur = fournisseurService.fournisseurCourant();
        return attributionRepository.findByFournisseurIdAndStatut(fournisseur.getId(), StatutAttribution.VALIDEE)
                .stream()
                .map(Attribution::getMarche)
                .distinct()
                .map(this::mapper)
                .toList();
    }

    @Transactional(readOnly = true)
    public Marche charger(Long id) {
        return marcheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marché introuvable"));
    }

    @Transactional(readOnly = true)
    public Marche verifierEtChargerPourPublication(Long id) {
        Marche marche = charger(id);
        if (marche.getStatut() == StatutMarche.ANNULE || marche.getStatut() == StatutMarche.CLOTURE) {
            throw new BusinessException("Ce marché est annulé ou clôturé");
        }
        if (marche.getDateValidation() == null) {
            throw new BusinessException("Le marché doit être validé par le directeur avant publication");
        }
        verifierCompletude(marche);
        return marche;
    }

    @Transactional
    public void marquerPublie(Marche marche) {
        marche.setStatut(StatutMarche.PUBLIE);
        marche.setDatePublication(Instant.now());
        marcheRepository.save(marche);
    }

    @Transactional
    public void marquerEnEvaluation(Marche marche) {
        if (marche.getStatut() == StatutMarche.PUBLIE) {
            marche.setStatut(StatutMarche.EN_EVALUATION);
            marcheRepository.save(marche);
        }
    }

    @Transactional
    public void marquerAttribue(Marche marche) {
        marche.setStatut(StatutMarche.ATTRIBUE);
        marcheRepository.save(marche);
    }

    @Transactional
    public void marquerEnExecution(Marche marche) {
        if (marche.getStatut() == StatutMarche.ATTRIBUE || marche.getStatut() == StatutMarche.EN_EXECUTION) {
            marche.setStatut(StatutMarche.EN_EXECUTION);
            marcheRepository.save(marche);
        }
    }

    private void verifierCompletude(Marche marche) {
        if (marche.getLignes().isEmpty()) {
            throw new BusinessException("Le marché doit contenir au moins une ligne de matériel");
        }
        if (marche.getBudgetPrevisionnel() == null || marche.getBudgetPrevisionnel().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le budget prévisionnel doit être renseigné");
        }
        if (marche.getCriteres().isEmpty()) {
            throw new BusinessException("Au moins un critère d'évaluation est requis");
        }
        double total = marche.getCriteres().stream().mapToDouble(CritereEvaluation::getPonderation).sum();
        if (total <= 0) {
            throw new BusinessException("La somme des pondérations des critères doit être positive");
        }
    }

    private void appliquer(Marche marche, MarcheRequest request) {
        marche.setObjet(request.objet().trim());
        marche.setDescription(request.description());
        marche.setBudgetPrevisionnel(request.budgetPrevisionnel());
        marche.setModePassation(request.modePassation());
        marche.setDateDebutExecution(request.dateDebutExecution());
        marche.setDateFinExecution(request.dateFinExecution());
        marche.setObservations(request.observations());

        marche.getLignes().clear();
        for (LigneMarcheRequest ligneRequest : request.lignes()) {
            LigneMarche ligne = new LigneMarche();
            ligne.setDesignation(ligneRequest.designation().trim());
            ligne.setQuantite(ligneRequest.quantite());
            ligne.setCaracteristiques(ligneRequest.caracteristiques());
            ligne.setPrixUnitaireEstime(ligneRequest.prixUnitaireEstime());
            marche.ajouterLigne(ligne);
        }

        marche.getCriteres().clear();
        if (request.criteres() != null) {
            for (CritereRequest critereRequest : request.criteres()) {
                CritereEvaluation critere = new CritereEvaluation();
                critere.setLibelle(critereRequest.libelle().trim());
                critere.setType(critereRequest.type());
                critere.setPonderation(critereRequest.ponderation());
                critere.setDescription(critereRequest.description());
                marche.ajouterCritere(critere);
            }
        }
    }

    public MarcheDto mapper(Marche marche) {
        var appelOffre = appelOffreRepository.findByMarcheId(marche.getId()).orElse(null);
        Attribution attribution = attributionRepository.findByMarcheId(marche.getId())
                .filter(a -> a.getStatut() == StatutAttribution.VALIDEE).orElse(null);
        long nbOffres = appelOffre != null ? offreRepository.countByAppelOffreId(appelOffre.getId()) : 0;
        return MarcheDto.from(
                marche,
                appelOffre != null ? appelOffre.getId() : null,
                appelOffre != null ? appelOffre.getReference() : null,
                attribution != null ? attribution.getId() : null,
                attribution != null ? attribution.getFournisseur().getRaisonSociale() : null,
                attribution != null ? attribution.getMontant() : null,
                nbOffres
        );
    }

    private String genererReference() {
        int annee = Year.now().getValue();
        long compteur = marcheRepository.count() + 1;
        String reference;
        do {
            reference = String.format("MAR-%d-%03d", annee, compteur++);
        } while (marcheRepository.findByReference(reference).isPresent());
        return reference;
    }
}
