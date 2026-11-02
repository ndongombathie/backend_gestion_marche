package com.marchepro.appeloffre.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProlongationRequest(
        @NotNull(message = "La nouvelle date limite est obligatoire")
        @Future(message = "La nouvelle date limite doit être dans le futur")
        LocalDateTime nouvelleDateLimite,

        @NotBlank(message = "La justification est obligatoire")
        @Size(max = 1000)
        String justification
) {
}
