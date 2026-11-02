package com.marchepro.marche.dto;

import com.marchepro.marche.entity.TypeCritere;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CritereRequest(
        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 160)
        String libelle,

        @NotNull(message = "Le type est obligatoire")
        TypeCritere type,

        @DecimalMin(value = "0.0", inclusive = false, message = "La pondération doit être positive")
        @DecimalMax(value = "100.0", message = "La pondération ne peut pas dépasser 100")
        double ponderation,

        @Size(max = 1000)
        String description
) {
}
