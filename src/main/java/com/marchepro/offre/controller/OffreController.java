package com.marchepro.offre.controller;

import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.offre.dto.OffreDto;
import com.marchepro.offre.dto.OffreRequest;
import com.marchepro.offre.service.OffreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@Tag(name = "Offres", description = "Soumission et suivi des offres fournisseurs")
public class OffreController {

    private final OffreService offreService;

    public OffreController(OffreService offreService) {
        this.offreService = offreService;
    }

    @GetMapping("/mes-offres")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Lister mes offres")
    public ApiResponse<List<OffreDto>> mesOffres() {
        return ApiResponse.success(offreService.mesOffres());
    }

    @GetMapping("/appel-offre/{appelOffreId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Lister les offres d'un appel d'offres")
    public ApiResponse<List<OffreDto>> parAppelOffre(@PathVariable Long appelOffreId) {
        return ApiResponse.success(offreService.offresParAppelOffre(appelOffreId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION','FOURNISSEUR')")
    @Operation(summary = "Détail d'une offre")
    public ApiResponse<OffreDto> parId(@PathVariable Long id) {
        return ApiResponse.success(offreService.parId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Créer un brouillon d'offre")
    public ResponseEntity<ApiResponse<OffreDto>> creer(@Valid @RequestBody OffreRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Brouillon d'offre créé", offreService.creerBrouillon(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Modifier un brouillon d'offre")
    public ApiResponse<OffreDto> modifier(@PathVariable Long id, @Valid @RequestBody OffreRequest request) {
        return ApiResponse.success("Brouillon mis à jour", offreService.modifierBrouillon(id, request));
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Soumettre définitivement une offre")
    public ApiResponse<OffreDto> soumettre(@PathVariable Long id) {
        return ApiResponse.success("Offre soumise avec succès", offreService.soumettre(id));
    }

    @PostMapping("/{id}/disqualifier")
    @PreAuthorize("hasAnyRole('MEMBRE_COMMISSION','ADMINISTRATEUR')")
    @Operation(summary = "Disqualifier une offre")
    public ApiResponse<OffreDto> disqualifier(@PathVariable Long id,
                                              @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("Offre disqualifiée", offreService.disqualifier(id, request));
    }
}
