package com.marchepro.marche.dto;

import com.marchepro.marche.entity.LigneMarche;

import java.math.BigDecimal;

public record LigneMarcheDto(Long id, String designation, int quantite, String caracteristiques,
                             BigDecimal prixUnitaireEstime) {
    public static LigneMarcheDto from(LigneMarche l) {
        return new LigneMarcheDto(l.getId(), l.getDesignation(), l.getQuantite(),
                l.getCaracteristiques(), l.getPrixUnitaireEstime());
    }
}
