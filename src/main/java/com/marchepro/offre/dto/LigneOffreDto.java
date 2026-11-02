package com.marchepro.offre.dto;

import com.marchepro.common.document.DocumentDto;
import com.marchepro.offre.entity.LigneOffre;

import java.math.BigDecimal;

public record LigneOffreDto(Long id, String designation, int quantite, BigDecimal prixUnitaire,
                            String caracteristiques, Long imageId, String imageNom) {
    public static LigneOffreDto from(LigneOffre l, DocumentDto image) {
        return new LigneOffreDto(l.getId(), l.getDesignation(), l.getQuantite(),
                l.getPrixUnitaire(), l.getCaracteristiques(),
                image != null ? image.id() : null,
                image != null ? image.nom() : null);
    }
}
