package com.marchepro.marche.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LigneMarcheRequest(
        @NotBlank(message = "La désignation est obligatoire")
        @Size(max = 160)
        String designation,

        @Min(value = 1, message = "La quantité doit être au moins 1")
        int quantite,

        @Size(max = 1000)
        String caracteristiques,

        @DecimalMin(value = "0.0", message = "Le prix doit être positif")
        BigDecimal prixUnitaireEstime
) {
}
