package com.marchepro.appeloffre.dto;

import com.marchepro.appeloffre.entity.DocumentObligatoire;
import com.marchepro.common.document.DocumentType;

public record DocumentObligatoireDto(
        Long id,
        Long appelOffreId,
        String libelle,
        String description,
        DocumentType type,
        boolean obligatoire,
        int ordre
) {
    public static DocumentObligatoireDto from(DocumentObligatoire document) {
        return new DocumentObligatoireDto(
                document.getId(),
                document.getAppelOffre() != null ? document.getAppelOffre().getId() : null,
                document.getLibelle(),
                document.getDescription(),
                document.getType(),
                document.isObligatoire(),
                document.getOrdre()
        );
    }
}
