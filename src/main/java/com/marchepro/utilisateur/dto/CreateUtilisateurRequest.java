package com.marchepro.utilisateur.dto;

import com.marchepro.utilisateur.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUtilisateurRequest(
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 80) String nom,
        @NotBlank(message = "Le prénom est obligatoire") @Size(max = 80) String prenom,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @Size(max = 30) String telephone,
        @NotNull(message = "Le rôle est obligatoire") Role role,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Au moins 8 caractères") String motDePasse
) {
}
