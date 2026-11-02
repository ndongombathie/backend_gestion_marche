package com.marchepro.besoin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpressionBesoinRequest(
        @NotBlank(message = "L'objet est obligatoire")
        @Size(max = 255)
        String objet,

        @Size(max = 3000)
        String description,

        @NotBlank(message = "Le service demandeur est obligatoire")
        @Size(max = 160)
        String serviceDemandeur,

        @DecimalMin(value = "0.0", inclusive = false, message = "Le budget doit être positif")
        BigDecimal budgetEstimatif,

        LocalDate dateSouhaitee,

        @Size(max = 3000)
        String justification,

        @NotEmpty(message = "Au moins un matériel est requis")
        @Valid
        List<LigneBesoinRequest> lignes
) {
}
