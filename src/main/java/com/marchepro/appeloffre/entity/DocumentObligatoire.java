package com.marchepro.appeloffre.entity;

import com.marchepro.common.document.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "documents_obligatoires", indexes = {
        @Index(name = "idx_doc_obligatoire_appel_offre", columnList = "appel_offre_id")
})
@Getter
@Setter
@NoArgsConstructor
public class DocumentObligatoire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appel_offre_id", nullable = false)
    private AppelOffre appelOffre;

    @Column(nullable = false, length = 160)
    private String libelle;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private DocumentType type;

    @Column(nullable = false)
    private boolean obligatoire = true;

    @Column(nullable = false)
    private int ordre;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }
}
