package com.marchepro.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 80)
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 80)
        String prenom,

        @Email(message = "Format d'email invalide")
        @NotBlank(message = "L'email est obligatoire")
        String email,

        @Size(max = 30)
        String telephone,

        String motDePasseActuel,

        @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères")
        String nouveauMotDePasse
) {
}
