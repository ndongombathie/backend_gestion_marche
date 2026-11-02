package com.marchepro.offre.entity;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.fournisseur.entity.Fournisseur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offres", uniqueConstraints = {
        @UniqueConstraint(name = "uk_offre_ao_fournisseur", columnNames = {"appel_offre_id", "fournisseur_id"})
}, indexes = {
        @Index(name = "idx_offres_reference", columnList = "reference", unique = true),
        @Index(name = "idx_offres_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
public class Offre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 40)
    private String reference;

    @Column(name = "reference_accuse", length = 60)
    private String referenceAccuse;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(name = "delai_livraison_jours")
    private Integer delaiLivraisonJours;

    @Column(name = "informations_techniques", length = 4000)
    private String informationsTechniques;

    @Column(name = "observations_fournisseur", length = 2000)
    private String observationsFournisseur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutOffre statut = StatutOffre.BROUILLON;

    @Column(name = "date_soumission")
    private Instant dateSoumission;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "motif_rejet", length = 1000)
    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appel_offre_id", nullable = false)
    private AppelOffre appelOffre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @OneToMany(mappedBy = "offre", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LigneOffre> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }

    public void ajouterLigne(LigneOffre ligne) {
        ligne.setOffre(this);
        lignes.add(ligne);
    }
}
