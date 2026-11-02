package com.marchepro.evaluation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConsolidationDto(
        Long appelOffreId,
        String appelOffreReference,
        String appelOffreObjet,
        String statutMarche,
        List<ClassementOffreDto> classement
) {
    public record ClassementOffreDto(
            int rang,
            Long offreId,
            String offreReference,
            String fournisseur,
            BigDecimal montant,
            BigDecimal noteTechnique,
            BigDecimal noteFinanciere,
            BigDecimal noteGlobale,
            long nombreEvaluations,
            boolean retenue
    ) {
    }
}
