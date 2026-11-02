package com.marchepro.utilisateur.dto;

import com.marchepro.utilisateur.entity.StatutUtilisateur;
import jakarta.validation.constraints.NotNull;

public record StatutRequest(@NotNull(message = "Le statut est obligatoire") StatutUtilisateur statut) {
}
