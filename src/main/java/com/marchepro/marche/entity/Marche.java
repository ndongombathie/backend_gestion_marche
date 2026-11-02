package com.marchepro.marche.entity;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.besoin.entity.ExpressionBesoin;
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
@Table(name = "marches", indexes = {
        @Index(name = "idx_marches_reference", columnList = "reference", unique = true),
        @Index(name = "idx_marches_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
public class Marche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(nullable = false, length = 255)
    private String objet;

    @Column(length = 3000)
    private String description;

    @Column(name = "budget_previsionnel", precision = 18, scale = 2)
    private BigDecimal budgetPrevisionnel;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_passation", nullable = false, length = 40)
    private ModePassation modePassation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutMarche statut = StatutMarche.EN_PREPARATION;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expression_besoin_id", unique = true)
    private ExpressionBesoin expressionBesoin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "createur_id", nullable = false)
    private Utilisateur createur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_validation")
    private Instant dateValidation;

    @Column(name = "date_publication")
    private Instant datePublication;

    @Column(name = "date_debut_execution")
    private LocalDate dateDebutExecution;

    @Column(name = "date_fin_execution")
    private LocalDate dateFinExecution;

    @Column(name = "date_cloture")
    private Instant dateCloture;

    @Column(name = "motif_annulation", length = 1000)
    private String motifAnnulation;

    @Column(length = 2000)
    private String observations;

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LigneMarche> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CritereEvaluation> criteres = new ArrayList<>();

    @OneToOne(mappedBy = "marche", fetch = FetchType.LAZY)
    private AppelOffre appelOffre;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }

    public void ajouterLigne(LigneMarche ligne) {
        ligne.setMarche(this);
        lignes.add(ligne);
    }

    public void ajouterCritere(CritereEvaluation critere) {
        critere.setMarche(this);
        criteres.add(critere);
    }
}
