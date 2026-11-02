package com.marchepro.common.dto;

import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @Size(max = 1000, message = "Le motif ne peut pas dépasser 1000 caractères")
        String motif
) {
}
