package com.marchepro.livraison.controller;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.livraison.dto.DeclarationLivraisonRequest;
import com.marchepro.livraison.dto.LivraisonDto;
import com.marchepro.livraison.dto.LivraisonRequest;
import com.marchepro.livraison.dto.ReceptionRequest;
import com.marchepro.livraison.service.LivraisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livraisons")
@Tag(name = "Livraisons", description = "Planification, déclaration et réception des livraisons")
public class LivraisonController {

    private final LivraisonService livraisonService;

    public LivraisonController(LivraisonService livraisonService) {
        this.livraisonService = livraisonService;
    }

    @GetMapping("/mes-livraisons")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Lister mes livraisons")
    public ApiResponse<List<LivraisonDto>> mesLivraisons() {
        return ApiResponse.success(livraisonService.mesLivraisons());
    }

    @GetMapping("/en-retard")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR')")
    @Operation(summary = "Lister les livraisons en retard")
    public ApiResponse<List<LivraisonDto>> enRetard() {
        return ApiResponse.success(livraisonService.enRetard());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Lister les livraisons")
    public ApiResponse<List<LivraisonDto>> lister(@RequestParam(required = false) Long marcheId) {
        return ApiResponse.success(livraisonService.lister(marcheId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION','FOURNISSEUR')")
    @Operation(summary = "Détail d'une livraison")
    public ApiResponse<LivraisonDto> parId(@PathVariable Long id) {
        return ApiResponse.success(livraisonService.parId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FOURNISSEUR','ADMINISTRATEUR')")
    @Operation(summary = "Planifier une livraison")
    public ResponseEntity<ApiResponse<LivraisonDto>> creer(@Valid @RequestBody LivraisonRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Livraison planifiée", livraisonService.creer(request)));
    }

    @PutMapping("/{id}/declarer")
    @PreAuthorize("hasAnyRole('FOURNISSEUR','ADMINISTRATEUR')")
    @Operation(summary = "Déclarer une livraison effective")
    public ApiResponse<LivraisonDto> declarer(@PathVariable Long id,
                                              @Valid @RequestBody DeclarationLivraisonRequest request) {
        return ApiResponse.success("Livraison déclarée", livraisonService.declarer(id, request));
    }

    @PostMapping("/{id}/reception")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Enregistrer la réception d'une livraison")
    public ApiResponse<LivraisonDto> receptionner(@PathVariable Long id,
                                                  @Valid @RequestBody ReceptionRequest request) {
        return ApiResponse.success("Réception enregistrée", livraisonService.receptionner(id, request));
    }
}
