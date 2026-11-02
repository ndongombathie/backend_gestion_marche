package com.marchepro.evaluation.dto;

import com.marchepro.evaluation.entity.NoteEvaluation;
import com.marchepro.marche.entity.TypeCritere;

import java.math.BigDecimal;

public record NoteEvaluationDto(Long critereId, String libelle, TypeCritere type, double ponderation,
                                BigDecimal note, String commentaire) {
    public static NoteEvaluationDto from(NoteEvaluation n) {
        return new NoteEvaluationDto(n.getCritere().getId(), n.getCritere().getLibelle(),
                n.getCritere().getType(), n.getCritere().getPonderation(), n.getNote(), n.getCommentaire());
    }
}
