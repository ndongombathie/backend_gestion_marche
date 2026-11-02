package com.marchepro.rapport.entity;

import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "rapports")
@Getter
@Setter
@NoArgsConstructor
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, length = 20)
    private String format;

    @Column(length = 1000)
    private String parametres;

    @Column(name = "date_generation", nullable = false, updatable = false)
    private Instant dateGeneration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genere_par_id")
    private Utilisateur generePar;

    @PrePersist
    void onCreate() {
        if (dateGeneration == null) {
            dateGeneration = Instant.now();
        }
    }
}
