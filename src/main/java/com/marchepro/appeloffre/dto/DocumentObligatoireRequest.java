package com.marchepro.appeloffre.dto;

import com.marchepro.common.document.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentObligatoireRequest(
        @NotBlank(message = "Le libellé du document est obligatoire")
        @Size(max = 160)
        String libelle,

        @Size(max = 500)
        String description,

        DocumentType type,

        Boolean obligatoire,

        Integer ordre
) {
}
