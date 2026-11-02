package com.marchepro.besoin.dto;

import com.marchepro.besoin.entity.ExpressionBesoin;
import com.marchepro.besoin.entity.StatutBesoin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpressionBesoinDto(
        Long id,
        String reference,
        String objet,
        String description,
        String serviceDemandeur,
        BigDecimal budgetEstimatif,
        LocalDate dateSouhaitee,
        String justification,
        StatutBesoin statut,
        String motifRejet,
        Instant dateCreation,
        Instant dateSoumission,
        Instant dateValidation,
        Long createurId,
        String createurNom,
        Long marcheId,
        List<LigneBesoinDto> lignes
) {
    public static ExpressionBesoinDto from(ExpressionBesoin e) {
        return from(e, null);
    }

    public static ExpressionBesoinDto from(ExpressionBesoin e, Long marcheId) {
        return new ExpressionBesoinDto(
                e.getId(), e.getReference(), e.getObjet(), e.getDescription(), e.getServiceDemandeur(),
                e.getBudgetEstimatif(), e.getDateSouhaitee(), e.getJustification(), e.getStatut(),
                e.getMotifRejet(), e.getDateCreation(), e.getDateSoumission(), e.getDateValidation(),
                e.getCreateur() != null ? e.getCreateur().getId() : null,
                e.getCreateur() != null ? e.getCreateur().getNomComplet() : null,
                marcheId,
                e.getLignes().stream().map(LigneBesoinDto::from).toList()
        );
    }
}
