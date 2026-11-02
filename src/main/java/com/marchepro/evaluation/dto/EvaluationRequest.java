package com.marchepro.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EvaluationRequest(
        @NotNull(message = "L'offre est obligatoire")
        Long offreId,

        @Size(max = 2000)
        String observations,

        @NotEmpty(message = "Au moins une note est requise")
        @Valid
        List<NoteEvaluationRequest> notes
) {
}
