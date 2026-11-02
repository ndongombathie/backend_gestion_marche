package com.marchepro.evaluation.dto;

import com.marchepro.evaluation.entity.Evaluation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EvaluationDto(
        Long id,
        Long offreId,
        String offreReference,
        String fournisseur,
        Long membreId,
        String membreNom,
        BigDecimal noteTechnique,
        BigDecimal noteFinanciere,
        BigDecimal noteGlobale,
        String observations,
        Instant dateEvaluation,
        List<NoteEvaluationDto> notes
) {
    public static EvaluationDto from(Evaluation e) {
        return new EvaluationDto(
                e.getId(),
                e.getOffre().getId(),
                e.getOffre().getReference() != null ? e.getOffre().getReference() : "Brouillon",
                e.getOffre().getFournisseur().getRaisonSociale(),
                e.getMembre().getId(),
                e.getMembre().getNomComplet(),
                e.getNoteTechnique(),
                e.getNoteFinanciere(),
                e.getNoteGlobale(),
                e.getObservations(),
                e.getDateEvaluation(),
                e.getNotes().stream().map(NoteEvaluationDto::from).toList()
        );
    }
}
