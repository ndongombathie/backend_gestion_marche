package com.marchepro.appeloffre.controller;

import com.marchepro.appeloffre.dto.DocumentObligatoireDto;
import com.marchepro.appeloffre.dto.DocumentObligatoireRequest;
import com.marchepro.appeloffre.service.DocumentObligatoireService;
import com.marchepro.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appels-offres/{appelOffreId}/documents-obligatoires")
@Tag(name = "Documents obligatoires",
        description = "Définition et suivi des pièces exigées pour un appel d'offres")
public class DocumentObligatoireController {

    private final DocumentObligatoireService documentObligatoireService;

    public DocumentObligatoireController(DocumentObligatoireService documentObligatoireService) {
        this.documentObligatoireService = documentObligatoireService;
    }

    @GetMapping
    @Operation(summary = "Lister les documents obligatoires d'un appel d'offres")
    public ApiResponse<List<DocumentObligatoireDto>> lister(@PathVariable Long appelOffreId) {
        return ApiResponse.success(documentObligatoireService.lister(appelOffreId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Ajouter un document obligatoire")
    public ResponseEntity<ApiResponse<DocumentObligatoireDto>> ajouter(
            @PathVariable Long appelOffreId,
            @Valid @RequestBody DocumentObligatoireRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Document obligatoire ajouté",
                documentObligatoireService.ajouter(appelOffreId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Modifier un document obligatoire")
    public ApiResponse<DocumentObligatoireDto> modifier(
            @PathVariable Long appelOffreId,
            @PathVariable Long id,
            @Valid @RequestBody DocumentObligatoireRequest request) {
        return ApiResponse.success("Document obligatoire mis à jour",
                documentObligatoireService.modifier(appelOffreId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Supprimer un document obligatoire")
    public ApiResponse<Void> supprimer(@PathVariable Long appelOffreId, @PathVariable Long id) {
        documentObligatoireService.supprimer(appelOffreId, id);
        return ApiResponse.success("Document obligatoire supprimé");
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Configurer la liste complète des documents obligatoires")
    public ApiResponse<List<DocumentObligatoireDto>> remplacer(
            @PathVariable Long appelOffreId,
            @Valid @RequestBody List<DocumentObligatoireRequest> requests) {
        return ApiResponse.success("Documents obligatoires configurés",
                documentObligatoireService.remplacer(appelOffreId, requests));
    }
}
