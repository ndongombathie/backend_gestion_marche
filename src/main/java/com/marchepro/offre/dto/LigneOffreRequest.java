package com.marchepro.offre.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LigneOffreRequest(
        Long id,

        @NotBlank(message = "La désignation est obligatoire")
        @Size(max = 160)
        String designation,

        @Min(value = 1, message = "La quantité doit être au moins 1")
        int quantite,

        @DecimalMin(value = "0.0", inclusive = false, message = "Le prix unitaire doit être positif")
        BigDecimal prixUnitaire,

        @Size(max = 1000)
        String caracteristiques
) {
}
