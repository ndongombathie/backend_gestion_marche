package com.marchepro.offre.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record OffreRequest(
        @NotNull(message = "L'appel d'offres est obligatoire")
        Long appelOffreId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
        BigDecimal montant,

        @Min(value = 1, message = "Le délai de livraison doit être au moins 1 jour")
        Integer delaiLivraisonJours,

        @Size(max = 4000)
        String informationsTechniques,

        @Size(max = 2000)
        String observationsFournisseur,

        @Valid
        List<LigneOffreRequest> lignes
) {
}
