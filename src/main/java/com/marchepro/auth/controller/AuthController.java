package com.marchepro.auth.controller;

import com.marchepro.auth.dto.*;
import com.marchepro.auth.service.AuthService;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.utilisateur.dto.UtilisateurDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Connexion, inscription fournisseur et gestion du compte")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifier un utilisateur")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Connexion réussie", authService.login(request));
    }

    @PostMapping("/register/fournisseur")
    @Operation(summary = "Inscrire un nouveau fournisseur")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterFournisseurRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(authService.registerFournisseur(request)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demander la réinitialisation du mot de passe")
    public ApiResponse<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe avec un token")
    public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.success(authService.resetPassword(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil de l'utilisateur connecté")
    public ApiResponse<UtilisateurDto> me() {
        return ApiResponse.success(authService.me());
    }

    @PutMapping("/profile")
    @Operation(summary = "Mettre à jour le profil de l'utilisateur connecté")
    public ApiResponse<UtilisateurDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profil mis à jour", authService.updateProfile(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (le token JWT est invalidé côté client)")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("Déconnexion réussie");
    }
}
