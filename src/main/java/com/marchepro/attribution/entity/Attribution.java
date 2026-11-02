package com.marchepro.attribution.entity;

import com.marchepro.fournisseur.entity.Fournisseur;
import com.marchepro.marche.entity.Marche;
import com.marchepro.offre.entity.Offre;
import com.marchepro.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "attributions", indexes = {
        @Index(name = "idx_attributions_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
public class Attribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAttribution statut = StatutAttribution.PROPOSEE;

    @Column(name = "date_proposition", nullable = false, updatable = false)
    private Instant dateProposition;

    @Column(name = "date_decision")
    private Instant dateDecision;

    @Column(name = "motif", length = 2000)
    private String motif;

    @Column(name = "observations", length = 2000)
    private String observations;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marche_id", nullable = false, unique = true)
    private Marche marche;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false, unique = true)
    private Offre offre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propose_par_id")
    private Utilisateur proposePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @PrePersist
    void onCreate() {
        if (dateProposition == null) {
            dateProposition = Instant.now();
        }
    }
}
