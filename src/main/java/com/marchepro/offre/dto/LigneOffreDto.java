package com.marchepro.offre.dto;

import com.marchepro.offre.entity.LigneOffre;

import java.math.BigDecimal;

public record LigneOffreDto(Long id, String designation, int quantite, BigDecimal prixUnitaire,
                            String caracteristiques) {
    public static LigneOffreDto from(LigneOffre l) {
        return new LigneOffreDto(l.getId(), l.getDesignation(), l.getQuantite(),
                l.getPrixUnitaire(), l.getCaracteristiques());
    }
}
