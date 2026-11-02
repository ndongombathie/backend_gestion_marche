package com.marchepro.livraison.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LigneLivraisonRequest(
        @NotNull(message = "La ligne du marché est obligatoire")
        Long ligneMarcheId,

        @Min(value = 1, message = "La quantité livrée doit être au moins 1")
        int quantiteLivree
) {
}
