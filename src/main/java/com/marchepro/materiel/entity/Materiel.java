package com.marchepro.materiel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "materiels", indexes = {
        @Index(name = "idx_materiels_categorie", columnList = "categorie")
})
@Getter
@Setter
@NoArgsConstructor
public class Materiel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String designation;

    @Column(nullable = false, length = 80)
    private String categorie;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String caracteristiques;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }
}
