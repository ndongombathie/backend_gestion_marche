package com.marchepro.besoin.entity;

import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expressions_besoin", indexes = {
        @Index(name = "idx_besoins_reference", columnList = "reference", unique = true),
        @Index(name = "idx_besoins_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
public class ExpressionBesoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(nullable = false, length = 255)
    private String objet;

    @Column(length = 3000)
    private String description;

    @Column(name = "service_demandeur", nullable = false, length = 160)
    private String serviceDemandeur;

    @Column(name = "budget_estimatif", precision = 18, scale = 2)
    private BigDecimal budgetEstimatif;

    @Column(name = "date_souhaitee")
    private LocalDate dateSouhaitee;

    @Column(length = 3000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutBesoin statut = StatutBesoin.BROUILLON;

    @Column(name = "motif_rejet", length = 1000)
    private String motifRejet;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_soumission")
    private Instant dateSoumission;

    @Column(name = "date_validation")
    private Instant dateValidation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @OneToMany(mappedBy = "expressionBesoin", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LigneBesoin> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }

    public void ajouterLigne(LigneBesoin ligne) {
        ligne.setExpressionBesoin(this);
        lignes.add(ligne);
    }
}
