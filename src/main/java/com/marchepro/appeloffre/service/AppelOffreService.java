package com.marchepro.appeloffre.service;

import com.marchepro.appeloffre.dto.AppelOffreDto;
import com.marchepro.appeloffre.dto.AppelOffreRequest;
import com.marchepro.appeloffre.dto.ProlongationRequest;
import com.marchepro.appeloffre.dto.PublicStatsDto;
import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.document.DocumentDto;
import com.marchepro.common.document.DocumentService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ConflictException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.marche.service.MarcheService;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class AppelOffreService {

    private final AppelOffreRepository appelOffreRepository;
    private final MarcheRepository marcheRepository;
    private final MarcheService marcheService;
    private final FournisseurRepository fournisseurRepository;
    private final OffreRepository offreRepository;
    private final AttributionRepository attributionRepository;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;
    private final String organisme;

    public AppelOffreService(AppelOffreRepository appelOffreRepository,
                             MarcheRepository marcheRepository,
                             MarcheService marcheService,
                             FournisseurRepository fournisseurRepository,
                             OffreRepository offreRepository,
                             AttributionRepository attributionRepository,
                             DocumentService documentService,
                             NotificationService notificationService,
                             UtilisateurRepository utilisateurRepository,
                             AuditService auditService,
                             @Value("${app.organisation.nom}") String organisme) {
        this.appelOffreRepository = appelOffreRepository;
        this.marcheRepository = marcheRepository;
        this.marcheService = marcheService;
        this.fournisseurRepository = fournisseurRepository;
        this.offreRepository = offreRepository;
        this.attributionRepository = attributionRepository;
        this.documentService = documentService;
        this.notificationService = notificationService;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
        this.organisme = organisme;
    }

    @Transactional
    public AppelOffreDto creer(AppelOffreRequest request) {
        if (request.marcheId() == null) {
            throw new BusinessException("Le marché associé est obligatoire");
        }
        Marche marche = marcheService.verifierEtChargerPourPublication(request.marcheId());
        if (appelOffreRepository.findByMarcheId(marche.getId()).isPresent()) {
            throw new ConflictException("Un appel d'offres existe déjà pour ce marché");
        }
        if (request.dateLimite().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La date limite doit être dans le futur");
        }
        AppelOffre appelOffre = new AppelOffre();
        appelOffre.setReference(genererReference());
        appelOffre.setMarche(marche);
        appliquer(appelOffre, request);
        appelOffre.setStatut(StatutAppelOffre.BROUILLON);
        appelOffreRepository.save(appelOffre);
        auditService.enregistrer("CREATION_APPEL_OFFRE", "APPEL_OFFRE", appelOffre.getId(),
                appelOffre.getReference());
        return mapper(appelOffre, false);
    }

    @Transactional
    public AppelOffreDto modifier(Long id, AppelOffreRequest request) {
        AppelOffre appelOffre = charger(id);
        if (appelOffre.getStatut() != StatutAppelOffre.BROUILLON) {
            throw new BusinessException("Seul un appel d'offres en brouillon peut être modifié");
        }
        appliquer(appelOffre, request);
        appelOffreRepository.save(appelOffre);
        auditService.enregistrer("MODIFICATION_APPEL_OFFRE", "APPEL_OFFRE", id, appelOffre.getReference());
        return mapper(appelOffre, false);
    }

    @Transactional
    public AppelOffreDto publier(Long id) {
        AppelOffre appelOffre = charger(id);
        if (appelOffre.getStatut() != StatutAppelOffre.BROUILLON) {
            throw new BusinessException("Cet appel d'offres est déjà publié ou annulé");
        }
        if (appelOffre.getDateLimite().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La date limite est dépassée, modifiez-la avant de publier");
        }
        marcheService.verifierEtChargerPourPublication(appelOffre.getMarche().getId());
        Utilisateur utilisateur = utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
        appelOffre.setStatut(StatutAppelOffre.PUBLIE);
        appelOffre.setDatePublication(java.time.Instant.now());
        appelOffre.setPubliePar(utilisateur);
        appelOffreRepository.save(appelOffre);
        marcheService.marquerPublie(appelOffre.getMarche());

        String message = "Nouvel appel d'offres publié : " + appelOffre.getReference()
                + " - " + appelOffre.getObjet() + ". Date limite : " + appelOffre.getDateLimite() + ".";
        for (Fournisseur fournisseur : fournisseurRepository.findByStatutValidation(StatutValidation.VALIDE)) {
            notificationService.envoyer(fournisseur.getUtilisateur(), "Nouvel appel d'offres",
                    message, "APPEL_OFFRE", "/opportunites/" + appelOffre.getId());
        }
        auditService.enregistrer("PUBLICATION_APPEL_OFFRE", "APPEL_OFFRE", id, appelOffre.getReference());
        return mapper(appelOffre, false);
    }

    @Transactional
    public AppelOffreDto prolonger(Long id, ProlongationRequest request) {
        AppelOffre appelOffre = charger(id);
        if (appelOffre.getStatut() != StatutAppelOffre.PUBLIE) {
            throw new BusinessException("Seul un appel d'offres publié peut être prolongé");
        }
        if (request.nouvelleDateLimite().isBefore(appelOffre.getDateLimite())) {
            throw new BusinessException("La nouvelle date limite doit être postérieure à l'actuelle");
        }
        if (request.nouvelleDateLimite().isBefore(LocalDateTime.now())) {
            throw new BusinessException("La nouvelle date limite doit être dans le futur");
        }
        LocalDateTime ancienne = appelOffre.getDateLimite();
        appelOffre.setDateLimite(request.nouvelleDateLimite());
        appelOffreRepository.save(appelOffre);
        for (Fournisseur fournisseur : fournisseurRepository.findByStatutValidation(StatutValidation.VALIDE)) {
            notificationService.envoyer(fournisseur.getUtilisateur(), "Prolongation de la date limite",
                    "La date limite de " + appelOffre.getReference() + " est reportée au "
                            + request.nouvelleDateLimite() + ". Justification : " + request.justification(),
                    "APPEL_OFFRE", "/opportunites/" + appelOffre.getId());
        }
        auditService.enregistrer("PROLONGATION_APPEL_OFFRE", "APPEL_OFFRE", id,
                "Ancienne date : " + ancienne + " - Nouvelle date : " + request.nouvelleDateLimite()
                        + " - Justification : " + request.justification());
        return mapper(appelOffre, false);
    }

    @Transactional
    public AppelOffreDto annuler(Long id, DecisionRequest request) {
        if (request.motif() == null || request.motif().isBlank()) {
            throw new BusinessException("Le motif d'annulation est obligatoire");
        }
        AppelOffre appelOffre = charger(id);
        if (appelOffre.getStatut() == StatutAppelOffre.ANNULE) {
            throw new BusinessException("Cet appel d'offres est déjà annulé");
        }
        if (appelOffre.getStatut() == StatutAppelOffre.CLOTURE) {
            throw new BusinessException("Un appel d'offres clôturé ne peut plus être annulé");
        }
        appelOffre.setStatut(StatutAppelOffre.ANNULE);
        appelOffre.setMotifAnnulation(request.motif());
        appelOffreRepository.save(appelOffre);
        auditService.enregistrer("ANNULATION_APPEL_OFFRE", "APPEL_OFFRE", id, request.motif());
        return mapper(appelOffre, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppelOffreDto> listerPublic(String q, String categorie, String organisme,
                                                     LocalDateTime dateLimiteApres, LocalDateTime dateLimiteAvant,
                                                     Pageable pageable) {
        return PageResponse.from(appelOffreRepository.rechercherPublics(
                normaliser(q), normaliser(categorie), normaliser(organisme),
                dateLimiteApres, dateLimiteAvant, pageable), a -> mapper(a, true));
    }

    @Transactional(readOnly = true)
    public AppelOffreDto parIdPublic(Long id) {
        AppelOffre appelOffre = charger(id);
        if (appelOffre.getStatut() != StatutAppelOffre.PUBLIE
                && appelOffre.getStatut() != StatutAppelOffre.CLOTURE) {
            throw new ResourceNotFoundException("Appel d'offres introuvable");
        }
        return mapper(appelOffre, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppelOffreDto> lister(StatutAppelOffre statut, String q, Pageable pageable) {
        return PageResponse.from(appelOffreRepository.rechercherTous(statut, normaliser(q), pageable),
                a -> mapper(a, false));
    }

    @Transactional(readOnly = true)
    public AppelOffreDto parId(Long id) {
        return mapper(charger(id), false);
    }

    @Transactional(readOnly = true)
    public List<AppelOffreDto> recents(int limite) {
        return appelOffreRepository.findRecents(LocalDateTime.now(), PageRequest.of(0, limite))
                .stream().map(a -> mapper(a, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<AppelOffreDto> bientotExpires(int limite) {
        return appelOffreRepository.findBientotExpires(LocalDateTime.now(),
                        LocalDateTime.now().plusDays(30), PageRequest.of(0, limite))
                .stream().map(a -> mapper(a, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<String> organismes() {
        return appelOffreRepository.listerOrganismes();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return appelOffreRepository.listerCategories();
    }

    @Transactional(readOnly = true)
    public PublicStatsDto statistiquesPublic() {
        return new PublicStatsDto(
                appelOffreRepository.countByStatut(StatutAppelOffre.PUBLIE),
                marcheRepository.count(),
                fournisseurRepository.countByStatutValidation(StatutValidation.VALIDE),
                attributionRepository.totalMontantAttribue(),
                categories(),
                recents(3),
                bientotExpires(3)
        );
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void cloturerAppelsOffresExpires() {
        List<AppelOffre> expires = appelOffreRepository.findByStatutAndDateLimiteBefore(
                StatutAppelOffre.PUBLIE, LocalDateTime.now());
        for (AppelOffre appelOffre : expires) {
            appelOffre.setStatut(StatutAppelOffre.CLOTURE);
            appelOffreRepository.save(appelOffre);
            auditService.enregistrer("CLOTURE_AUTOMATIQUE_APPEL_OFFRE", "APPEL_OFFRE",
                    appelOffre.getId(), "Date limite dépassée");
        }
    }

    @Transactional(readOnly = true)
    public AppelOffre charger(Long id) {
        return appelOffreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appel d'offres introuvable"));
    }

    public AppelOffreDto mapper(AppelOffre appelOffre, boolean vuePublique) {
        List<DocumentDto> documents = documentService.lister("APPEL_OFFRE", appelOffre.getId());
        long nombreOffres = vuePublique ? 0 : offreRepository.countByAppelOffreId(appelOffre.getId());
        return AppelOffreDto.from(appelOffre, nombreOffres, documents);
    }

    private void appliquer(AppelOffre appelOffre, AppelOffreRequest request) {
        appelOffre.setObjet(request.objet().trim());
        appelOffre.setDescription(request.description());
        appelOffre.setCategorie(request.categorie());
        appelOffre.setOrganisme(request.organisme() == null || request.organisme().isBlank()
                ? organisme : request.organisme().trim());
        appelOffre.setConditionsParticipation(request.conditionsParticipation());
        appelOffre.setDocumentsObligatoires(request.documentsObligatoires());
        appelOffre.setModalitesSoumission(request.modalitesSoumission());
        appelOffre.setBudgetPublic(request.budgetPublic());
        appelOffre.setDateLimite(request.dateLimite());
    }

    private String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private String genererReference() {
        int annee = Year.now().getValue();
        long compteur = appelOffreRepository.count() + 1;
        String reference;
        do {
            reference = String.format("AO-%d-%03d", annee, compteur++);
        } while (appelOffreRepository.findByReference(reference).isPresent());
        return reference;
    }
}
