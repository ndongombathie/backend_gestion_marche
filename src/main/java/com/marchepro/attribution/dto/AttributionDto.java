package com.marchepro.attribution.dto;

import com.marchepro.attribution.entity.Attribution;
import com.marchepro.attribution.entity.StatutAttribution;

import java.math.BigDecimal;
import java.time.Instant;

public record AttributionDto(
        Long id,
        BigDecimal montant,
        StatutAttribution statut,
        Instant dateProposition,
        Instant dateDecision,
        String motif,
        String observations,
        Long marcheId,
        String marcheReference,
        String marcheObjet,
        Long appelOffreId,
        Long offreId,
        String offreReference,
        Long fournisseurId,
        String raisonSociale,
        String proposePar,
        String validePar
) {
    public static AttributionDto from(Attribution a) {
        return new AttributionDto(
                a.getId(), a.getMontant(), a.getStatut(), a.getDateProposition(), a.getDateDecision(),
                a.getMotif(), a.getObservations(),
                a.getMarche().getId(), a.getMarche().getReference(), a.getMarche().getObjet(),
                a.getOffre().getAppelOffre() != null ? a.getOffre().getAppelOffre().getId() : null,
                a.getOffre().getId(), a.getOffre().getReference(),
                a.getFournisseur().getId(), a.getFournisseur().getRaisonSociale(),
                a.getProposePar() != null ? a.getProposePar().getNomComplet() : null,
                a.getValidePar() != null ? a.getValidePar().getNomComplet() : null
        );
    }
}
