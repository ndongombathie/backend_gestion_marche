package com.marchepro.livraison.entity;

import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.marche.entity.Marche;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "livraisons", indexes = {
        @Index(name = "idx_livraisons_statut", columnList = "statut"),
        @Index(name = "idx_livraisons_date_prevue", columnList = "date_prevue")
})
@Getter
@Setter
@NoArgsConstructor
public class Livraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_prevue", nullable = false)
    private LocalDate datePrevue;

    @Column(name = "date_effective")
    private LocalDate dateEffective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutLivraison statut = StatutLivraison.PLANIFIEE;

    @Column(length = 2000)
    private String observations;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marche_id", nullable = false)
    private Marche marche;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @OneToMany(mappedBy = "livraison", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LigneLivraison> lignes = new ArrayList<>();

    @OneToOne(mappedBy = "livraison", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Reception reception;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }

    public void ajouterLigne(LigneLivraison ligne) {
        ligne.setLivraison(this);
        lignes.add(ligne);
    }
}
