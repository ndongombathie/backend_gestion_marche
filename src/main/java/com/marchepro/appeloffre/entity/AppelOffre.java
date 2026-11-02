package com.marchepro.appeloffre.entity;

import com.marchepro.marche.entity.Marche;
import com.marchepro.offre.entity.Offre;
import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appels_offres", indexes = {
        @Index(name = "idx_ao_reference", columnList = "reference", unique = true),
        @Index(name = "idx_ao_statut", columnList = "statut"),
        @Index(name = "idx_ao_date_limite", columnList = "date_limite")
})
@Getter
@Setter
@NoArgsConstructor
public class AppelOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(nullable = false, length = 255)
    private String objet;

    @Column(length = 4000)
    private String description;

    @Column(length = 80)
    private String categorie;

    @Column(length = 160)
    private String organisme;

    @Column(name = "conditions_participation", length = 4000)
    private String conditionsParticipation;

    @Column(name = "documents_obligatoires", length = 2000)
    private String documentsObligatoires;

    @Column(name = "modalites_soumission", length = 3000)
    private String modalitesSoumission;

    @Column(name = "budget_public", nullable = false)
    private boolean budgetPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAppelOffre statut = StatutAppelOffre.BROUILLON;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_publication")
    private Instant datePublication;

    @Column(name = "date_limite", nullable = false)
    private LocalDateTime dateLimite;

    @Column(name = "motif_annulation", length = 1000)
    private String motifAnnulation;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marche_id", nullable = false, unique = true)
    private Marche marche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publie_par_id")
    private Utilisateur publiePar;

    @OneToMany(mappedBy = "appelOffre", fetch = FetchType.LAZY)
    private List<Offre> offres = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = Instant.now();
        }
    }

    public boolean estOuvert() {
        return statut == StatutAppelOffre.PUBLIE && dateLimite.isAfter(LocalDateTime.now());
    }
}
