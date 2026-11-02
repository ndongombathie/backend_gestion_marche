package com.marchepro.livraison.entity;

import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "receptions")
@Getter
@Setter
@NoArgsConstructor
public class Reception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_reception", nullable = false)
    private LocalDate dateReception;

    @Column(length = 2000)
    private String observations;

    @Column(length = 2000)
    private String reserves;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutReception statut;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livraison_id", nullable = false, unique = true)
    private Livraison livraison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receptionnaire_id")
    private Utilisateur receptionnaire;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }
}
