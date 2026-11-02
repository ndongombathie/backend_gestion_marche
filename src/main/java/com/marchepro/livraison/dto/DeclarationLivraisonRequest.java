package com.marchepro.livraison.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeclarationLivraisonRequest(
        @NotNull(message = "La date effective est obligatoire")
        LocalDate dateEffective,

        @Size(max = 2000)
        String observations
) {
}
