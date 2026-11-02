package com.marchepro.besoin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LigneBesoinRequest(
        @NotBlank(message = "La désignation est obligatoire")
        @Size(max = 160)
        String designation,

        @Min(value = 1, message = "La quantité doit être au moins 1")
        int quantite,

        @Size(max = 1000)
        String caracteristiques
) {
}
