package com.marchepro.evaluation.controller;

import com.marchepro.appeloffre.dto.AppelOffreDto;
import com.marchepro.appeloffre.service.AppelOffreService;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.evaluation.dto.ConsolidationDto;
import com.marchepro.evaluation.dto.EvaluationDto;
import com.marchepro.evaluation.dto.EvaluationRequest;
import com.marchepro.evaluation.service.EvaluationService;
import com.marchepro.offre.dto.OffreDto;
import com.marchepro.offre.service.OffreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "Évaluations", description = "Évaluation technique et financière des offres par la commission")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final AppelOffreService appelOffreService;
    private final OffreService offreService;

    public EvaluationController(EvaluationService evaluationService,
                                AppelOffreService appelOffreService,
                                OffreService offreService) {
        this.evaluationService = evaluationService;
        this.appelOffreService = appelOffreService;
        this.offreService = offreService;
    }

    @GetMapping("/a-evaluer")
    @PreAuthorize("hasAnyRole('MEMBRE_COMMISSION','ADMINISTRATEUR','DIRECTEUR')")
    @Operation(summary = "Lister les appels d'offres à évaluer")
    public ApiResponse<List<AppelOffreDto>> aEvaluer() {
        return ApiResponse.success(evaluationService.appelsOffresAEvaluer().stream()
                .map(a -> appelOffreService.mapper(a, false)).toList());
    }

    @GetMapping("/appel-offre/{appelOffreId}/offres")
    @PreAuthorize("hasAnyRole('MEMBRE_COMMISSION','ADMINISTRATEUR','DIRECTEUR')")
    @Operation(summary = "Lister les offres à évaluer pour un appel d'offres")
    public ApiResponse<List<OffreDto>> offres(@PathVariable Long appelOffreId) {
        return ApiResponse.success(offreService.offresParAppelOffre(appelOffreId));
    }

    @GetMapping("/appel-offre/{appelOffreId}/consolidation")
    @PreAuthorize("hasAnyRole('MEMBRE_COMMISSION','ADMINISTRATEUR','DIRECTEUR','RESPONSABLE_SERVICE')")
    @Operation(summary = "Consolider le classement des offres")
    public ApiResponse<ConsolidationDto> consolidation(@PathVariable Long appelOffreId) {
        return ApiResponse.success(evaluationService.consolider(appelOffreId));
    }

    @GetMapping("/offre/{offreId}")
    @PreAuthorize("hasAnyRole('MEMBRE_COMMISSION','ADMINISTRATEUR','DIRECTEUR','RESPONSABLE_SERVICE')")
    @Operation(summary = "Lister les évaluations d'une offre")
    public ApiResponse<List<EvaluationDto>> parOffre(@PathVariable Long offreId) {
        return ApiResponse.success(evaluationService.evaluationsParOffre(offreId));
    }

    @PostMapping
    @PreAuthorize("hasRole('MEMBRE_COMMISSION')")
    @Operation(summary = "Enregistrer ou mettre à jour mon évaluation d'une offre")
    public ResponseEntity<ApiResponse<EvaluationDto>> enregistrer(
            @Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Évaluation enregistrée", evaluationService.enregistrer(request)));
    }
}
