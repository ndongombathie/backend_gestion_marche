package com.marchepro.auth.dto;

import com.marchepro.utilisateur.dto.UtilisateurDto;

public record LoginResponse(String token, String type, long expiresInMs, UtilisateurDto utilisateur) {
}
