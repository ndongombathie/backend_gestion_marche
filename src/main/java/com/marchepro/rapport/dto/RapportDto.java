package com.marchepro.rapport.dto;

import java.time.Instant;

public record RapportDto(Long id, String titre, String format, String parametres,
                         Instant dateGeneration, String generePar) {
}
