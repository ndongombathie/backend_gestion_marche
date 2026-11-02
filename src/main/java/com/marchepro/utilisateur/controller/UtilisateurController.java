package com.marchepro.utilisateur.controller;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import com.marchepro.utilisateur.dto.CreateUtilisateurRequest;
import com.marchepro.utilisateur.dto.StatutRequest;
import com.marchepro.utilisateur.dto.UtilisateurDto;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@Tag(name = "Utilisateurs", description = "Administration des comptes internes")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @Operation(summary = "Lister les utilisateurs")
    public ApiResponse<PageResponse<UtilisateurDto>> lister(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) StatutUtilisateur statut,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "nom") Pageable pageable) {
        return ApiResponse.success(utilisateurService.lister(role, statut, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    public ApiResponse<UtilisateurDto> parId(@PathVariable Long id) {
        return ApiResponse.success(utilisateurService.parId(id));
    }

    @PostMapping
    @Operation(summary = "Créer un compte interne")
    public ResponseEntity<ApiResponse<UtilisateurDto>> creer(@Valid @RequestBody CreateUtilisateurRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Utilisateur créé", utilisateurService.creer(request)));
    }

    @PutMapping("/{id}/statut")
    @Operation(summary = "Activer ou désactiver un utilisateur")
    public ApiResponse<UtilisateurDto> changerStatut(@PathVariable Long id,
                                                     @Valid @RequestBody StatutRequest request) {
        return ApiResponse.success("Statut mis à jour", utilisateurService.changerStatut(id, request.statut()));
    }
}
