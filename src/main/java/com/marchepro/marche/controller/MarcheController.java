package com.marchepro.marche.controller;

import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import com.marchepro.marche.dto.MarcheDto;
import com.marchepro.marche.dto.MarcheRequest;
import com.marchepro.marche.entity.StatutMarche;
import com.marchepro.marche.service.MarcheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/marches")
@Tag(name = "Marchés", description = "Gestion et workflow des marchés publics")
public class MarcheController {

    private final MarcheService marcheService;

    public MarcheController(MarcheService marcheService) {
        this.marcheService = marcheService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Lister les marchés")
    public ApiResponse<PageResponse<MarcheDto>> lister(
            @RequestParam(required = false) StatutMarche statut,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fin,
            @PageableDefault(size = 10, sort = "dateCreation") Pageable pageable) {
        return ApiResponse.success(marcheService.lister(statut, q, debut, fin, pageable));
    }

    @GetMapping("/mes-marches")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Lister les marchés remportés par le fournisseur connecté")
    public ApiResponse<java.util.List<MarcheDto>> mesMarches() {
        return ApiResponse.success(marcheService.mesMarches());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Détail d'un marché")
    public ApiResponse<MarcheDto> parId(@PathVariable Long id) {
        return ApiResponse.success(marcheService.parId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Créer un marché")
    public ResponseEntity<ApiResponse<MarcheDto>> creer(@Valid @RequestBody MarcheRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Marché créé", marcheService.creer(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Modifier un marché en préparation")
    public ApiResponse<MarcheDto> modifier(@PathVariable Long id, @Valid @RequestBody MarcheRequest request) {
        return ApiResponse.success("Marché mis à jour", marcheService.modifier(id, request));
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Soumettre un marché pour validation")
    public ApiResponse<MarcheDto> soumettre(@PathVariable Long id) {
        return ApiResponse.success("Marché soumis pour validation", marcheService.soumettre(id));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Valider un marché")
    public ApiResponse<MarcheDto> valider(@PathVariable Long id,
                                          @Valid @RequestBody(required = false) DecisionRequest request) {
        return ApiResponse.success("Marché validé",
                marcheService.valider(id, request == null ? new DecisionRequest(null) : request));
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Rejeter un marché")
    public ApiResponse<MarcheDto> rejeter(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("Marché rejeté", marcheService.rejeter(id, request));
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Annuler un marché")
    public ApiResponse<MarcheDto> annuler(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("Marché annulé", marcheService.annuler(id, request));
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Clôturer un marché")
    public ApiResponse<MarcheDto> cloturer(@PathVariable Long id) {
        return ApiResponse.success("Marché clôturé", marcheService.cloturer(id));
    }
}
