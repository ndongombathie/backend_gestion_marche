package com.marchepro.offre.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_offre")
@Getter
@Setter
@NoArgsConstructor
public class LigneOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String designation;

    @Column(nullable = false)
    private int quantite;

    @Column(name = "prix_unitaire", precision = 18, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(length = 1000)
    private String caracteristiques;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private Offre offre;
}
