package com.marchepro.config;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.DocumentObligatoire;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.appeloffre.repository.DocumentObligatoireRepository;
import com.marchepro.attribution.entity.Attribution;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.besoin.entity.ExpressionBesoin;
import com.marchepro.besoin.entity.LigneBesoin;
import com.marchepro.besoin.entity.StatutBesoin;
import com.marchepro.besoin.repository.ExpressionBesoinRepository;
import com.marchepro.common.document.Document;
import com.marchepro.common.document.DocumentRepository;
import com.marchepro.common.document.DocumentType;
import com.marchepro.evaluation.entity.Evaluation;
import com.marchepro.evaluation.entity.NoteEvaluation;
import com.marchepro.evaluation.repository.EvaluationRepository;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.livraison.entity.LigneLivraison;
import com.marchepro.livraison.entity.Livraison;
import com.marchepro.livraison.entity.Reception;
import com.marchepro.livraison.entity.StatutLivraison;
import com.marchepro.livraison.entity.StatutReception;
import com.marchepro.livraison.repository.LivraisonRepository;
import com.marchepro.marche.entity.CritereEvaluation;
import com.marchepro.marche.entity.LigneMarche;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.entity.ModePassation;
import com.marchepro.marche.entity.StatutMarche;
import com.marchepro.marche.entity.TypeCritere;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.materiel.entity.Materiel;
import com.marchepro.materiel.repository.MaterielRepository;
import com.marchepro.notification.entity.Notification;
import com.marchepro.notification.repository.NotificationRepository;
import com.marchepro.offre.entity.LigneOffre;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");

    private final UtilisateurRepository utilisateurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final MaterielRepository materielRepository;
    private final ExpressionBesoinRepository besoinRepository;
    private final MarcheRepository marcheRepository;
    private final AppelOffreRepository appelOffreRepository;
    private final DocumentObligatoireRepository documentObligatoireRepository;
    private final OffreRepository offreRepository;
    private final EvaluationRepository evaluationRepository;
    private final AttributionRepository attributionRepository;
    private final LivraisonRepository livraisonRepository;
    private final NotificationRepository notificationRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final String annee = String.valueOf(Year.now(DAKAR).getValue());
    private int compteurOffre = 0;

    public DataSeeder(UtilisateurRepository utilisateurRepository,
                      FournisseurRepository fournisseurRepository,
                      MaterielRepository materielRepository,
                      ExpressionBesoinRepository besoinRepository,
                      MarcheRepository marcheRepository,
                      AppelOffreRepository appelOffreRepository,
                      DocumentObligatoireRepository documentObligatoireRepository,
                      OffreRepository offreRepository,
                      EvaluationRepository evaluationRepository,
                      AttributionRepository attributionRepository,
                      LivraisonRepository livraisonRepository,
                      NotificationRepository notificationRepository,
                      DocumentRepository documentRepository,
                      PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.materielRepository = materielRepository;
        this.besoinRepository = besoinRepository;
        this.marcheRepository = marcheRepository;
        this.appelOffreRepository = appelOffreRepository;
        this.documentObligatoireRepository = documentObligatoireRepository;
        this.offreRepository = offreRepository;
        this.evaluationRepository = evaluationRepository;
        this.attributionRepository = attributionRepository;
        this.livraisonRepository = livraisonRepository;
        this.notificationRepository = notificationRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) {
            log.info("Données déjà présentes : le seed MarchéPro est ignoré.");
            return;
        }
        log.info("Initialisation des données de démonstration MarchéPro...");
        Instant maintenant = Instant.now();
        LocalDate aujourdhui = LocalDate.now(DAKAR);

        Utilisateur admin = utilisateur("Diallo", "Mamadou", "admin@marchepro.sn", "Admin@2026",
                Role.ADMINISTRATEUR, "77 123 45 67");
        Utilisateur responsable = utilisateur("Ndiaye", "Awa", "responsable@marchepro.sn", "Responsable@2026",
                Role.RESPONSABLE_SERVICE, "77 234 56 78");
        Utilisateur commission = utilisateur("Diop", "Moussa", "commission@marchepro.sn", "Commission@2026",
                Role.MEMBRE_COMMISSION, "77 345 67 89");
        Utilisateur commission2 = utilisateur("Fall", "Aminata", "commission2@marchepro.sn", "Commission@2026",
                Role.MEMBRE_COMMISSION, "77 456 78 90");
        Utilisateur directeur = utilisateur("Sow", "Fatou", "directeur@marchepro.sn", "Directeur@2026",
                Role.DIRECTEUR, "77 567 89 01");

        Utilisateur uTech = utilisateur("Sarr", "Ibrahima", "fournisseur@demo.sn", "Fournisseur@2026",
                Role.FOURNISSEUR, "78 111 22 33");
        Utilisateur uBatimat = utilisateur("Ba", "Ousmane", "batimat@demo.sn", "Fournisseur@2026",
                Role.FOURNISSEUR, "78 222 33 44");
        Utilisateur uDigital = utilisateur("Faye", "Cheikh", "digital@demo.sn", "Fournisseur@2026",
                Role.FOURNISSEUR, "78 333 44 55");
        Utilisateur uGlobal = utilisateur("Kane", "Aissatou", "global@demo.sn", "Fournisseur@2026",
                Role.FOURNISSEUR, "78 444 55 66");
        Utilisateur uSahel = utilisateur("Gueye", "Modou", "sahel@demo.sn", "Fournisseur@2026",
                Role.FOURNISSEUR, "78 555 66 77");

        Fournisseur tech = fournisseur("Tech Sénégal SARL", "1234567890", "SN-DKR-2020-B-1234",
                "Sacré-Cœur 3, Dakar", "78 111 22 33", "contact@techsenegal.sn", "Ibrahima Sarr",
                uTech, StatutValidation.VALIDE, null);
        Fournisseur batimat = fournisseur("Batimat Sénégal", "2345678901", "SN-DKR-2018-B-5678",
                "Zone industrielle, Dakar", "78 222 33 44", "contact@batimat.sn", "Ousmane Ba",
                uBatimat, StatutValidation.VALIDE, null);
        Fournisseur digital = fournisseur("Digital Services Dakar", "3456789012", "SN-DKR-2021-B-9012",
                "Point E, Dakar", "78 333 44 55", "contact@digitaldakar.sn", "Cheikh Faye",
                uDigital, StatutValidation.VALIDE, null);
        Fournisseur globalOffice = fournisseur("Global Office Sénégal", "4567890123", "SN-DKR-2022-B-3456",
                "Almadies, Dakar", "78 444 55 66", "contact@globaloffice.sn", "Aissatou Kane",
                uGlobal, StatutValidation.EN_ATTENTE, null);
        fournisseur("Sahel Construction", "5678901234", "SN-THS-2019-B-7788",
                "Thiès, Sénégal", "78 555 66 77", "contact@sahel.sn", "Modou Gueye",
                uSahel, StatutValidation.REJETE, "Pièces administratives incomplètes");

        materiel("Ordinateur portable professionnel", "INFORMATIQUE",
                "Portable 14 pouces pour cadres et agents",
                "Intel Core i7, 16 Go RAM, SSD 512 Go, écran 14\" Full HD");
        materiel("Ordinateur de bureau", "INFORMATIQUE",
                "Unité centrale complète avec écran 24 pouces",
                "Intel Core i5, 8 Go RAM, SSD 256 Go, clavier et souris");
        materiel("Imprimante multifonction laser", "INFORMATIQUE",
                "Imprimante réseau recto-verso",
                "Laser couleur, 30 ppm, scanner, fax, WiFi");
        materiel("Serveur rack", "INFORMATIQUE",
                "Serveur de virtualisation pour le datacenter",
                "2 processeurs Xeon, 64 Go RAM, baie de stockage 8 To");
        materiel("Onduleur 1500 VA", "INFORMATIQUE",
                "Protection électrique des équipements sensibles",
                "1500 VA, autonomie 20 min, régulation AVR");
        materiel("Commutateur réseau 48 ports", "INFORMATIQUE",
                "Switch administrable pour le réseau local",
                "48 ports Gigabit, 4 ports SFP+, VLAN");
        materiel("Vidéoprojecteur Full HD", "AUDIOVISUEL",
                "Projecteur pour salles de formation",
                "Full HD, 4000 lumens, HDMI, WiFi");
        materiel("Tableau blanc interactif", "AUDIOVISUEL",
                "Tableau numérique pour salles de réunion",
                "Surface 180 x 120 cm, tactile, logiciel inclus");
        materiel("Bureau direction", "MOBILIER",
                "Bureau en bois massif avec caisson",
                "180 x 90 cm, finition acajou, passe-câbles");
        materiel("Chaise ergonomique", "MOBILIER",
                "Siège de bureau réglable",
                "Assise tissu, soutien lombaire, accoudoirs réglables");
        materiel("Armoire métallique", "MOBILIER",
                "Armoire de rangement sécurisée",
                "2 portes, 4 étagères, serrure à clé");
        materiel("Véhicule utilitaire 4x4", "VEHICULE",
                "Pick-up double cabine pour missions terrain",
                "Diesel, 4x4, 5 places, climatisation");
        materiel("Fourgon de livraison", "VEHICULE",
                "Véhicule de transport de matériel",
                "Diesel, 12 m3, 3 places, hayon élévateur");
        materiel("Climatiseur split 2 CV", "EQUIPEMENT",
                "Climatisation des bureaux",
                "2 CV, inverter, silencieux, télécommande");

        ExpressionBesoin eb1 = besoin("EB", 1, "Acquisition de matériel informatique pour les services centraux",
                "Renouvellement du parc informatique arrivé en fin de vie.",
                "Direction des Systèmes d'Information", new BigDecimal("45000000"), aujourdhui.minusDays(130),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 135),
                ligneBesoin("Ordinateur portable professionnel", 50, "Intel Core i7, 16 Go RAM, SSD 512 Go"),
                ligneBesoin("Imprimante multifonction laser", 10, "Laser couleur, réseau, recto-verso"),
                ligneBesoin("Serveur rack", 2, "2 Xeon, 64 Go RAM, stockage 8 To"),
                ligneBesoin("Onduleur 1500 VA", 20, "1500 VA, autonomie 20 min"));

        ExpressionBesoin eb2 = besoin("EB", 2, "Renouvellement du parc informatique de la direction générale",
                "Remplacement des postes de travail obsolètes et des périphériques.",
                "Direction Générale", new BigDecimal("28000000"), aujourdhui.minusDays(30),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 35),
                ligneBesoin("Ordinateur de bureau", 30, "Intel Core i5, 8 Go RAM, SSD 256 Go"),
                ligneBesoin("Ordinateur portable professionnel", 15, "Intel Core i7, 16 Go RAM"),
                ligneBesoin("Imprimante multifonction laser", 5, "Laser couleur, réseau"));

        ExpressionBesoin eb3 = besoin("EB", 3, "Fourniture de mobilier de bureau pour les nouveaux locaux",
                "Équipement des nouveaux bureaux du siège.",
                "Direction des Ressources Générales", new BigDecimal("18500000"), aujourdhui.minusDays(70),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 75),
                ligneBesoin("Bureau direction", 40, "180 x 90 cm, finition acajou"),
                ligneBesoin("Chaise ergonomique", 80, "Assise tissu, soutien lombaire"),
                ligneBesoin("Armoire métallique", 20, "2 portes, 4 étagères, serrure"));

        ExpressionBesoin eb4 = besoin("EB", 4, "Acquisition de véhicules utilitaires",
                "Renforcement de la flotte pour les missions de terrain.",
                "Direction de la Logistique", new BigDecimal("65000000"), aujourdhui.minusDays(15),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 20),
                ligneBesoin("Véhicule utilitaire 4x4", 3, "Pick-up double cabine, diesel"),
                ligneBesoin("Fourgon de livraison", 2, "12 m3, hayon élévateur"));

        ExpressionBesoin eb5 = besoin("EB", 5, "Équipement des salles de formation",
                "Modernisation des salles de formation du centre de documentation.",
                "Direction de la Formation", new BigDecimal("12000000"), aujourdhui.minusDays(100),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 105),
                ligneBesoin("Vidéoprojecteur Full HD", 10, "4000 lumens, HDMI, WiFi"),
                ligneBesoin("Tableau blanc interactif", 10, "180 x 120 cm, tactile"),
                ligneBesoin("Ordinateur portable professionnel", 15, "Intel Core i7, 16 Go RAM"));

        ExpressionBesoin eb6 = besoin("EB", 6, "Maintenance des équipements réseau",
                "Contrat annuel de maintenance préventive et curative.",
                "Direction des Systèmes d'Information", new BigDecimal("9500000"), aujourdhui.minusDays(190),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 195),
                ligneBesoin("Maintenance préventive annuelle", 1, "Contrat 12 mois, interventions trimestrielles"),
                ligneBesoin("Pièces de rechange réseau", 1, "Stock de pièces critiques"));

        ExpressionBesoin eb7 = besoin("EB", 7, "Fourniture de consommables de bureau",
                "Achat de consommables pour l'ensemble des services.",
                "Direction des Ressources Générales", new BigDecimal("4200000"), aujourdhui.minusDays(55),
                StatutBesoin.VALIDEE, responsable, jours(maintenant, 60),
                ligneBesoin("Ramettes de papier A4", 200, "80 g/m2, 500 feuilles"),
                ligneBesoin("Cartouches d'encre", 60, "Compatible imprimantes laser"));

        besoin("EB", 8, "Achat de climatiseurs pour les salles de réunion",
                "Remplacement de trois climatiseurs hors service.",
                "Direction des Ressources Générales", new BigDecimal("3500000"), aujourdhui.plusDays(15),
                StatutBesoin.SOUMISE, responsable, jours(maintenant, 5),
                ligneBesoin("Climatiseur split 2 CV", 3, "Inverter, silencieux"));

        besoin("EB", 9, "Entretien des espaces verts du siège",
                "Contrat d'entretien paysager annuel.",
                "Direction des Ressources Générales", new BigDecimal("2500000"), aujourdhui.plusDays(30),
                StatutBesoin.BROUILLON, responsable, null,
                ligneBesoin("Entretien mensuel des espaces verts", 12, "Tonte, taille, arrosage"));

        Marche m1 = marche(1, "Acquisition de matériel informatique pour les services centraux",
                "Marché relatif au renouvellement du parc informatique des services centraux.",
                new BigDecimal("45000000"), ModePassation.APPEL_OFFRES_OUVERT, StatutMarche.EN_EXECUTION,
                eb1, responsable, jours(maintenant, 125));
        m1.setDateValidation(jours(maintenant, 118));
        m1.setDatePublication(jours(maintenant, 112));
        m1.setDateDebutExecution(aujourdhui.minusDays(60));
        m1.setDateFinExecution(aujourdhui.plusDays(60));
        lignesMarche(m1,
                ligneMarche("Ordinateur portable professionnel", 50, "Intel Core i7, 16 Go RAM, SSD 512 Go",
                        new BigDecimal("850000")),
                ligneMarche("Imprimante multifonction laser", 10, "Laser couleur, réseau, recto-verso",
                        new BigDecimal("450000")),
                ligneMarche("Serveur rack", 2, "2 Xeon, 64 Go RAM, stockage 8 To",
                        new BigDecimal("4500000")),
                ligneMarche("Onduleur 1500 VA", 20, "1500 VA, autonomie 20 min",
                        new BigDecimal("150000")));
        criteres(m1);
        marcheRepository.save(m1);

        Marche m2 = marche(2, "Renouvellement du parc informatique de la direction générale",
                "Acquisition de postes de travail et périphériques pour la direction générale.",
                new BigDecimal("28000000"), ModePassation.APPEL_OFFRES_OUVERT, StatutMarche.PUBLIE,
                eb2, responsable, jours(maintenant, 28));
        m2.setDateValidation(jours(maintenant, 22));
        m2.setDatePublication(jours(maintenant, 18));
        lignesMarche(m2,
                ligneMarche("Ordinateur de bureau", 30, "Intel Core i5, 8 Go RAM, SSD 256 Go",
                        new BigDecimal("550000")),
                ligneMarche("Ordinateur portable professionnel", 15, "Intel Core i7, 16 Go RAM",
                        new BigDecimal("850000")),
                ligneMarche("Imprimante multifonction laser", 5, "Laser couleur, réseau",
                        new BigDecimal("450000")));
        criteres(m2);
        marcheRepository.save(m2);

        Marche m3 = marche(3, "Fourniture de mobilier de bureau pour les nouveaux locaux",
                "Équipement complet des nouveaux bureaux du siège.",
                new BigDecimal("18500000"), ModePassation.APPEL_OFFRES_OUVERT, StatutMarche.EN_EVALUATION,
                eb3, responsable, jours(maintenant, 65));
        m3.setDateValidation(jours(maintenant, 58));
        m3.setDatePublication(jours(maintenant, 52));
        lignesMarche(m3,
                ligneMarche("Bureau direction", 40, "180 x 90 cm, finition acajou",
                        new BigDecimal("250000")),
                ligneMarche("Chaise ergonomique", 80, "Assise tissu, soutien lombaire",
                        new BigDecimal("85000")),
                ligneMarche("Armoire métallique", 20, "2 portes, 4 étagères, serrure",
                        new BigDecimal("150000")));
        criteres(m3);
        marcheRepository.save(m3);

        Marche m4 = marche(4, "Acquisition de véhicules utilitaires",
                "Renforcement de la flotte de véhicules pour les missions de terrain.",
                new BigDecimal("65000000"), ModePassation.APPEL_OFFRES_OUVERT, StatutMarche.EN_PREPARATION,
                eb4, responsable, jours(maintenant, 12));
        lignesMarche(m4,
                ligneMarche("Véhicule utilitaire 4x4", 3, "Pick-up double cabine, diesel",
                        new BigDecimal("18000000")),
                ligneMarche("Fourgon de livraison", 2, "12 m3, hayon élévateur",
                        new BigDecimal("5500000")));
        criteres(m4);
        marcheRepository.save(m4);

        Marche m5 = marche(5, "Équipement des salles de formation",
                "Modernisation des salles de formation du centre de documentation.",
                new BigDecimal("12000000"), ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, StatutMarche.ATTRIBUE,
                eb5, responsable, jours(maintenant, 95));
        m5.setDateValidation(jours(maintenant, 88));
        m5.setDatePublication(jours(maintenant, 82));
        m5.setDateDebutExecution(aujourdhui.minusDays(20));
        m5.setDateFinExecution(aujourdhui.plusDays(70));
        lignesMarche(m5,
                ligneMarche("Vidéoprojecteur Full HD", 10, "4000 lumens, HDMI, WiFi",
                        new BigDecimal("650000")),
                ligneMarche("Tableau blanc interactif", 10, "180 x 120 cm, tactile",
                        new BigDecimal("450000")),
                ligneMarche("Ordinateur portable professionnel", 15, "Intel Core i7, 16 Go RAM",
                        new BigDecimal("850000")));
        criteres(m5);
        marcheRepository.save(m5);

        Marche m6 = marche(6, "Maintenance des équipements réseau",
                "Contrat annuel de maintenance préventive et curative du réseau.",
                new BigDecimal("9500000"), ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, StatutMarche.CLOTURE,
                eb6, responsable, jours(maintenant, 185));
        m6.setDateValidation(jours(maintenant, 178));
        m6.setDatePublication(jours(maintenant, 172));
        m6.setDateDebutExecution(aujourdhui.minusDays(170));
        m6.setDateFinExecution(aujourdhui.minusDays(10));
        m6.setDateCloture(jours(maintenant, 8));
        lignesMarche(m6,
                ligneMarche("Maintenance préventive annuelle", 1, "Contrat 12 mois",
                        new BigDecimal("7000000")),
                ligneMarche("Pièces de rechange réseau", 1, "Stock de pièces critiques",
                        new BigDecimal("2500000")));
        criteres(m6);
        marcheRepository.save(m6);

        Marche m7 = marche(7, "Fourniture de consommables de bureau",
                "Achat de consommables pour l'ensemble des services.",
                new BigDecimal("4200000"), ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, StatutMarche.ANNULE,
                eb7, responsable, jours(maintenant, 50));
        m7.setDateValidation(jours(maintenant, 45));
        m7.setDatePublication(jours(maintenant, 42));
        m7.setMotifAnnulation("Révision du besoin et indisponibilité budgétaire.");
        lignesMarche(m7,
                ligneMarche("Ramettes de papier A4", 200, "80 g/m2, 500 feuilles",
                        new BigDecimal("15000")),
                ligneMarche("Cartouches d'encre", 60, "Compatible imprimantes laser",
                        new BigDecimal("25000")));
        criteres(m7);
        marcheRepository.save(m7);

        AppelOffre ao1 = appelOffre(1, m1, "Acquisition de matériel informatique pour les services centraux",
                "Appel d'offres ouvert pour le renouvellement du parc informatique.",
                "Informatique", "Organisme National de Démonstration", StatutAppelOffre.CLOTURE,
                LocalDateTime.now(DAKAR).minusDays(70), admin, jours(maintenant, 112));
        AppelOffre ao2 = appelOffre(2, m2, "Renouvellement du parc informatique de la direction générale",
                "Appel d'offres ouvert pour l'acquisition de postes de travail.",
                "Informatique", "Organisme National de Démonstration", StatutAppelOffre.PUBLIE,
                LocalDateTime.now(DAKAR).plusDays(14), admin, jours(maintenant, 18));
        AppelOffre ao3 = appelOffre(3, m3, "Fourniture de mobilier de bureau pour les nouveaux locaux",
                "Appel d'offres ouvert pour l'équipement des nouveaux bureaux.",
                "Mobilier", "Organisme National de Démonstration", StatutAppelOffre.CLOTURE,
                LocalDateTime.now(DAKAR).minusDays(20), admin, jours(maintenant, 52));
        AppelOffre ao5 = appelOffre(5, m5, "Équipement des salles de formation",
                "Demande de renseignements et de prix pour l'équipement des salles.",
                "Audiovisuel", "Organisme National de Démonstration", StatutAppelOffre.CLOTURE,
                LocalDateTime.now(DAKAR).minusDays(55), admin, jours(maintenant, 82));
        AppelOffre ao6 = appelOffre(6, m6, "Maintenance des équipements réseau",
                "Consultation pour le contrat annuel de maintenance réseau.",
                "Informatique", "Organisme National de Démonstration", StatutAppelOffre.CLOTURE,
                LocalDateTime.now(DAKAR).minusDays(160), admin, jours(maintenant, 172));
        AppelOffre ao7 = appelOffre(7, m7, "Fourniture de consommables de bureau",
                "Consultation annulée pour révision du besoin.",
                "Fournitures", "Organisme National de Démonstration", StatutAppelOffre.ANNULE,
                LocalDateTime.now(DAKAR).minusDays(20), admin, jours(maintenant, 42));
        ao7.setMotifAnnulation("Révision du besoin et indisponibilité budgétaire.");
        appelOffreRepository.save(ao7);

        cahierDesCharges(ao1);
        cahierDesCharges(ao2);
        cahierDesCharges(ao3);
        cahierDesCharges(ao5);
        cahierDesCharges(ao6);
        cahierDesCharges(ao7);

        documentsObligatoires(ao1);
        documentsObligatoires(ao2);
        documentsObligatoires(ao3);
        documentsObligatoires(ao5);
        documentsObligatoires(ao6);
        documentsObligatoires(ao7);

        Map<Integer, AppelOffre> aosPublies = appelsOffresPublies(admin, maintenant);

        offresAEvaluer(aosPublies, tech, batimat, digital, globalOffice, maintenant);

        Offre o1Tech = offre(ao1, tech, new BigDecimal("42500000"), 45, StatutOffre.RETENUE,
                jours(maintenant, 75), "Solution complète conforme au cahier des charges.");
        Offre o1Digital = offre(ao1, digital, new BigDecimal("44800000"), 60, StatutOffre.NON_RETENUE,
                jours(maintenant, 74), "Proposition technique et financière.");
        Offre o1Global = offre(ao1, globalOffice, new BigDecimal("46900000"), 90, StatutOffre.NON_RETENUE,
                jours(maintenant, 73), "Offre alternative.");

        Offre o2Tech = offre(ao2, tech, new BigDecimal("26900000"), 30, StatutOffre.SOUMISE,
                jours(maintenant, 4), "Offre soumise dans les délais.");
        Offre o2Digital = offre(ao2, digital, new BigDecimal("27500000"), 35, StatutOffre.SOUMISE,
                jours(maintenant, 3), "Offre soumise dans les délais.");
        offre(ao2, globalOffice, new BigDecimal("28200000"), 40, StatutOffre.BROUILLON,
                null, "Brouillon en cours de préparation.");

        Offre o3Batimat = offre(ao3, batimat, new BigDecimal("17900000"), 30, StatutOffre.EVALUEE,
                jours(maintenant, 30), "Mobilier conforme aux spécifications.");
        Offre o3Global = offre(ao3, globalOffice, new BigDecimal("18200000"), 35, StatutOffre.EVALUEE,
                jours(maintenant, 29), "Mobilier de qualité supérieure.");
        Offre o3Tech = offre(ao3, tech, new BigDecimal("19500000"), 25, StatutOffre.EVALUEE,
                jours(maintenant, 28), "Offre technique et financière.");

        Offre o5Global = offre(ao5, globalOffice, new BigDecimal("11400000"), 30, StatutOffre.RETENUE,
                jours(maintenant, 60), "Offre conforme aux besoins de formation.");
        Offre o5Batimat = offre(ao5, batimat, new BigDecimal("11900000"), 40, StatutOffre.NON_RETENUE,
                jours(maintenant, 59), "Offre technique et financière.");

        Offre o6Digital = offre(ao6, digital, new BigDecimal("9100000"), 365, StatutOffre.RETENUE,
                jours(maintenant, 165), "Contrat de maintenance annuel.");
        Offre o6Tech = offre(ao6, tech, new BigDecimal("9300000"), 365, StatutOffre.NON_RETENUE,
                jours(maintenant, 164), "Contrat de maintenance annuel.");

        evaluer(o1Tech, commission, 17, 18);
        evaluer(o1Tech, commission2, 16, 18);
        evaluer(o1Digital, commission, 15, 15);
        evaluer(o1Digital, commission2, 14, 16);
        evaluer(o1Global, commission, 13, 13);
        evaluer(o1Global, commission2, 14, 12);

        evaluer(o3Batimat, commission, 16, 17);
        evaluer(o3Batimat, commission2, 17, 16);
        evaluer(o3Global, commission, 18, 14);
        evaluer(o3Global, commission2, 17, 15);
        evaluer(o3Tech, commission, 15, 13);
        evaluer(o3Tech, commission2, 14, 14);

        evaluer(o5Global, commission, 17, 18);
        evaluer(o5Global, commission2, 16, 17);
        evaluer(o5Batimat, commission, 14, 15);
        evaluer(o5Batimat, commission2, 15, 14);

        evaluer(o6Digital, commission, 18, 17);
        evaluer(o6Digital, commission2, 17, 18);
        evaluer(o6Tech, commission, 15, 16);
        evaluer(o6Tech, commission2, 14, 15);

        Attribution a1 = attribution(m1, o1Tech, commission, directeur, StatutAttribution.VALIDEE,
                jours(maintenant, 70), jours(maintenant, 65),
                "Attribution validée par le directeur après consolidation des évaluations.");
        Attribution a3 = attribution(m3, o3Batimat, commission, null, StatutAttribution.PROPOSEE,
                jours(maintenant, 10), null,
                "Proposition d'attribution en attente de validation.");
        Attribution a5 = attribution(m5, o5Global, commission, directeur, StatutAttribution.VALIDEE,
                jours(maintenant, 60), jours(maintenant, 55),
                "Attribution validée par le directeur.");
        Attribution a6 = attribution(m6, o6Digital, commission, directeur, StatutAttribution.VALIDEE,
                jours(maintenant, 160), jours(maintenant, 155),
                "Attribution validée par le directeur.");

        livraison(m1, tech, aujourdhui.minusDays(50), aujourdhui.minusDays(52),
                StatutLivraison.RECEPTIONNEE, StatutReception.ACCEPTEE, aujourdhui.minusDays(52),
                "Livraison partielle conforme.", null,
                quantite(m1, 0, 25), quantite(m1, 1, 5), quantite(m1, 2, 1), quantite(m1, 3, 10));
        livraison(m1, tech, aujourdhui.minusDays(10), null,
                StatutLivraison.EN_RETARD, null, null,
                "Reliquat de la commande en attente de livraison.", null,
                quantite(m1, 0, 25), quantite(m1, 1, 5), quantite(m1, 2, 1), quantite(m1, 3, 10));
        livraison(m5, globalOffice, aujourdhui.plusDays(20), null,
                StatutLivraison.PLANIFIEE, null, null,
                "Livraison de l'ensemble des équipements de formation.", null,
                quantite(m5, 0, 10), quantite(m5, 1, 10), quantite(m5, 2, 15));
        livraison(m6, digital, aujourdhui.minusDays(160), aujourdhui.minusDays(162),
                StatutLivraison.RECEPTIONNEE, StatutReception.ACCEPTEE, aujourdhui.minusDays(162),
                "Contrat de maintenance soldé.", null,
                quantite(m6, 0, 1), quantite(m6, 1, 1));

        notification(admin, "Marché attribué",
                "Le marché " + m1.getReference() + " a été attribué à Tech Sénégal SARL.",
                "ATTRIBUTION", "/marches/" + m1.getId());
        notification(admin, "Livraison en retard",
                "La livraison du marché " + m1.getReference() + " prévue le "
                        + aujourdhui.minusDays(10) + " est en retard.", "LIVRAISON", "/marches/" + m1.getId());
        notification(admin, "Fournisseur en attente",
                "Le fournisseur Global Office Sénégal attend la validation de son dossier.",
                "FOURNISSEUR", "/admin/fournisseurs");
        notification(responsable, "Expression de besoin soumise",
                "Une nouvelle expression de besoin est en attente de validation.",
                "BESOIN", "/besoins");
        notification(responsable, "Attribution proposée",
                "Une proposition d'attribution est en attente de validation pour le marché "
                        + m3.getReference() + ".", "ATTRIBUTION", "/marches/" + m3.getId());
        notification(uTech, "Marché remporté",
                "Félicitations, votre offre a été retenue pour le marché " + m1.getReference() + ".",
                "ATTRIBUTION", "/fournisseur/marches");
        notification(uTech, "Livraison en retard",
                "Le reliquat de livraison du marché " + m1.getReference() + " est en retard.",
                "LIVRAISON", "/fournisseur/livraisons");
        notification(uGlobal, "Dossier en cours de validation",
                "Votre dossier fournisseur est en cours d'examen par l'administration.",
                "FOURNISSEUR", "/fournisseur/profil");

        log.info("Seed MarchéPro terminé : {} utilisateurs, {} marchés, {} appels d'offres, {} offres.",
                utilisateurRepository.count(), marcheRepository.count(),
                appelOffreRepository.count(), offreRepository.count());
    }

    private static Instant jours(Instant base, int nombre) {
        return base.minus(nombre, ChronoUnit.DAYS);
    }

    private record LigneSeed(String designation, int quantite, String caracteristiques, BigDecimal prixUnitaire) {
    }

    private record AppelOffrePublieSeed(int numero, String objet, String description, String categorie,
                                        String organisme, BigDecimal budget, ModePassation mode,
                                        int dateLimiteJours, int datePublicationJours,
                                        List<LigneSeed> lignes) {
    }

    private Map<Integer, AppelOffre> appelsOffresPublies(Utilisateur admin, Instant maintenant) {
        LocalDateTime base = LocalDateTime.now(DAKAR);
        Map<Integer, AppelOffre> appelsOffres = new LinkedHashMap<>();
        List<AppelOffrePublieSeed> seeds = List.of(
                new AppelOffrePublieSeed(8, "Acquisition d'équipements de sécurité informatique",
                        "Mise à niveau de la protection du système d'information de l'administration.",
                        "Informatique", "Agence De l'Informatique de l'État", new BigDecimal("22000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 30, 12,
                        List.of(new LigneSeed("Pare-feu matériel", 2,
                                        "Appliance nouvelle génération, haute disponibilité", new BigDecimal("4500000")),
                                new LigneSeed("Serveur de sauvegarde", 2,
                                        "Stockage 16 To, sauvegarde chiffrée", new BigDecimal("5500000")),
                                new LigneSeed("Licence antivirus", 200,
                                        "Protection postes et serveurs, 12 mois", new BigDecimal("25000")))),
                new AppelOffrePublieSeed(9, "Acquisition de groupes électrogènes",
                        "Fourniture et installation de groupes électrogènes de secours pour les sites déconcentrés.",
                        "Énergie", "Ministère de l'Énergie et du Pétrole", new BigDecimal("48000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 45, 8,
                        List.of(new LigneSeed("Groupe électrogène 100 kVA", 3,
                                        "Diesel, insonorisé, coffret automatique", new BigDecimal("12000000")),
                                new LigneSeed("Groupe électrogène 60 kVA", 2,
                                        "Diesel, insonorisé", new BigDecimal("6000000")))),
                new AppelOffrePublieSeed(10, "Construction d'un bâtiment administratif de deux niveaux",
                        "Travaux de construction d'un bâtiment administratif au sein du siège.",
                        "Travaux", "Ministère des Infrastructures et des Transports", new BigDecimal("185000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 60, 15,
                        List.of(new LigneSeed("Gros œuvre", 1,
                                        "Fondations et structure en béton armé", new BigDecimal("110000000")),
                                new LigneSeed("Second œuvre", 1,
                                        "Menuiserie, électricité, plomberie", new BigDecimal("55000000")),
                                new LigneSeed("Étanchéité et peinture", 1,
                                        "Toiture et finitions", new BigDecimal("20000000")))),
                new AppelOffrePublieSeed(11, "Réhabilitation des sanitaires des services centraux",
                        "Travaux de réhabilitation et de mise aux normes des blocs sanitaires.",
                        "Travaux", "Ministère de l'Éducation Nationale", new BigDecimal("27000000"),
                        ModePassation.APPEL_OFFRES_RESTREINT, 35, 10,
                        List.of(new LigneSeed("Rénovation des réseaux de plomberie", 1,
                                        "Alimentation et évacuation", new BigDecimal("12000000")),
                                new LigneSeed("Carrelage et faïence", 1,
                                        "Revêtement des sanitaires", new BigDecimal("9000000")),
                                new LigneSeed("Appareils sanitaires", 1,
                                        "Lavabos, WC et robinetterie", new BigDecimal("6000000")))),
                new AppelOffrePublieSeed(12, "Fourniture de médicaments et consommables médicaux",
                        "Approvisionnement annuel en médicaments essentiels et consommables médicaux.",
                        "Santé", "Ministère de la Santé et de l'Action Sociale", new BigDecimal("95000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 40, 6,
                        List.of(new LigneSeed("Médicaments essentiels", 1,
                                        "Liste nationale, lot annuel", new BigDecimal("65000000")),
                                new LigneSeed("Consommables médicaux", 1,
                                        "Gants, seringues et pansements", new BigDecimal("30000000")))),
                new AppelOffrePublieSeed(13, "Acquisition de matériel médical pour les centres de santé",
                        "Dotation des centres de santé en équipements de diagnostic.",
                        "Santé", "Ministère de la Santé et de l'Action Sociale", new BigDecimal("72000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 50, 4,
                        List.of(new LigneSeed("Échographe portable", 4,
                                        "Sonde multifréquence, écran couleur", new BigDecimal("9000000")),
                                new LigneSeed("Table d'examen", 10,
                                        "Réglable, revêtement médical", new BigDecimal("1200000")),
                                new LigneSeed("Stérilisateur", 6,
                                        "Autoclave 24 litres", new BigDecimal("2000000")))),
                new AppelOffrePublieSeed(14, "Entretien et maintenance des véhicules de service",
                        "Contrat annuel d'entretien préventif et curatif de la flotte administrative.",
                        "Services", "Ministère des Infrastructures et des Transports", new BigDecimal("15000000"),
                        ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, 25, 9,
                        List.of(new LigneSeed("Entretien préventif", 12,
                                        "Vidanges et contrôles mensuels", new BigDecimal("500000")),
                                new LigneSeed("Réparations mécaniques", 1,
                                        "Forfait annuel pièces et main d'œuvre", new BigDecimal("9000000")))),
                new AppelOffrePublieSeed(15, "Fourniture de carburant et lubrifiants",
                        "Approvisionnement en carburant et lubrifiants pour la flotte administrative.",
                        "Fournitures", "Ville de Dakar", new BigDecimal("38000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 20, 3,
                        List.of(new LigneSeed("Gazole", 40000,
                                        "Litres, livraison sur site", new BigDecimal("800")),
                                new LigneSeed("Essence", 6000,
                                        "Litres", new BigDecimal("850")),
                                new LigneSeed("Huile moteur", 400,
                                        "Bidons de 5 litres", new BigDecimal("25000")))),
                new AppelOffrePublieSeed(16, "Installation de panneaux solaires photovoltaïques",
                        "Fourniture, installation et mise en service de centrales solaires.",
                        "Énergie", "Société Nationale d'Électricité", new BigDecimal("88000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 55, 7,
                        List.of(new LigneSeed("Panneaux solaires 550 Wc", 200,
                                        "Modules monocristallins", new BigDecimal("120000")),
                                new LigneSeed("Onduleurs hybrides", 8,
                                        "10 kW, MPPT", new BigDecimal("1800000")),
                                new LigneSeed("Batteries de stockage", 16,
                                        "Lithium 48 V, 200 Ah", new BigDecimal("2500000")))),
                new AppelOffrePublieSeed(17, "Fourniture de mobilier scolaire",
                        "Équipement des salles de classe en mobilier neuf.",
                        "Mobilier", "Ministère de l'Éducation Nationale", new BigDecimal("42000000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 30, 11,
                        List.of(new LigneSeed("Tables-bancs", 1500,
                                        "Bois massif, deux places", new BigDecimal("22000")),
                                new LigneSeed("Chaises de bureau", 120,
                                        "Métal et tissu", new BigDecimal("35000")),
                                new LigneSeed("Tableaux muraux", 80,
                                        "120 x 90 cm", new BigDecimal("45000")))),
                new AppelOffrePublieSeed(18, "Acquisition de matériel pédagogique et didactique",
                        "Dotation des établissements en matériel pédagogique.",
                        "Éducation", "Ministère de l'Éducation Nationale", new BigDecimal("13500000"),
                        ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, 28, 5,
                        List.of(new LigneSeed("Kits de sciences", 100,
                                        "Physique, chimie, biologie", new BigDecimal("75000")),
                                new LigneSeed("Cartes murales", 200,
                                        "Géographie et histoire", new BigDecimal("15000")),
                                new LigneSeed("Matériel de géométrie", 150,
                                        "Équerres, compas, rapporteurs", new BigDecimal("20000")))),
                new AppelOffrePublieSeed(19, "Nettoyage des locaux administratifs",
                        "Prestation de nettoyage quotidien des locaux administratifs.",
                        "Services", "Ville de Dakar", new BigDecimal("9600000"),
                        ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, 18, 2,
                        List.of(new LigneSeed("Nettoyage quotidien", 12,
                                        "Prestation mensuelle", new BigDecimal("650000")),
                                new LigneSeed("Grand nettoyage trimestriel", 4,
                                        "Vitrerie et désinfection", new BigDecimal("450000")))),
                new AppelOffrePublieSeed(20, "Sécurité et gardiennage des sites administratifs",
                        "Prestation de gardiennage et de surveillance des sites administratifs.",
                        "Services", "Ministère de l'Intérieur", new BigDecimal("24000000"),
                        ModePassation.APPEL_OFFRES_RESTREINT, 32, 13,
                        List.of(new LigneSeed("Gardiennage permanent", 12,
                                        "Agents 24h/24 et 7j/7", new BigDecimal("1800000")),
                                new LigneSeed("Ronde de surveillance", 12,
                                        "Patrouilles nocturnes", new BigDecimal("200000")))),
                new AppelOffrePublieSeed(21, "Fourniture de denrées alimentaires pour les cantines",
                        "Approvisionnement des cantines administratives et sociales.",
                        "Fournitures", "Ministère de la Santé et de l'Action Sociale", new BigDecimal("18500000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 22, 6,
                        List.of(new LigneSeed("Riz parfumé", 2000,
                                        "Sacs de 50 kg", new BigDecimal("18000")),
                                new LigneSeed("Huile végétale", 500,
                                        "Bidons de 20 litres", new BigDecimal("22000")),
                                new LigneSeed("Légumes secs", 800,
                                        "Sacs de 25 kg", new BigDecimal("14000")))),
                new AppelOffrePublieSeed(22, "Acquisition de logiciels de gestion administrative",
                        "Acquisition et déploiement de solutions logicielles métier.",
                        "Informatique", "Agence De l'Informatique de l'État", new BigDecimal("31000000"),
                        ModePassation.APPEL_OFFRES_RESTREINT, 38, 9,
                        List.of(new LigneSeed("Licences de gestion documentaire", 50,
                                        "Utilisateurs, 12 mois", new BigDecimal("150000")),
                                new LigneSeed("Module de gestion budgétaire", 1,
                                        "Déploiement et paramétrage", new BigDecimal("18000000")),
                                new LigneSeed("Formation des utilisateurs", 1,
                                        "Sessions sur site", new BigDecimal("4000000")))),
                new AppelOffrePublieSeed(23, "Maintenance préventive des climatiseurs",
                        "Contrat annuel d'entretien des installations de climatisation.",
                        "Maintenance", "Ministère de l'Énergie et du Pétrole", new BigDecimal("6800000"),
                        ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, 16, 4,
                        List.of(new LigneSeed("Entretien semestriel", 120,
                                        "Nettoyage et recharge de gaz", new BigDecimal("35000")),
                                new LigneSeed("Réparation des unités", 1,
                                        "Forfait annuel", new BigDecimal("2600000")))),
                new AppelOffrePublieSeed(24, "Aménagement des espaces verts du siège",
                        "Travaux d'aménagement paysager et d'irrigation.",
                        "Travaux", "Ville de Dakar", new BigDecimal("14500000"),
                        ModePassation.APPEL_OFFRES_OUVERT, 42, 14,
                        List.of(new LigneSeed("Plantation d'arbres", 200,
                                        "Essences locales", new BigDecimal("25000")),
                                new LigneSeed("Gazon et massifs", 1,
                                        "Fourniture et pose", new BigDecimal("6000000")),
                                new LigneSeed("Système d'irrigation", 1,
                                        "Goutte-à-goutte automatisé", new BigDecimal("3500000")))),
                new AppelOffrePublieSeed(25, "Fourniture de produits d'entretien et d'hygiène",
                        "Achat de produits d'entretien pour l'ensemble des services.",
                        "Fournitures", "Ministère de l'Éducation Nationale", new BigDecimal("5200000"),
                        ModePassation.DEMANDE_RENSEIGNEMENTS_PRIX, 15, 3,
                        List.of(new LigneSeed("Détergents", 600,
                                        "Bidons de 5 litres", new BigDecimal("4500")),
                                new LigneSeed("Papier hygiénique", 1200,
                                        "Rouleaux professionnels", new BigDecimal("1200")),
                                new LigneSeed("Sacs poubelle", 3000,
                                        "Grand format", new BigDecimal("350")))),
                new AppelOffrePublieSeed(26, "Acquisition de matériel d'impression et de reprographie",
                        "Renouvellement du parc d'impression des services centraux.",
                        "Informatique", "Ministère de l'Économie, du Plan et de la Coopération",
                        new BigDecimal("16800000"), ModePassation.APPEL_OFFRES_OUVERT, 26, 8,
                        List.of(new LigneSeed("Photocopieur multifonction", 6,
                                        "A3, 45 ppm, réseau", new BigDecimal("2200000")),
                                new LigneSeed("Imprimante laser couleur", 20,
                                        "A4, recto-verso", new BigDecimal("350000")),
                                new LigneSeed("Consommables d'impression", 1,
                                        "Lot initial", new BigDecimal("800000")))),
                new AppelOffrePublieSeed(27, "Prestation de services de formation professionnelle",
                        "Formation des agents en management et en outils numériques.",
                        "Services", "Ministère de la Fonction Publique", new BigDecimal("12500000"),
                        ModePassation.APPEL_OFFRES_RESTREINT, 24, 5,
                        List.of(new LigneSeed("Formation en management", 1,
                                        "5 sessions de 3 jours", new BigDecimal("5000000")),
                                new LigneSeed("Formation aux outils numériques", 1,
                                        "8 sessions de 2 jours", new BigDecimal("4500000")),
                                new LigneSeed("Accompagnement et évaluation", 1,
                                        "Suivi post-formation", new BigDecimal("3000000")))));

        for (AppelOffrePublieSeed seed : seeds) {
            Marche marche = marche(seed.numero(), seed.objet(), seed.description(), seed.budget(), seed.mode(),
                    StatutMarche.PUBLIE, null, admin, jours(maintenant, seed.datePublicationJours() + 5));
            marche.setDateValidation(jours(maintenant, seed.datePublicationJours() + 2));
            marche.setDatePublication(jours(maintenant, seed.datePublicationJours()));
            for (LigneSeed ligne : seed.lignes()) {
                marche.ajouterLigne(ligneMarche(ligne.designation(), ligne.quantite(),
                        ligne.caracteristiques(), ligne.prixUnitaire()));
            }
            criteres(marche);
            marcheRepository.save(marche);

            AppelOffre appelOffre = appelOffre(seed.numero(), marche, seed.objet(), seed.description(),
                    seed.categorie(), seed.organisme(), StatutAppelOffre.PUBLIE,
                    base.plusDays(seed.dateLimiteJours()), admin,
                    jours(maintenant, seed.datePublicationJours()));
            cahierDesCharges(appelOffre);
            documentsObligatoires(appelOffre);
            appelsOffres.put(seed.numero(), appelOffre);
        }
        return appelsOffres;
    }

    private void offresAEvaluer(Map<Integer, AppelOffre> appelsOffres, Fournisseur tech, Fournisseur batimat,
                                Fournisseur digital, Fournisseur globalOffice, Instant maintenant) {
        offre(appelsOffres.get(8), tech, new BigDecimal("20500000"), 30, StatutOffre.SOUMISE,
                jours(maintenant, 2), "Offre technique et financière conforme au cahier des charges.");
        offre(appelsOffres.get(8), digital, new BigDecimal("21300000"), 35, StatutOffre.SOUMISE,
                jours(maintenant, 1), "Offre technique et financière en attente d'évaluation.");

        offre(appelsOffres.get(9), batimat, new BigDecimal("45800000"), 60, StatutOffre.SOUMISE,
                jours(maintenant, 2), "Proposition conforme aux spécifications des groupes électrogènes.");
        offre(appelsOffres.get(9), tech, new BigDecimal("46500000"), 55, StatutOffre.SOUMISE,
                jours(maintenant, 1), "Offre technique et financière en attente d'évaluation.");

        offre(appelsOffres.get(10), tech, new BigDecimal("172000000"), 180, StatutOffre.SOUMISE,
                jours(maintenant, 3), "Offre de construction détaillée avec planning d'exécution.");
        offre(appelsOffres.get(10), digital, new BigDecimal("179500000"), 200, StatutOffre.SOUMISE,
                jours(maintenant, 2), "Offre technique et financière en attente d'évaluation.");

        offre(appelsOffres.get(11), batimat, new BigDecimal("25400000"), 45, StatutOffre.SOUMISE,
                jours(maintenant, 2), "Offre de réhabilitation conforme aux normes en vigueur.");
        offre(appelsOffres.get(11), globalOffice, new BigDecimal("26200000"), 50, StatutOffre.SOUMISE,
                jours(maintenant, 1), "Offre technique et financière en attente d'évaluation.");

        offre(appelsOffres.get(12), digital, new BigDecimal("89500000"), 30, StatutOffre.SOUMISE,
                jours(maintenant, 2), "Offre d'approvisionnement en médicaments et consommables.");
        offre(appelsOffres.get(12), tech, new BigDecimal("92000000"), 25, StatutOffre.SOUMISE,
                jours(maintenant, 1), "Offre technique et financière en attente d'évaluation.");
    }

    private Utilisateur utilisateur(String nom, String prenom, String email, String motDePasse,
                                    Role role, String telephone) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email.toLowerCase(Locale.ROOT));
        u.setMotDePasse(passwordEncoder.encode(motDePasse));
        u.setRole(role);
        u.setTelephone(telephone);
        u.setStatut(StatutUtilisateur.ACTIF);
        return utilisateurRepository.save(u);
    }

    private Fournisseur fournisseur(String raisonSociale, String ninea, String rccm, String adresse,
                                    String telephone, String email, String representant,
                                    Utilisateur utilisateur, StatutValidation statut, String motifRejet) {
        Fournisseur f = new Fournisseur();
        f.setRaisonSociale(raisonSociale);
        f.setNinea(ninea);
        f.setRccm(rccm);
        f.setAdresse(adresse);
        f.setTelephone(telephone);
        f.setEmail(email);
        f.setRepresentant(representant);
        f.setUtilisateur(utilisateur);
        f.setStatutValidation(statut);
        f.setMotifRejet(motifRejet);
        return fournisseurRepository.save(f);
    }

    private Materiel materiel(String designation, String categorie, String description,
                              String caracteristiques) {
        Materiel m = new Materiel();
        m.setDesignation(designation);
        m.setCategorie(categorie);
        m.setDescription(description);
        m.setCaracteristiques(caracteristiques);
        m.setActif(true);
        return materielRepository.save(m);
    }

    private ExpressionBesoin besoin(String prefixe, int numero, String objet, String description,
                                    String service, BigDecimal budget, LocalDate dateSouhaitee,
                                    StatutBesoin statut, Utilisateur createur, Instant dateCreation,
                                    LigneBesoin... lignes) {
        ExpressionBesoin eb = new ExpressionBesoin();
        eb.setReference(String.format("%s-%s-%03d", prefixe, annee, numero));
        eb.setObjet(objet);
        eb.setDescription(description);
        eb.setServiceDemandeur(service);
        eb.setBudgetEstimatif(budget);
        eb.setDateSouhaitee(dateSouhaitee);
        eb.setStatut(statut);
        eb.setCreateur(createur);
        if (dateCreation != null) {
            eb.setDateCreation(dateCreation);
            if (statut != StatutBesoin.BROUILLON) {
                eb.setDateSoumission(dateCreation.plus(1, ChronoUnit.DAYS));
            }
            if (statut == StatutBesoin.VALIDEE) {
                eb.setDateValidation(dateCreation.plus(4, ChronoUnit.DAYS));
            }
        }
        for (LigneBesoin ligne : lignes) {
            eb.ajouterLigne(ligne);
        }
        return besoinRepository.save(eb);
    }

    private LigneBesoin ligneBesoin(String designation, int quantite, String caracteristiques) {
        LigneBesoin ligne = new LigneBesoin();
        ligne.setDesignation(designation);
        ligne.setQuantite(quantite);
        ligne.setCaracteristiques(caracteristiques);
        return ligne;
    }

    private Marche marche(int numero, String objet, String description, BigDecimal budget,
                          ModePassation mode, StatutMarche statut, ExpressionBesoin besoin,
                          Utilisateur createur, Instant dateCreation) {
        Marche m = new Marche();
        m.setReference(String.format("MAR-%s-%03d", annee, numero));
        m.setObjet(objet);
        m.setDescription(description);
        m.setBudgetPrevisionnel(budget);
        m.setModePassation(mode);
        m.setStatut(statut);
        m.setExpressionBesoin(besoin);
        m.setCreateur(createur);
        m.setDateCreation(dateCreation);
        return m;
    }

    private LigneMarche ligneMarche(String designation, int quantite, String caracteristiques,
                                    BigDecimal prixUnitaire) {
        LigneMarche ligne = new LigneMarche();
        ligne.setDesignation(designation);
        ligne.setQuantite(quantite);
        ligne.setCaracteristiques(caracteristiques);
        ligne.setPrixUnitaireEstime(prixUnitaire);
        return ligne;
    }

    private void lignesMarche(Marche marche, LigneMarche... lignes) {
        for (LigneMarche ligne : lignes) {
            marche.ajouterLigne(ligne);
        }
    }

    private void criteres(Marche marche) {
        marche.ajouterCritere(critere("Qualité et conformité technique", TypeCritere.TECHNIQUE, 40,
                "Adéquation de la solution aux spécifications techniques."));
        marche.ajouterCritere(critere("Offre financière", TypeCritere.FINANCIER, 35,
                "Pertinence et compétitivité du prix proposé."));
        marche.ajouterCritere(critere("Délai de livraison", TypeCritere.TECHNIQUE, 15,
                "Capacité à respecter le délai de livraison demandé."));
        marche.ajouterCritere(critere("Références et garanties", TypeCritere.TECHNIQUE, 10,
                "Références similaires et garanties proposées."));
    }

    private CritereEvaluation critere(String libelle, TypeCritere type, double ponderation,
                                      String description) {
        CritereEvaluation c = new CritereEvaluation();
        c.setLibelle(libelle);
        c.setType(type);
        c.setPonderation(ponderation);
        c.setDescription(description);
        return c;
    }

    private AppelOffre appelOffre(int numero, Marche marche, String objet, String description,
                                  String categorie, String organisme, StatutAppelOffre statut,
                                  LocalDateTime dateLimite, Utilisateur publiePar, Instant datePublication) {
        AppelOffre ao = new AppelOffre();
        ao.setReference(String.format("AO-%s-%03d", annee, numero));
        ao.setObjet(objet);
        ao.setDescription(description);
        ao.setCategorie(categorie);
        ao.setOrganisme(organisme);
        ao.setConditionsParticipation("Fournisseur régulièrement enregistré, à jour de ses obligations fiscales "
                + "et disposant des capacités techniques et financières requises.");
        ao.setDocumentsObligatoires("Offre technique, offre financière, pièces administratives, "
                + "références similaires et garanties.");
        ao.setModalitesSoumission("Dépôt électronique sur la plateforme MarchéPro avant la date limite. "
                + "Les offres reçues après la date limite sont irrecevables.");
        ao.setBudgetPublic(true);
        ao.setStatut(statut);
        ao.setDateLimite(dateLimite);
        ao.setMarche(marche);
        ao.setPubliePar(publiePar);
        ao.setDatePublication(datePublication);
        return appelOffreRepository.save(ao);
    }

    private Offre offre(AppelOffre appelOffre, Fournisseur fournisseur, BigDecimal montant,
                        int delaiJours, StatutOffre statut, Instant dateSoumission, String observations) {
        compteurOffre++;
        Offre offre = new Offre();
        offre.setReference(String.format("OFF-%s-%03d", annee, compteurOffre));
        if (statut != StatutOffre.BROUILLON) {
            offre.setReferenceAccuse(String.format("ACC-%s-%08d", annee, 10000000 + compteurOffre));
            offre.setDateSoumission(dateSoumission);
        }
        offre.setMontant(montant);
        offre.setDelaiLivraisonJours(delaiJours);
        offre.setInformationsTechniques("Solution conforme au cahier des charges, matériel neuf et garanti.");
        offre.setObservationsFournisseur(observations);
        offre.setStatut(statut);
        offre.setAppelOffre(appelOffre);
        offre.setFournisseur(fournisseur);
        offre.setDateCreation(dateSoumission != null ? dateSoumission.minus(2, ChronoUnit.DAYS)
                : Instant.now());

        int totalQuantite = appelOffre.getMarche().getLignes().stream()
                .mapToInt(LigneMarche::getQuantite).sum();
        BigDecimal prixUnitaire = totalQuantite > 0
                ? montant.divide(BigDecimal.valueOf(totalQuantite), 2, RoundingMode.HALF_UP)
                : montant;
        for (LigneMarche ligneMarche : appelOffre.getMarche().getLignes()) {
            LigneOffre ligne = new LigneOffre();
            ligne.setDesignation(ligneMarche.getDesignation());
            ligne.setQuantite(ligneMarche.getQuantite());
            ligne.setPrixUnitaire(prixUnitaire);
            ligne.setCaracteristiques(ligneMarche.getCaracteristiques());
            offre.ajouterLigne(ligne);
        }
        return offreRepository.save(offre);
    }

    private void evaluer(Offre offre, Utilisateur membre, int noteTechniqueBase, int noteFinanciere) {
        Marche marche = offre.getAppelOffre().getMarche();
        Evaluation evaluation = new Evaluation();
        evaluation.setOffre(offre);
        evaluation.setMembre(membre);
        evaluation.setObservations("Évaluation réalisée sur la base des critères du dossier d'appel d'offres.");

        BigDecimal totalTechnique = BigDecimal.ZERO;
        BigDecimal totalFinancier = BigDecimal.ZERO;
        int index = 0;
        for (CritereEvaluation critere : marche.getCriteres()) {
            int note = critere.getType() == TypeCritere.FINANCIER
                    ? noteFinanciere
                    : noteTechniqueBase - (index % 2);
            NoteEvaluation noteEvaluation = new NoteEvaluation();
            noteEvaluation.setCritere(critere);
            noteEvaluation.setNote(BigDecimal.valueOf(Math.max(0, Math.min(20, note))));
            noteEvaluation.setCommentaire("Note attribuée par " + membre.getNomComplet());
            evaluation.ajouterNote(noteEvaluation);
            BigDecimal pondere = noteEvaluation.getNote()
                    .multiply(BigDecimal.valueOf(critere.getPonderation()))
                    .divide(BigDecimal.valueOf(20), 4, RoundingMode.HALF_UP);
            if (critere.getType() == TypeCritere.TECHNIQUE) {
                totalTechnique = totalTechnique.add(pondere);
            } else {
                totalFinancier = totalFinancier.add(pondere);
            }
            index++;
        }
        evaluation.setNoteTechnique(totalTechnique.setScale(2, RoundingMode.HALF_UP));
        evaluation.setNoteFinanciere(totalFinancier.setScale(2, RoundingMode.HALF_UP));
        evaluation.setNoteGlobale(totalTechnique.add(totalFinancier).setScale(2, RoundingMode.HALF_UP));
        evaluationRepository.save(evaluation);
    }

    private Attribution attribution(Marche marche, Offre offre, Utilisateur proposePar,
                                    Utilisateur validePar, StatutAttribution statut,
                                    Instant dateProposition, Instant dateDecision, String observations) {
        Attribution attribution = new Attribution();
        attribution.setMarche(marche);
        attribution.setOffre(offre);
        attribution.setFournisseur(offre.getFournisseur());
        attribution.setMontant(offre.getMontant());
        attribution.setStatut(statut);
        attribution.setDateProposition(dateProposition);
        attribution.setDateDecision(dateDecision);
        attribution.setProposePar(proposePar);
        attribution.setValidePar(validePar);
        attribution.setMotif("Offre la mieux-disante après évaluation technique et financière.");
        attribution.setObservations(observations);
        return attributionRepository.save(attribution);
    }

    private int[] quantite(Marche marche, int indexLigne, int quantite) {
        return new int[]{marche.getLignes().get(indexLigne).getId().intValue(), quantite};
    }

    private void livraison(Marche marche, Fournisseur fournisseur, LocalDate datePrevue,
                           LocalDate dateEffective, StatutLivraison statut, StatutReception statutReception,
                           LocalDate dateReception, String observations, String reserves, int[]... lignes) {
        Livraison livraison = new Livraison();
        livraison.setMarche(marche);
        livraison.setFournisseur(fournisseur);
        livraison.setDatePrevue(datePrevue);
        livraison.setDateEffective(dateEffective);
        livraison.setStatut(statut);
        livraison.setObservations(observations);
        for (int[] ligne : lignes) {
            LigneMarche ligneMarche = marche.getLignes().stream()
                    .filter(l -> l.getId().equals((long) ligne[0]))
                    .findFirst().orElseThrow();
            LigneLivraison ligneLivraison = new LigneLivraison();
            ligneLivraison.setLigneMarche(ligneMarche);
            ligneLivraison.setQuantiteLivree(ligne[1]);
            livraison.ajouterLigne(ligneLivraison);
        }
        if (statutReception != null) {
            Reception reception = new Reception();
            reception.setLivraison(livraison);
            reception.setDateReception(dateReception);
            reception.setStatut(statutReception);
            reception.setObservations("Réception effectuée par le service compétent.");
            reception.setReserves(reserves);
            livraison.setReception(reception);
        }
        livraisonRepository.save(livraison);
    }

    private void notification(Utilisateur utilisateur, String titre, String message, String type, String lien) {
        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLien(lien);
        notification.setLue(false);
        notificationRepository.save(notification);
    }


    private void documentsObligatoires(AppelOffre appelOffre) {
        documentObligatoire(appelOffre, "Offre technique", DocumentType.OFFRE_TECHNIQUE,
                "Mémoire technique, descriptifs et fiches produits.", 1);
        documentObligatoire(appelOffre, "Offre financière", DocumentType.OFFRE_FINANCIERE,
                "Bordereau des prix et devis détaillé.", 2);
        documentObligatoire(appelOffre, "Documents administratifs", DocumentType.DOCUMENT_ADMINISTRATIF,
                "NINEA, RCCM, attestations fiscales et sociales.", 3);
    }

    private void documentObligatoire(AppelOffre appelOffre, String libelle, DocumentType type,
                                     String description, int ordre) {
        DocumentObligatoire document = new DocumentObligatoire();
        document.setAppelOffre(appelOffre);
        document.setLibelle(libelle);
        document.setDescription(description);
        document.setType(type);
        document.setObligatoire(true);
        document.setOrdre(ordre);
        documentObligatoireRepository.save(document);
    }

    private void cahierDesCharges(AppelOffre appelOffre) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("CAHIER DES CHARGES"));
            document.add(new Paragraph("Référence : " + appelOffre.getReference()));
            document.add(new Paragraph("Objet : " + appelOffre.getObjet()));
            document.add(new Paragraph("Organisme : " + appelOffre.getOrganisme()));
            document.add(new Paragraph("Date limite de soumission : " + appelOffre.getDateLimite()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(appelOffre.getDescription()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Conditions de participation : " + appelOffre.getConditionsParticipation()));
            document.add(new Paragraph("Documents obligatoires : " + appelOffre.getDocumentsObligatoires()));
            document.close();

            Path racine = Paths.get(uploadDir).toAbsolutePath().normalize();
            String nomStockage = "APPEL_OFFRE/" + appelOffre.getId() + "/cahier-des-charges.pdf";
            Path destination = racine.resolve(nomStockage).normalize();
            Files.createDirectories(destination.getParent());
            Files.write(destination, out.toByteArray());

            Document doc = new Document();
            doc.setNom("Cahier des charges - " + appelOffre.getReference() + ".pdf");
            doc.setNomStockage(nomStockage);
            doc.setContentType("application/pdf");
            doc.setTaille(out.size());
            doc.setType(DocumentType.CAHIER_DES_CHARGES);
            doc.setEntiteType("APPEL_OFFRE");
            doc.setEntiteId(appelOffre.getId());
            documentRepository.save(doc);
        } catch (Exception ex) {
            log.warn("Impossible de générer le cahier des charges pour {} : {}",
                    appelOffre.getReference(), ex.getMessage());
        }
    }
}
