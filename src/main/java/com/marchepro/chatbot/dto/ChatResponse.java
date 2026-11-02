package com.marchepro.chatbot.dto;

import java.util.List;

public record ChatResponse(
        String intention,
        String reponse,
        List<Resultat> resultats
) {
    public record Resultat(String type, Long id, String titre, String detail, String lien) {
    }
}
