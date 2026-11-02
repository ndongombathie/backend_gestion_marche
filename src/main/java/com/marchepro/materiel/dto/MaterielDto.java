package com.marchepro.materiel.dto;

import com.marchepro.materiel.entity.Materiel;

import java.time.Instant;

public record MaterielDto(
        Long id,
        String designation,
        String categorie,
        String description,
        String caracteristiques,
        boolean actif,
        Instant dateCreation
) {
    public static MaterielDto from(Materiel m) {
        return new MaterielDto(m.getId(), m.getDesignation(), m.getCategorie(), m.getDescription(),
                m.getCaracteristiques(), m.isActif(), m.getDateCreation());
    }
}
