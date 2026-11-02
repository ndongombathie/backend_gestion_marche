package com.marchepro.offre.dto;

import com.marchepro.common.document.DocumentDto;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.entity.StatutOffre;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record OffreDto(
        Long id,
        String reference,
        String referenceAccuse,
        BigDecimal montant,
        Integer delaiLivraisonJours,
        String informationsTechniques,
        String observationsFournisseur,
        StatutOffre statut,
        Instant dateCreation,
        Instant dateSoumission,
        String motifRejet,
        Long appelOffreId,
        String appelOffreReference,
        String appelOffreObjet,
        LocalDateTime dateLimite,
        Long fournisseurId,
        String raisonSociale,
        String ninea,
        long nombreEvaluations,
        List<DocumentDto> documents,
        List<LigneOffreDto> lignes
) {
    public static OffreDto from(Offre o, long nombreEvaluations, List<DocumentDto> documents) {
        return new OffreDto(
                o.getId(), o.getReference(), o.getReferenceAccuse(), o.getMontant(),
                o.getDelaiLivraisonJours(), o.getInformationsTechniques(), o.getObservationsFournisseur(),
                o.getStatut(), o.getDateCreation(), o.getDateSoumission(), o.getMotifRejet(),
                o.getAppelOffre() != null ? o.getAppelOffre().getId() : null,
                o.getAppelOffre() != null ? o.getAppelOffre().getReference() : null,
                o.getAppelOffre() != null ? o.getAppelOffre().getObjet() : null,
                o.getAppelOffre() != null ? o.getAppelOffre().getDateLimite() : null,
                o.getFournisseur() != null ? o.getFournisseur().getId() : null,
                o.getFournisseur() != null ? o.getFournisseur().getRaisonSociale() : null,
                o.getFournisseur() != null ? o.getFournisseur().getNinea() : null,
                nombreEvaluations,
                documents,
                o.getLignes().stream().map(LigneOffreDto::from).toList()
        );
    }
}
