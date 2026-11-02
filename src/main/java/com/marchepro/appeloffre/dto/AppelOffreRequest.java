package com.marchepro.appeloffre.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppelOffreRequest(
        Long marcheId,

        @NotBlank(message = "L'objet est obligatoire")
        @Size(max = 255)
        String objet,

        @Size(max = 4000)
        String description,

        @Size(max = 80)
        String categorie,

        @Size(max = 160)
        String organisme,

        @Size(max = 4000)
        String conditionsParticipation,

        @Size(max = 2000)
        String documentsObligatoires,

        @Size(max = 3000)
        String modalitesSoumission,

        boolean budgetPublic,

        @NotNull(message = "La date limite est obligatoire")
        @Future(message = "La date limite doit être dans le futur")
        LocalDateTime dateLimite
) {
}
