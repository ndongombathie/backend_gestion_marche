package com.marchepro.appeloffre.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicStatsDto(
        long appelsOffresPublies,
        long marchesTotal,
        long fournisseursActifs,
        BigDecimal montantAttribue,
        List<String> categories,
        List<AppelOffreDto> recents,
        List<AppelOffreDto> bientotExpires
) {
}
