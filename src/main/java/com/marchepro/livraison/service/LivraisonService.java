package com.marchepro.livraison.service;

import com.marchepro.attribution.entity.Attribution;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.document.DocumentDto;
import com.marchepro.common.document.DocumentService;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ForbiddenException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.service.FournisseurService;
import com.marchepro.livraison.dto.DeclarationLivraisonRequest;
import com.marchepro.livraison.dto.LigneLivraisonRequest;
import com.marchepro.livraison.dto.LivraisonDto;
import com.marchepro.livraison.dto.LivraisonRequest;
import com.marchepro.livraison.dto.ReceptionRequest;
import com.marchepro.livraison.entity.*;
import com.marchepro.livraison.repository.LivraisonRepository;
import com.marchepro.marche.entity.LigneMarche;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.entity.StatutMarche;
import com.marchepro.marche.service.MarcheService;
import com.marchepro.notification.service.NotificationService;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LivraisonService {

    private final LivraisonRepository livraisonRepository;
    private final AttributionRepository attributionRepository;
    private final MarcheService marcheService;
    private final FournisseurService fournisseurService;
    private final DocumentService documentService;
    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    public LivraisonService(LivraisonRepository livraisonRepository,
                            AttributionRepository attributionRepository,
                            MarcheService marcheService,
                            FournisseurService fournisseurService,
                            DocumentService documentService,
                            NotificationService notificationService,
                            UtilisateurRepository utilisateurRepository,
                            AuditService auditService) {
        this.livraisonRepository = livraisonRepository;
        this.attributionRepository = attributionRepository;
        this.marcheService = marcheService;
        this.fournisseurService = fournisseurService;
        this.documentService = documentService;
        this.notificationService = notificationService;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LivraisonDto creer(LivraisonRequest request) {
        Marche marche = marcheService.charger(request.marcheId());
        Attribution attribution = attributionRepository.findByMarcheId(marche.getId())
                .filter(a -> a.getStatut() == StatutAttribution.VALIDEE)
                .orElseThrow(() -> new BusinessException("Ce marché n'a pas encore été attribué"));
        Fournisseur fournisseur = attribution.getFournisseur();
        if (SecurityUtils.hasRole(Role.FOURNISSEUR)) {
            Fournisseur courant = fournisseurService.fournisseurCourant();
            if (!courant.getId().equals(fournisseur.getId())) {
                throw new ForbiddenException("Ce marché ne vous est pas attribué");
            }
        }
        if (marche.getStatut() != StatutMarche.ATTRIBUE && marche.getStatut() != StatutMarche.EN_EXECUTION) {
            throw new BusinessException("Ce marché n'est pas en cours d'exécution");
        }
        if (request.datePrevue().isBefore(LocalDate.now())) {
            throw new BusinessException("La date prévue de livraison ne peut pas être dans le passé");
        }

        Map<Long, Integer> dejaLivres = quantitesDejaLivrees(marche.getId());
        Livraison livraison = new Livraison();
        livraison.setMarche(marche);
        livraison.setFournisseur(fournisseur);
        livraison.setDatePrevue(request.datePrevue());
        livraison.setObservations(request.observations());
        livraison.setStatut(StatutLivraison.PLANIFIEE);

        for (LigneLivraisonRequest ligneRequest : request.lignes()) {
            LigneMarche ligneMarche = marche.getLignes().stream()
                    .filter(l -> l.getId().equals(ligneRequest.ligneMarcheId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Ligne de marché invalide : " + ligneRequest.ligneMarcheId()));
            int deja = dejaLivres.getOrDefault(ligneMarche.getId(), 0);
            if (ligneRequest.quantiteLivree() > ligneMarche.getQuantite() - deja) {
                throw new BusinessException("La quantité livrée pour '" + ligneMarche.getDesignation()
                        + "' dépasse le reste à livrer (" + (ligneMarche.getQuantite() - deja) + ")");
            }
            LigneLivraison ligne = new LigneLivraison();
            ligne.setLigneMarche(ligneMarche);
            ligne.setQuantiteLivree(ligneRequest.quantiteLivree());
            livraison.ajouterLigne(ligne);
        }
        livraisonRepository.save(livraison);
        marcheService.marquerEnExecution(marche);

        notificationService.envoyerAuRole(Role.RESPONSABLE_SERVICE, "Nouvelle livraison planifiée",
                "Le fournisseur " + fournisseur.getRaisonSociale() + " a planifié une livraison pour "
                        + marche.getReference() + " le " + request.datePrevue() + ".",
                "LIVRAISON", "/marches/" + marche.getId());
        auditService.enregistrer("CREATION_LIVRAISON", "LIVRAISON", livraison.getId(),
                marche.getReference() + " - date prévue " + request.datePrevue());
        return mapper(livraison);
    }

    @Transactional
    public LivraisonDto declarer(Long id, DeclarationLivraisonRequest request) {
        Livraison livraison = chargerAutorise(id);
        if (livraison.getStatut() == StatutLivraison.RECEPTIONNEE) {
            throw new BusinessException("Cette livraison a déjà été réceptionnée");
        }
        livraison.setDateEffective(request.dateEffective());
        livraison.setObservations(request.observations());
        livraison.setStatut(StatutLivraison.LIVREE);
        livraisonRepository.save(livraison);
        notificationService.envoyerAuRole(Role.RESPONSABLE_SERVICE, "Livraison déclarée",
                "La livraison du marché " + livraison.getMarche().getReference()
                        + " a été déclarée le " + request.dateEffective() + ". Vous pouvez procéder à la réception.",
                "LIVRAISON", "/marches/" + livraison.getMarche().getId());
        auditService.enregistrer("DECLARATION_LIVRAISON", "LIVRAISON", id,
                "Date effective : " + request.dateEffective());
        return mapper(livraison);
    }

    @Transactional
    public LivraisonDto receptionner(Long id, ReceptionRequest request) {
        Livraison livraison = charger(id);
        if (livraison.getStatut() != StatutLivraison.LIVREE
                && livraison.getStatut() != StatutLivraison.EN_RETARD) {
            throw new BusinessException("Seule une livraison déclarée peut être réceptionnée");
        }
        if (livraison.getReception() != null) {
            throw new BusinessException("Cette livraison a déjà une réception enregistrée");
        }
        Utilisateur receptionnaire = utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);

        Reception reception = new Reception();
        reception.setLivraison(livraison);
        reception.setDateReception(request.dateReception());
        reception.setObservations(request.observations());
        reception.setReserves(request.reserves());
        reception.setStatut(request.statut());
        reception.setReceptionnaire(receptionnaire);
        livraison.setReception(reception);
        if (livraison.getDateEffective() == null) {
            livraison.setDateEffective(request.dateReception());
        }
        livraison.setStatut(StatutLivraison.RECEPTIONNEE);
        livraisonRepository.save(livraison);

        notificationService.envoyer(livraison.getFournisseur().getUtilisateur(), "Réception de livraison",
                "La livraison du marché " + livraison.getMarche().getReference() + " a été réceptionnée ("
                        + request.statut() + ")." + (request.reserves() != null
                        ? " Réserves : " + request.reserves() : ""),
                "LIVRAISON", "/fournisseur/livraisons");
        auditService.enregistrer("RECEPTION_LIVRAISON", "LIVRAISON", id,
                request.statut() + (request.reserves() != null ? " - " + request.reserves() : ""));
        return mapper(livraison);
    }

    @Transactional(readOnly = true)
    public List<LivraisonDto> lister(Long marcheId) {
        List<Livraison> livraisons = marcheId != null
                ? livraisonRepository.findByMarcheIdOrderByDatePrevueAsc(marcheId)
                : livraisonRepository.findAllByOrderByDatePrevueDesc();
        return livraisons.stream().map(this::mapper).toList();
    }

    @Transactional(readOnly = true)
    public List<LivraisonDto> mesLivraisons() {
        Fournisseur fournisseur = fournisseurService.fournisseurCourant();
        return livraisonRepository.findByFournisseurIdOrderByDatePrevueDesc(fournisseur.getId())
                .stream().map(this::mapper).toList();
    }

    @Transactional(readOnly = true)
    public List<LivraisonDto> enRetard() {
        return livraisonRepository.findEnRetard(LocalDate.now()).stream().map(this::mapper).toList();
    }

    @Transactional(readOnly = true)
    public LivraisonDto parId(Long id) {
        return mapper(chargerAutorise(id));
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void marquerLivraisonsEnRetard() {
        for (Livraison livraison : livraisonRepository.findEnRetard(LocalDate.now())) {
            if (livraison.getStatut() == StatutLivraison.PLANIFIEE) {
                livraison.setStatut(StatutLivraison.EN_RETARD);
                livraisonRepository.save(livraison);
            }
        }
    }

    private Livraison charger(Long id) {
        return livraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livraison introuvable"));
    }

    private Livraison chargerAutorise(Long id) {
        Livraison livraison = charger(id);
        if (SecurityUtils.hasRole(Role.FOURNISSEUR)) {
            Fournisseur fournisseur = fournisseurService.fournisseurCourant();
            if (!livraison.getFournisseur().getId().equals(fournisseur.getId())) {
                throw new ForbiddenException("Cette livraison ne vous appartient pas");
            }
        }
        return livraison;
    }

    private Map<Long, Integer> quantitesDejaLivrees(Long marcheId) {
        Map<Long, Integer> quantites = new HashMap<>();
        for (Livraison livraison : livraisonRepository.findByMarcheIdOrderByDatePrevueAsc(marcheId)) {
            if (livraison.getReception() != null
                    && livraison.getReception().getStatut() == StatutReception.REFUSEE) {
                continue;
            }
            for (LigneLivraison ligne : livraison.getLignes()) {
                quantites.merge(ligne.getLigneMarche().getId(), ligne.getQuantiteLivree(), Integer::sum);
            }
        }
        return quantites;
    }

    public LivraisonDto mapper(Livraison livraison) {
        List<DocumentDto> documents = documentService.lister("LIVRAISON", livraison.getId());
        return LivraisonDto.from(livraison, documents);
    }
}
