package com.marchepro.chatbot.service;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.chatbot.dto.ChatResponse;
import com.marchepro.chatbot.entity.RechercheChatbot;
import com.marchepro.chatbot.repository.RechercheChatbotRepository;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.fournisseur.service.FournisseurService;
import com.marchepro.livraison.entity.Livraison;
import com.marchepro.livraison.repository.LivraisonRepository;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.entity.StatutMarche;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.materiel.entity.Materiel;
import com.marchepro.materiel.repository.MaterielRepository;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private static final Pattern REFERENCE = Pattern.compile("\\b([A-Z]{2,4})-(\\d{4})-(\\d{3})\\b");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> MOTS_VIDES = new java.util.HashSet<>(java.util.List.of(
            "quels", "quelles", "quel", "quelle", "sont", "les", "des", "pour", "dans", "avec",
            "recherche", "rechercher", "concernant", "actuellement", "disponibles", "disponible",
            "marches", "marche", "appels", "appel", "offres", "offre", "fournisseurs", "fournisseur",
            "materiels", "materiel", "infrastructures", "infrastructure", "sous", "une", "mes", "mon",
            "tous", "toutes", "liste", "lister", "soumis", "soumise", "sont", "encore",
            "actifs", "actives", "ouverts", "ouvertes", "bientot", "expires", "expire", "retard",
            "livraison", "livraisons", "statut", "reference", "donne", "moi", "sur", "par", "en"));

    private final AppelOffreRepository appelOffreRepository;
    private final MarcheRepository marcheRepository;
    private final FournisseurRepository fournisseurRepository;
    private final MaterielRepository materielRepository;
    private final OffreRepository offreRepository;
    private final LivraisonRepository livraisonRepository;
    private final AttributionRepository attributionRepository;
    private final RechercheChatbotRepository rechercheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FournisseurService fournisseurService;

    public ChatbotService(AppelOffreRepository appelOffreRepository,
                          MarcheRepository marcheRepository,
                          FournisseurRepository fournisseurRepository,
                          MaterielRepository materielRepository,
                          OffreRepository offreRepository,
                          LivraisonRepository livraisonRepository,
                          AttributionRepository attributionRepository,
                          RechercheChatbotRepository rechercheRepository,
                          UtilisateurRepository utilisateurRepository,
                          FournisseurService fournisseurService) {
        this.appelOffreRepository = appelOffreRepository;
        this.marcheRepository = marcheRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.materielRepository = materielRepository;
        this.offreRepository = offreRepository;
        this.livraisonRepository = livraisonRepository;
        this.attributionRepository = attributionRepository;
        this.rechercheRepository = rechercheRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.fournisseurService = fournisseurService;
    }

    @Transactional
    public ChatResponse repondre(String question) {
        String q = normaliser(question);
        Role role = SecurityUtils.currentRole();
        boolean interne = role == Role.ADMINISTRATEUR || role == Role.DIRECTEUR
                || role == Role.MEMBRE_COMMISSION || role == Role.RESPONSABLE_SERVICE;

        ChatResponse response = router(q, role, interne);
        journaliser(question, response);
        return response;
    }

    private ChatResponse router(String q, Role role, boolean interne) {
        Matcher matcher = REFERENCE.matcher(q.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            String reference = matcher.group();
            if (reference.startsWith("AO-")) {
                return rechercherAppelOffre(reference);
            }
            if (reference.startsWith("MAR-")) {
                if (q.contains("offre") || q.contains("fournisseur")) {
                    return fournisseursDuMarche(reference, role, interne);
                }
                return rechercherMarche(reference, role, interne);
            }
        }

        if ((q.contains("retard") && (q.contains("livraison") || q.contains("marche")))
                || q.contains("retard de livraison")) {
            return livraisonsEnRetard(interne);
        }

        if (q.contains("bientot") && (q.contains("expire") || q.contains("expiration"))) {
            return appelsOffresBientotExpires();
        }

        if (q.contains("appel") && (q.contains("ouvert") || q.contains("disponible")
                || q.contains("encours") || q.contains("actuel"))) {
            return appelsOffresOuverts();
        }

        if (q.contains("informatique") || q.contains("infrastructure")) {
            return infrastructures(q);
        }

        if (q.contains("fournisseur") && !q.contains("offre")) {
            return rechercherFournisseurs(q, interne);
        }

        if (q.contains("marche")) {
            ChatResponse parStatut = marchesParStatut(q, role, interne);
            if (parStatut != null) {
                return parStatut;
            }
        }

        return rechercheGlobale(q, role, interne);
    }

    private ChatResponse rechercherAppelOffre(String reference) {
        AppelOffre appelOffre = appelOffreRepository.findByReference(reference).orElse(null);
        if (appelOffre == null) {
            return new ChatResponse("APPEL_OFFRE", "Aucun appel d'offres ne porte la référence "
                    + reference + ".", List.of());
        }
        if (appelOffre.getStatut() != StatutAppelOffre.PUBLIE
                && appelOffre.getStatut() != StatutAppelOffre.CLOTURE) {
            return new ChatResponse("APPEL_OFFRE", "L'appel d'offres " + reference
                    + " existe mais n'est pas publié.", List.of());
        }
        return new ChatResponse("APPEL_OFFRE", "J'ai trouvé l'appel d'offres " + reference + ".",
                List.of(resultatAo(appelOffre, true)));
    }

    private ChatResponse rechercherMarche(String reference, Role role, boolean interne) {
        Marche marche = marcheRepository.findByReference(reference).orElse(null);
        if (marche == null) {
            return new ChatResponse("MARCHE", "Aucun marché ne porte la référence " + reference + ".", List.of());
        }
        if (!interne) {
            Fournisseur fournisseur = fournisseurService.fournisseurCourant();
            boolean concerne = attributionRepository.findByMarcheId(marche.getId())
                    .filter(a -> a.getStatut() == StatutAttribution.VALIDEE)
                    .map(a -> a.getFournisseur().getId().equals(fournisseur.getId()))
                    .orElse(false);
            if (!concerne) {
                return new ChatResponse("MARCHE",
                        "Aucun marché correspondant n'est accessible avec votre compte.", List.of());
            }
        }
        return new ChatResponse("MARCHE", "J'ai trouvé le marché " + reference + ".",
                List.of(resultatMarche(marche, interne)));
    }

    private ChatResponse fournisseursDuMarche(String reference, Role role, boolean interne) {
        Marche marche = marcheRepository.findByReference(reference).orElse(null);
        if (marche == null) {
            return new ChatResponse("FOURNISSEURS_OFFRES", "Aucun marché ne porte la référence "
                    + reference + ".", List.of());
        }
        AppelOffre appelOffre = appelOffreRepository.findByMarcheId(marche.getId()).orElse(null);
        if (appelOffre == null) {
            return new ChatResponse("FOURNISSEURS_OFFRES", "Aucun appel d'offres n'est associé au marché "
                    + reference + ".", List.of());
        }
        List<Offre> offres = offreRepository.findByAppelOffreIdOrderByMontantAsc(appelOffre.getId());
        List<ChatResponse.Resultat> resultats = new ArrayList<>();
        if (!interne) {
            Fournisseur fournisseur = fournisseurService.fournisseurCourant();
            offres.stream().filter(o -> o.getFournisseur().getId().equals(fournisseur.getId())
                    && o.getStatut() != StatutOffre.BROUILLON).forEach(o -> resultats.add(resultatOffre(o)));
            return new ChatResponse("FOURNISSEURS_OFFRES", resultats.isEmpty()
                    ? "Vous n'avez pas soumis d'offre pour le marché " + reference + "."
                    : "Voici votre offre pour le marché " + reference + ".", resultats);
        }
        offres.stream().filter(o -> o.getStatut() != StatutOffre.BROUILLON)
                .forEach(o -> resultats.add(resultatOffre(o)));
        return new ChatResponse("FOURNISSEURS_OFFRES",
                resultats.isEmpty()
                        ? "Aucune offre n'a été soumise pour le marché " + reference + "."
                        : resultats.size() + " offre(s) soumise(s) pour le marché " + reference + ".",
                resultats);
    }

    private ChatResponse livraisonsEnRetard(boolean interne) {
        if (!interne) {
            return new ChatResponse("LIVRAISONS_RETARD",
                    "Cette information est réservée aux agents habilités de l'organisme.", List.of());
        }
        List<Livraison> livraisons = livraisonRepository.findEnRetard(LocalDate.now());
        List<ChatResponse.Resultat> resultats = livraisons.stream()
                .map(l -> new ChatResponse.Resultat("LIVRAISON", l.getId(),
                        l.getMarche().getReference() + " - " + l.getFournisseur().getRaisonSociale(),
                        "Date prévue : " + l.getDatePrevue() + " | Statut : " + l.getStatut(),
                        "/marches/" + l.getMarche().getId()))
                .toList();
        return new ChatResponse("LIVRAISONS_RETARD", resultats.isEmpty()
                ? "Aucune livraison n'est actuellement en retard."
                : resultats.size() + " livraison(s) en retard de livraison.", resultats);
    }

    private ChatResponse appelsOffresBientotExpires() {
        List<AppelOffre> appels = appelOffreRepository.findBientotExpires(
                LocalDateTime.now(), LocalDateTime.now().plusDays(30), PageRequest.of(0, 6)).getContent();
        List<ChatResponse.Resultat> resultats = appels.stream()
                .map(a -> resultatAo(a, true)).toList();
        return new ChatResponse("APPELS_BIENTOT_EXPIRES", resultats.isEmpty()
                ? "Aucun appel d'offres n'expire dans les 30 prochains jours."
                : resultats.size() + " appel(s) d'offres expirent dans les 30 prochains jours.", resultats);
    }

    private ChatResponse appelsOffresOuverts() {
        List<AppelOffre> appels = appelOffreRepository.findBientotExpires(
                LocalDateTime.now(), LocalDateTime.now().plusYears(10), PageRequest.of(0, 10)).getContent();
        List<ChatResponse.Resultat> resultats = appels.stream()
                .map(a -> resultatAo(a, true)).toList();
        return new ChatResponse("APPELS_OUVERTS", resultats.isEmpty()
                ? "Aucun appel d'offres n'est actuellement ouvert."
                : resultats.size() + " appel(s) d'offres sont actuellement ouverts.", resultats);
    }

    private ChatResponse infrastructures(String q) {
        String terme = q.contains("informatique") ? "informatique" : null;
        List<Materiel> materiels = terme != null
                ? materielRepository.rechercher("INFORMATIQUE", null, PageRequest.of(0, 8)).getContent()
                : materielRepository.rechercher(null, "reseau", PageRequest.of(0, 8)).getContent();
        if (materiels.isEmpty()) {
            materiels = materielRepository.rechercher(null, null, PageRequest.of(0, 8)).getContent();
        }
        List<ChatResponse.Resultat> resultats = materiels.stream().map(this::resultatMateriel).toList();
        return new ChatResponse("MATERIELS", resultats.isEmpty()
                ? "Le catalogue des matériels est vide pour le moment."
                : "Voici les matériels et infrastructures du catalogue de l'organisme.", resultats);
    }

    private ChatResponse rechercherFournisseurs(String q, boolean interne) {
        if (!interne) {
            Fournisseur fournisseur = fournisseurService.fournisseurCourant();
            return new ChatResponse("FOURNISSEUR", "Voici votre fiche fournisseur.",
                    List.of(new ChatResponse.Resultat("FOURNISSEUR", fournisseur.getId(),
                            fournisseur.getRaisonSociale(), "NINEA : " + fournisseur.getNinea()
                            + " | Statut : " + fournisseur.getStatutValidation(), "/fournisseur/profil")));
        }
        String terme = extraireTerme(q);
        List<Fournisseur> fournisseurs = fournisseurRepository
                .rechercher(null, terme, PageRequest.of(0, 8)).getContent();
        List<ChatResponse.Resultat> resultats = fournisseurs.stream()
                .map(f -> new ChatResponse.Resultat("FOURNISSEUR", f.getId(), f.getRaisonSociale(),
                        "NINEA : " + f.getNinea() + " | Statut : " + f.getStatutValidation()
                                + " | " + f.getEmail(), "/admin/fournisseurs"))
                .toList();
        return new ChatResponse("FOURNISSEUR", resultats.isEmpty()
                ? "Aucun fournisseur ne correspond à votre recherche."
                : resultats.size() + " fournisseur(s) trouvé(s).", resultats);
    }

    private ChatResponse marchesParStatut(String q, Role role, boolean interne) {
        StatutMarche statut = detecterStatut(q);
        if (statut == null) {
            return null;
        }
        if (!interne) {
            Fournisseur fournisseur = fournisseurService.fournisseurCourant();
            List<ChatResponse.Resultat> resultats = attributionRepository
                    .findByFournisseurIdAndStatut(fournisseur.getId(), StatutAttribution.VALIDEE)
                    .stream()
                    .filter(a -> a.getMarche().getStatut() == statut)
                    .map(a -> resultatMarche(a.getMarche(), false))
                    .toList();
            return new ChatResponse("MARCHES_STATUT", resultats.isEmpty()
                    ? "Aucun de vos marchés ne correspond à ce statut."
                    : resultats.size() + " de vos marchés correspondent à ce statut.", resultats);
        }
        List<Marche> marches = marcheRepository.rechercher(statut, null, null, null,
                PageRequest.of(0, 8)).getContent();
        List<ChatResponse.Resultat> resultats = marches.stream()
                .map(m -> resultatMarche(m, true)).toList();
        return new ChatResponse("MARCHES_STATUT", resultats.isEmpty()
                ? "Aucun marché ne correspond à ce statut."
                : resultats.size() + " marché(s) au statut " + statut + ".", resultats);
    }

    private ChatResponse rechercheGlobale(String q, Role role, boolean interne) {
        String terme = extraireTerme(q);
        List<ChatResponse.Resultat> resultats = new ArrayList<>();

        if (terme != null) {
            List<AppelOffre> appels = appelOffreRepository.rechercherPublics(
                    terme, null, null, LocalDateTime.now(), null, PageRequest.of(0, 4)).getContent();
            appels.forEach(a -> resultats.add(resultatAo(a, true)));

            List<Materiel> materiels = materielRepository.rechercher(null, terme, PageRequest.of(0, 4)).getContent();
            materiels.forEach(m -> resultats.add(resultatMateriel(m)));

            if (interne) {
                List<Marche> marches = marcheRepository.rechercher(null, terme, null, null,
                        PageRequest.of(0, 4)).getContent();
                marches.forEach(m -> resultats.add(resultatMarche(m, true)));
            }
        }

        if (resultats.isEmpty()) {
            List<AppelOffre> recents = appelOffreRepository.findRecents(
                    LocalDateTime.now(), PageRequest.of(0, 3)).getContent();
            recents.forEach(a -> resultats.add(resultatAo(a, true)));
            return new ChatResponse("RECHERCHE_GLOBALE",
                    "Je n'ai pas trouvé de résultat correspondant à votre question. Voici les appels d'offres récents.",
                    resultats);
        }
        return new ChatResponse("RECHERCHE_GLOBALE",
                resultats.size() + " résultat(s) trouvé(s) dans les données de la plateforme.", resultats);
    }

    private StatutMarche detecterStatut(String q) {
        if (q.contains("preparation")) return StatutMarche.EN_PREPARATION;
        if (q.contains("validation")) return StatutMarche.EN_VALIDATION;
        if (q.contains("publie")) return StatutMarche.PUBLIE;
        if (q.contains("evaluation")) return StatutMarche.EN_EVALUATION;
        if (q.contains("attribue")) return StatutMarche.ATTRIBUE;
        if (q.contains("execution")) return StatutMarche.EN_EXECUTION;
        if (q.contains("cloture")) return StatutMarche.CLOTURE;
        if (q.contains("annule")) return StatutMarche.ANNULE;
        return null;
    }

    private ChatResponse.Resultat resultatAo(AppelOffre a, boolean publicLien) {
        String detail = (a.getOrganisme() != null ? a.getOrganisme() : "Organisme")
                + " | Catégorie : " + (a.getCategorie() != null ? a.getCategorie() : "Non précisée")
                + " | Date limite : " + DATE.format(a.getDateLimite())
                + " | Statut : " + a.getStatut();
        String lien = publicLien ? "/opportunites/" + a.getId() : "/appels-offres/" + a.getId();
        return new ChatResponse.Resultat("APPEL_OFFRE", a.getId(), a.getReference() + " - " + a.getObjet(),
                detail, lien);
    }

    private ChatResponse.Resultat resultatMarche(Marche m, boolean interne) {
        String detail = "Statut : " + m.getStatut()
                + " | Budget : " + (m.getBudgetPrevisionnel() != null ? m.getBudgetPrevisionnel() + " FCFA" : "non renseigné")
                + " | Mode : " + m.getModePassation();
        return new ChatResponse.Resultat("MARCHE", m.getId(), m.getReference() + " - " + m.getObjet(),
                detail, interne ? "/marches/" + m.getId() : "/fournisseur/marches");
    }

    private ChatResponse.Resultat resultatMateriel(Materiel m) {
        return new ChatResponse.Resultat("MATERIEL", m.getId(), m.getDesignation(),
                "Catégorie : " + m.getCategorie()
                        + (m.getCaracteristiques() != null ? " | " + m.getCaracteristiques() : ""),
                "/catalogue");
    }

    private ChatResponse.Resultat resultatOffre(Offre o) {
        return new ChatResponse.Resultat("OFFRE", o.getId(),
                (o.getReference() != null ? o.getReference() : "Brouillon") + " - "
                        + o.getFournisseur().getRaisonSociale(),
                "Montant : " + o.getMontant() + " FCFA | Statut : " + o.getStatut(),
                "/offres/" + o.getId());
    }

    private String extraireTerme(String q) {
        Set<String> mots = new LinkedHashSet<>();
        for (String mot : q.split("[^a-z0-9]+")) {
            if (mot.length() >= 3 && !MOTS_VIDES.contains(mot)) {
                mots.add(mot);
            }
        }
        return mots.isEmpty() ? null : mots.iterator().next();
    }

    private void journaliser(String question, ChatResponse response) {
        try {
            RechercheChatbot recherche = new RechercheChatbot();
            recherche.setQuestion(question);
            recherche.setReponse(response.reponse());
            recherche.setIntention(response.intention());
            Utilisateur utilisateur = utilisateurRepository.findById(SecurityUtils.currentUserId()).orElse(null);
            recherche.setUtilisateur(utilisateur);
            rechercheRepository.save(recherche);
        } catch (Exception ignored) {
            // La journalisation ne doit pas bloquer la réponse du chatbot
        }
    }

    private String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sansAccents.toLowerCase(Locale.ROOT).trim();
    }
}
