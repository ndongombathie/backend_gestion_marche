package com.marchepro.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterFournisseurRequest(
        @NotBlank(message = "La raison sociale est obligatoire")
        @Size(max = 160)
        String raisonSociale,

        @NotBlank(message = "Le NINEA est obligatoire")
        @Size(max = 40)
        String ninea,

        @Size(max = 40)
        String rccm,

        @Size(max = 255)
        String adresse,

        @NotBlank(message = "Le téléphone est obligatoire")
        @Size(max = 30)
        String telephone,

        @NotBlank(message = "L'email professionnel est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le nom du représentant est obligatoire")
        @Size(max = 120)
        String representant,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String motDePasse,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        String confirmationMotDePasse
) {
}
