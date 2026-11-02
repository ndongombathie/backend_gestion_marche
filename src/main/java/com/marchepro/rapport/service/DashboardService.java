package com.marchepro.rapport.service;

import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.repository.AttributionRepository;
import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.repository.FournisseurRepository;
import com.marchepro.fournisseur.service.FournisseurService;
import com.marchepro.livraison.entity.StatutLivraison;
import com.marchepro.livraison.repository.LivraisonRepository;
import com.marchepro.livraison.service.LivraisonService;
import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.entity.StatutMarche;
import com.marchepro.marche.repository.MarcheRepository;
import com.marchepro.marche.service.MarcheService;
import com.marchepro.offre.entity.StatutOffre;
import com.marchepro.offre.repository.OffreRepository;
import com.marchepro.offre.service.OffreService;
import com.marchepro.rapport.dto.DashboardDto;
import com.marchepro.rapport.dto.FournisseurDashboardDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class DashboardService {

    private static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");
    private static final List<StatutOffre> OFFRES_RECUES = List.of(
            StatutOffre.SOUMISE, StatutOffre.EN_EVALUATION, StatutOffre.EVALUEE,
            StatutOffre.RETENUE, StatutOffre.NON_RETENUE, StatutOffre.DISQUALIFIEE);
    private static final List<StatutOffre> OFFRES_EN_COURS = List.of(
            StatutOffre.SOUMISE, StatutOffre.EN_EVALUATION, StatutOffre.EVALUEE);

    private final MarcheRepository marcheRepository;
    private final AppelOffreRepository appelOffreRepository;
    private final AttributionRepository attributionRepository;
    private final OffreRepository offreRepository;
    private final LivraisonRepository livraisonRepository;
    private final FournisseurRepository fournisseurRepository;
    private final FournisseurService fournisseurService;
    private final MarcheService marcheService;
    private final OffreService offreService;
    private final LivraisonService livraisonService;

    public DashboardService(MarcheRepository marcheRepository,
                            AppelOffreRepository appelOffreRepository,
                            AttributionRepository attributionRepository,
                            OffreRepository offreRepository,
                            LivraisonRepository livraisonRepository,
                            FournisseurRepository fournisseurRepository,
                            FournisseurService fournisseurService,
                            MarcheService marcheService,
                            OffreService offreService,
                            LivraisonService livraisonService) {
        this.marcheRepository = marcheRepository;
        this.appelOffreRepository = appelOffreRepository;
        this.attributionRepository = attributionRepository;
        this.offreRepository = offreRepository;
        this.livraisonRepository = livraisonRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.fournisseurService = fournisseurService;
        this.marcheService = marcheService;
        this.offreService = offreService;
        this.livraisonService = livraisonService;
    }

    @Transactional(readOnly = true)
    public DashboardDto tableauDeBord(LocalDate debut, LocalDate fin) {
        LocalDate debutEffectif = debut != null ? debut : LocalDate.now(DAKAR).minusMonths(6).withDayOfMonth(1);
        LocalDate finEffective = fin != null ? fin : LocalDate.now(DAKAR);
        Instant debutInstant = debut(debutEffectif);
        Instant finInstant = fin(finEffective);

        long recues = livraisonRepository.countByStatut(StatutLivraison.RECEPTIONNEE);
        long dansDelais = livraisonRepository.countLivraisonsDansDelais();
        double taux = recues == 0 ? 0d : Math.round((dansDelais * 1000.0) / recues) / 10.0;

        List<DashboardDto.Repartition> parStatut = marcheRepository.repartitionParStatut().stream()
                .map(row -> new DashboardDto.Repartition(
                        ((StatutMarche) row[0]).name(), ((Number) row[1]).longValue()))
                .toList();

        List<DashboardDto.Repartition> parCategorie = marcheRepository.repartitionParCategorie().stream()
                .map(row -> new DashboardDto.Repartition(
                        String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        List<DashboardDto.PointMois> evolution = marcheRepository.evolutionMensuelle(debutInstant).stream()
                .map(row -> new DashboardDto.PointMois(
                        String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        List<DashboardDto.ClassementFournisseur> classement = attributionRepository.classementFournisseurs()
                .stream()
                .map(row -> new DashboardDto.ClassementFournisseur(
                        String.valueOf(row[0]), ((Number) row[1]).longValue(),
                        row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO))
                .toList();

        List<Marche> recents = marcheRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dateCreation"))).getContent();

        return new DashboardDto(
                marcheRepository.count(),
                appelOffreRepository.countByStatut(StatutAppelOffre.PUBLIE),
                nz(marcheRepository.totalBudgetPrevisionnel()),
                nz(attributionRepository.totalMontantAttribue()),
                fournisseurRepository.countByStatutValidation(StatutValidation.VALIDE),
                offreRepository.countByStatutIn(OFFRES_RECUES),
                livraisonRepository.findEnRetard(LocalDate.now(DAKAR)).size(),
                taux,
                parStatut,
                parCategorie,
                evolution,
                classement,
                recents.stream().map(marcheService::mapper).toList(),
                livraisonService.enRetard()
        );
    }

    @Transactional(readOnly = true)
    public FournisseurDashboardDto tableauDeBordFournisseur() {
        Fournisseur fournisseur = fournisseurService.fournisseurCourant();
        List<com.marchepro.offre.entity.Offre> offres =
                offreRepository.findByFournisseurIdOrderByDateCreationDesc(fournisseur.getId());
        long soumises = offres.stream().filter(o -> o.getStatut() != StatutOffre.BROUILLON).count();

        long disponibles = appelOffreRepository.findBientotExpires(
                ZonedDateTime.now(DAKAR).toLocalDateTime(),
                ZonedDateTime.now(DAKAR).plusYears(10).toLocalDateTime(),
                PageRequest.of(0, 1)).getTotalElements();

        List<com.marchepro.livraison.dto.LivraisonDto> livraisons =
                livraisonRepository.findByFournisseurIdOrderByDatePrevueDesc(fournisseur.getId())
                        .stream().limit(5).map(livraisonService::mapper).toList();

        return new FournisseurDashboardDto(
                disponibles,
                soumises,
                offreRepository.countByFournisseurIdAndStatutIn(fournisseur.getId(), OFFRES_EN_COURS),
                attributionRepository.countByFournisseurIdAndStatut(fournisseur.getId(), StatutAttribution.VALIDEE),
                attributionRepository.countMarchesEnExecutionFournisseur(fournisseur.getId()),
                livraisonRepository.countByFournisseurId(fournisseur.getId()),
                fournisseur.getStatutValidation().name(),
                offres.stream().limit(5).map(offreService::mapper).toList(),
                livraisons
        );
    }

    private static BigDecimal nz(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }

    private static Instant debut(LocalDate date) {
        return date.atStartOfDay(DAKAR).toInstant();
    }

    private static Instant fin(LocalDate date) {
        return date.plusDays(1).atStartOfDay(DAKAR).toInstant();
    }
}
