package com.marchepro.marche.dto;

import com.marchepro.marche.entity.ModePassation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MarcheRequest(
        @NotBlank(message = "L'objet est obligatoire")
        @Size(max = 255)
        String objet,

        @Size(max = 3000)
        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Le budget doit être positif")
        BigDecimal budgetPrevisionnel,

        @NotNull(message = "Le mode de passation est obligatoire")
        ModePassation modePassation,

        Long expressionBesoinId,

        LocalDate dateDebutExecution,

        LocalDate dateFinExecution,

        @Size(max = 2000)
        String observations,

        @NotEmpty(message = "Au moins une ligne de matériel est requise")
        @Valid
        List<LigneMarcheRequest> lignes,

        @Valid
        List<CritereRequest> criteres
) {
}
