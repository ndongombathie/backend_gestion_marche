package com.marchepro.livraison.dto;

import com.marchepro.livraison.entity.StatutReception;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReceptionRequest(
        @NotNull(message = "La date de réception est obligatoire")
        LocalDate dateReception,

        @Size(max = 2000)
        String observations,

        @Size(max = 2000)
        String reserves,

        @NotNull(message = "Le statut de réception est obligatoire")
        StatutReception statut
) {
}
