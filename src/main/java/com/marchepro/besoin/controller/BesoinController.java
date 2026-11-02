package com.marchepro.besoin.controller;

import com.marchepro.besoin.dto.ExpressionBesoinDto;
import com.marchepro.besoin.dto.ExpressionBesoinRequest;
import com.marchepro.besoin.entity.StatutBesoin;
import com.marchepro.besoin.service.BesoinService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/besoins")
@Tag(name = "Expressions de besoin", description = "Expression et validation des besoins en matériels")
public class BesoinController {

    private final BesoinService besoinService;

    public BesoinController(BesoinService besoinService) {
        this.besoinService = besoinService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Lister les expressions de besoin")
    public ApiResponse<PageResponse<ExpressionBesoinDto>> lister(
            @RequestParam(required = false) StatutBesoin statut,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "dateCreation") Pageable pageable) {
        return ApiResponse.success(besoinService.lister(statut, q, pageable));
    }

    @GetMapping("/validees")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Lister les expressions de besoin validées")
    public ApiResponse<List<ExpressionBesoinDto>> validees() {
        return ApiResponse.success(besoinService.validees());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Détail d'une expression de besoin")
    public ApiResponse<ExpressionBesoinDto> parId(@PathVariable Long id) {
        return ApiResponse.success(besoinService.parId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Créer une expression de besoin")
    public ResponseEntity<ApiResponse<ExpressionBesoinDto>> creer(
            @Valid @RequestBody ExpressionBesoinRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Expression de besoin créée", besoinService.creer(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Modifier une expression de besoin en brouillon")
    public ApiResponse<ExpressionBesoinDto> modifier(@PathVariable Long id,
                                                    @Valid @RequestBody ExpressionBesoinRequest request) {
        return ApiResponse.success("Expression de besoin mise à jour", besoinService.modifier(id, request));
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Soumettre une expression de besoin pour validation")
    public ApiResponse<ExpressionBesoinDto> soumettre(@PathVariable Long id) {
        return ApiResponse.success("Expression de besoin soumise", besoinService.soumettre(id));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Valider une expression de besoin")
    public ApiResponse<ExpressionBesoinDto> valider(@PathVariable Long id,
                                                    @Valid @RequestBody(required = false) DecisionRequest request) {
        return ApiResponse.success("Expression de besoin validée",
                besoinService.valider(id, request == null ? new DecisionRequest(null) : request));
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('DIRECTEUR','ADMINISTRATEUR')")
    @Operation(summary = "Rejeter une expression de besoin")
    public ApiResponse<ExpressionBesoinDto> rejeter(@PathVariable Long id,
                                                    @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("Expression de besoin rejetée", besoinService.rejeter(id, request));
    }
}
