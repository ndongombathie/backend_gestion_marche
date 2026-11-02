package com.marchepro.marche.dto;

import com.marchepro.marche.entity.CritereEvaluation;
import com.marchepro.marche.entity.TypeCritere;

public record CritereDto(Long id, String libelle, TypeCritere type, double ponderation, String description) {
    public static CritereDto from(CritereEvaluation c) {
        return new CritereDto(c.getId(), c.getLibelle(), c.getType(), c.getPonderation(), c.getDescription());
    }
}
