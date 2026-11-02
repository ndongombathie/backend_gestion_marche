package com.marchepro.marche.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_marche")
@Getter
@Setter
@NoArgsConstructor
public class LigneMarche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String designation;

    @Column(nullable = false)
    private int quantite;

    @Column(length = 1000)
    private String caracteristiques;

    @Column(name = "prix_unitaire_estime", precision = 18, scale = 2)
    private BigDecimal prixUnitaireEstime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marche_id", nullable = false)
    private Marche marche;
}
