package com.marchepro.fournisseur.controller;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import com.marchepro.fournisseur.dto.FournisseurDto;
import com.marchepro.fournisseur.dto.FournisseurUpdateRequest;
import com.marchepro.fournisseur.dto.ValidationFournisseurRequest;
import com.marchepro.fournisseur.entity.StatutValidation;
import com.marchepro.fournisseur.service.FournisseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fournisseurs")
@Tag(name = "Fournisseurs", description = "Gestion des fournisseurs et validation des inscriptions")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    public FournisseurController(FournisseurService fournisseurService) {
        this.fournisseurService = fournisseurService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Lister les fournisseurs")
    public ApiResponse<PageResponse<FournisseurDto>> lister(
            @RequestParam(required = false) StatutValidation statut,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "raisonSociale") Pageable pageable) {
        return ApiResponse.success(fournisseurService.lister(statut, q, pageable));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Lister les demandes d'inscription en attente")
    public ApiResponse<List<FournisseurDto>> enAttente() {
        return ApiResponse.success(fournisseurService.enAttente());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Profil du fournisseur connecté")
    public ApiResponse<FournisseurDto> monProfil() {
        return ApiResponse.success(fournisseurService.monProfil());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Mettre à jour le profil du fournisseur connecté")
    public ApiResponse<FournisseurDto> mettreAJourProfil(@Valid @RequestBody FournisseurUpdateRequest request) {
        return ApiResponse.success("Profil fournisseur mis à jour", fournisseurService.mettreAJourProfil(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Détail d'un fournisseur")
    public ApiResponse<FournisseurDto> parId(@PathVariable Long id) {
        return ApiResponse.success(fournisseurService.parId(id));
    }

    @PutMapping("/{id}/validation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Valider ou rejeter un fournisseur")
    public ApiResponse<FournisseurDto> valider(@PathVariable Long id,
                                               @Valid @RequestBody ValidationFournisseurRequest request) {
        return ApiResponse.success("Statut du fournisseur mis à jour", fournisseurService.valider(id, request));
    }
}
