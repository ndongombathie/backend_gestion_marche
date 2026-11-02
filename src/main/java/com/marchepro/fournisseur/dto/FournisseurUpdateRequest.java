package com.marchepro.fournisseur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FournisseurUpdateRequest(
        @NotBlank(message = "La raison sociale est obligatoire")
        @Size(max = 160)
        String raisonSociale,

        @Size(max = 40)
        String rccm,

        @Size(max = 255)
        String adresse,

        @NotBlank(message = "Le téléphone est obligatoire")
        @Size(max = 30)
        String telephone,

        @NotBlank(message = "Le représentant est obligatoire")
        @Size(max = 120)
        String representant
) {
}
