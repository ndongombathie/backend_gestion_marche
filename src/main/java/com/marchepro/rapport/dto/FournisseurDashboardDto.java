package com.marchepro.rapport.dto;

import com.marchepro.livraison.dto.LivraisonDto;
import com.marchepro.offre.dto.OffreDto;

import java.util.List;

public record FournisseurDashboardDto(
        long appelsOffresDisponibles,
        long offresSoumises,
        long offresEnEvaluation,
        long marchesRemportes,
        long marchesEnExecution,
        long livraisonsPlanifiees,
        String statutValidation,
        List<OffreDto> dernieresOffres,
        List<LivraisonDto> prochainesLivraisons
) {
}
