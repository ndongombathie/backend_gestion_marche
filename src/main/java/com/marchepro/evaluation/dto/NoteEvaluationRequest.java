package com.marchepro.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NoteEvaluationRequest(
        @NotNull(message = "Le critère est obligatoire")
        Long critereId,

        @NotNull(message = "La note est obligatoire")
        @DecimalMin(value = "0.0", message = "La note minimale est 0")
        @DecimalMax(value = "20.0", message = "La note maximale est 20")
        BigDecimal note,

        @Size(max = 1000)
        String commentaire
) {
}
