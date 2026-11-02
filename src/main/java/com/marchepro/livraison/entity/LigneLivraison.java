package com.marchepro.livraison.entity;

import com.marchepro.marche.entity.LigneMarche;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lignes_livraison")
@Getter
@Setter
@NoArgsConstructor
public class LigneLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantite_livree", nullable = false)
    private int quantiteLivree;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_marche_id", nullable = false)
    private LigneMarche ligneMarche;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livraison_id", nullable = false)
    private Livraison livraison;
}
