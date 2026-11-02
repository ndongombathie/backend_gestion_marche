package com.marchepro.attribution.controller;

import com.marchepro.attribution.dto.AttributionDto;
import com.marchepro.attribution.dto.AttributionRequest;
import com.marchepro.attribution.entity.StatutAttribution;
import com.marchepro.attribution.service.AttributionService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/attributions")
@Tag(name = "Attributions", description = "Proposition et validation des attributions de marchés")
public class AttributionController {

    private final AttributionService attributionService;

    public AttributionController(AttributionService attributionService) {
        this.attributionService = attributionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DIRECTEUR','MEMBRE_COMMISSION','RESPONSABLE_SERVICE')")
    @Operation(summary = "Lister les attributions")
    public ApiResponse<List<AttributionDto>> lister(@RequestParam(required = false) StatutAttribution statut) {
        return ApiResponse.success(attributionService.lister(statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DIRECTEUR','MEMBRE_COMMISSION','RESPONSABLE_SERVICE')")
    @Operation(summary = "Détail d'une attribution")
    public ApiResponse<AttributionDto> parId(@PathVariable Long id) {
        return ApiResponse.success(attributionService.parId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBRE_COMMISSION','ADMINISTRATEUR')")
    @Operation(summary = "Proposer l'attribution d'un marché")
    public ResponseEntity<ApiResponse<AttributionDto>> proposer(
            @Valid @RequestBody AttributionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Proposition d'attribution enregistrée", attributionService.proposer(request)));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('DIRECTEUR')")
    @Operation(summary = "Valider une attribution")
    public ApiResponse<AttributionDto> valider(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) DecisionRequest request) {
        return ApiResponse.success("Attribution validée", attributionService.valider(id, request));
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('DIRECTEUR')")
    @Operation(summary = "Rejeter une attribution")
    public ApiResponse<AttributionDto> rejeter(@PathVariable Long id,
                                               @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("Attribution rejetée", attributionService.rejeter(id, request));
    }

    @GetMapping("/{id}/pv")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','DIRECTEUR','MEMBRE_COMMISSION','RESPONSABLE_SERVICE')")
    @Operation(summary = "Télécharger le procès-verbal d'attribution")
    public ResponseEntity<byte[]> pv(@PathVariable Long id) {
        AttributionDto attribution = attributionService.parId(id);
        byte[] contenu = attributionService.genererPv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("PV-" + attribution.marcheReference() + ".pdf", StandardCharsets.UTF_8)
                        .build().toString())
                .body(contenu);
    }
}
