package com.marchepro.livraison.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record LivraisonRequest(
        @NotNull(message = "Le marché est obligatoire")
        Long marcheId,

        @NotNull(message = "La date prévue est obligatoire")
        @FutureOrPresent(message = "La date prévue ne peut pas être dans le passé")
        LocalDate datePrevue,

        @Size(max = 2000)
        String observations,

        @NotEmpty(message = "Au moins une ligne de livraison est requise")
        @Valid
        List<LigneLivraisonRequest> lignes
) {
}
