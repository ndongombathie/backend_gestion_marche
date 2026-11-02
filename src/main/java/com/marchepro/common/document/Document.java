package com.marchepro.common.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_entite", columnList = "entite_type, entite_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(name = "nom_stockage", nullable = false, length = 255)
    private String nomStockage;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(nullable = false)
    private long taille;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentType type;

    @Column(name = "entite_type", nullable = false, length = 40)
    private String entiteType;

    @Column(name = "entite_id", nullable = false)
    private Long entiteId;

    @Column(name = "document_obligatoire_id")
    private Long documentObligatoireId;

    @Column(name = "date_upload", nullable = false, updatable = false)
    private Instant dateUpload;

    @PrePersist
    void onCreate() {
        if (dateUpload == null) {
            dateUpload = Instant.now();
        }
    }

    public boolean estApercuPossible() {
        if (nom == null) {
            return false;
        }
        int index = nom.lastIndexOf('.');
        if (index < 0 || index == nom.length() - 1) {
            return false;
        }
        String extension = nom.substring(index + 1).toLowerCase(Locale.ROOT);
        return "pdf".equals(extension)
                || "png".equals(extension)
                || "jpg".equals(extension)
                || "jpeg".equals(extension);
    }
}
