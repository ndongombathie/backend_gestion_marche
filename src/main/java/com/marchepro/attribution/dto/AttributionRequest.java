package com.marchepro.attribution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttributionRequest(
        @NotNull(message = "L'offre est obligatoire")
        Long offreId,

        @Size(max = 2000)
        String motif,

        @Size(max = 2000)
        String observations
) {
}
