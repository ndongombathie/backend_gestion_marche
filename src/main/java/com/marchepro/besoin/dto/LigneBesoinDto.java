package com.marchepro.besoin.dto;

import com.marchepro.besoin.entity.LigneBesoin;

public record LigneBesoinDto(Long id, String designation, int quantite, String caracteristiques) {
    public static LigneBesoinDto from(LigneBesoin l) {
        return new LigneBesoinDto(l.getId(), l.getDesignation(), l.getQuantite(), l.getCaracteristiques());
    }
}
