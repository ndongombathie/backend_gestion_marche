package com.marchepro.appeloffre.dto;

import com.marchepro.common.document.DocumentDto;
import com.marchepro.common.document.DocumentType;

import java.util.List;

public record DocumentObligatoireSuiviDto(
        Long id,
        String libelle,
        String description,
        DocumentType type,
        boolean obligatoire,
        int ordre,
        List<DocumentDto> documents,
        boolean fourni
) {
    public static DocumentObligatoireSuiviDto from(DocumentObligatoireDto document,
                                                   List<DocumentDto> documents,
                                                   boolean fourni) {
        return new DocumentObligatoireSuiviDto(
                document.id(),
                document.libelle(),
                document.description(),
                document.type(),
                document.obligatoire(),
                document.ordre(),
                documents,
                fourni
        );
    }
}
