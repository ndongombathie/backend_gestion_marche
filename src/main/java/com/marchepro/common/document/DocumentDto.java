package com.marchepro.common.document;

import java.time.Instant;

public record DocumentDto(
        Long id,
        String nom,
        String contentType,
        long taille,
        DocumentType type,
        String entiteType,
        Long entiteId,
        Instant dateUpload
) {
    public static DocumentDto from(Document document) {
        return new DocumentDto(
                document.getId(),
                document.getNom(),
                document.getContentType(),
                document.getTaille(),
                document.getType(),
                document.getEntiteType(),
                document.getEntiteId(),
                document.getDateUpload()
        );
    }
}
