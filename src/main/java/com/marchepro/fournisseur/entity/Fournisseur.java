package com.marchepro.fournisseur.entity;

import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "fournisseurs", indexes = {
        @Index(name = "idx_fournisseurs_ninea", columnList = "ninea", unique = true),
        @Index(name = "idx_fournisseurs_statut", columnList = "statut_validation")
})
@Getter
@Setter
@NoArgsConstructor
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raison_sociale", nullable = false, length = 160)
    private String raisonSociale;

    @Column(nullable = false, unique = true, length = 40)
    private String ninea;

    @Column(length = 40)
    private String rccm;

    @Column(length = 255)
    private String adresse;

    @Column(length = 30)
    private String telephone;

    @Column(length = 160)
    private String email;

    @Column(length = 120)
    private String representant;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false, length = 20)
    private StatutValidation statutValidation = StatutValidation.EN_ATTENTE;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
        if (statutValidation == null) {
            statutValidation = StatutValidation.EN_ATTENTE;
        }
    }
}
