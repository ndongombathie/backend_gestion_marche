package com.marchepro.besoin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lignes_besoin")
@Getter
@Setter
@NoArgsConstructor
public class LigneBesoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String designation;

    @Column(nullable = false)
    private int quantite;

    @Column(length = 1000)
    private String caracteristiques;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expression_besoin_id", nullable = false)
    private ExpressionBesoin expressionBesoin;
}
