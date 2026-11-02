package com.marchepro.fournisseur.dto;

import com.marchepro.fournisseur.entity.StatutValidation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidationFournisseurRequest(
        @NotNull(message = "Le statut est obligatoire")
        StatutValidation statut,

        @Size(max = 500)
        String motif
) {
}
