package com.marchepro.marche.dto;

import com.marchepro.marche.entity.Marche;
import com.marchepro.marche.entity.ModePassation;
import com.marchepro.marche.entity.StatutMarche;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MarcheDto(
        Long id,
        String reference,
        String objet,
        String description,
        BigDecimal budgetPrevisionnel,
        ModePassation modePassation,
        StatutMarche statut,
        Long expressionBesoinId,
        String expressionBesoinReference,
        String expressionBesoinObjet,
        String createurNom,
        Instant dateCreation,
        Instant dateValidation,
        Instant datePublication,
        LocalDate dateDebutExecution,
        LocalDate dateFinExecution,
        Instant dateCloture,
        String motifAnnulation,
        String observations,
        Long appelOffreId,
        String appelOffreReference,
        Long attributionId,
        String fournisseurAttribue,
        BigDecimal montantAttribue,
        long nombreOffres,
        List<LigneMarcheDto> lignes,
        List<CritereDto> criteres
) {
    public static MarcheDto from(Marche m, Long appelOffreId, String appelOffreReference,
                                 Long attributionId, String fournisseurAttribue, BigDecimal montantAttribue,
                                 long nombreOffres) {
        return new MarcheDto(
                m.getId(), m.getReference(), m.getObjet(), m.getDescription(), m.getBudgetPrevisionnel(),
                m.getModePassation(), m.getStatut(),
                m.getExpressionBesoin() != null ? m.getExpressionBesoin().getId() : null,
                m.getExpressionBesoin() != null ? m.getExpressionBesoin().getReference() : null,
                m.getExpressionBesoin() != null ? m.getExpressionBesoin().getObjet() : null,
                m.getCreateur() != null ? m.getCreateur().getNomComplet() : null,
                m.getDateCreation(), m.getDateValidation(), m.getDatePublication(),
                m.getDateDebutExecution(), m.getDateFinExecution(), m.getDateCloture(),
                m.getMotifAnnulation(), m.getObservations(),
                appelOffreId, appelOffreReference, attributionId, fournisseurAttribue, montantAttribue,
                nombreOffres,
                m.getLignes().stream().map(LigneMarcheDto::from).toList(),
                m.getCriteres().stream().map(CritereDto::from).toList()
        );
    }
}
