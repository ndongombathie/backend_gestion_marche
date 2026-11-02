package com.marchepro.materiel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterielRequest(
        @NotBlank(message = "La désignation est obligatoire")
        @Size(max = 160)
        String designation,

        @NotBlank(message = "La catégorie est obligatoire")
        @Size(max = 80)
        String categorie,

        @Size(max = 1000)
        String description,

        @Size(max = 1000)
        String caracteristiques
) {
}
