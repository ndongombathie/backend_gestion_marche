package com.marchepro.rapport.controller;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.rapport.dto.DashboardDto;
import com.marchepro.rapport.dto.FournisseurDashboardDto;
import com.marchepro.rapport.dto.RapportDto;
import com.marchepro.rapport.service.DashboardService;
import com.marchepro.rapport.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rapports")
@Tag(name = "Rapports et tableaux de bord", description = "Indicateurs, exports PDF et Excel")
public class RapportController {

    private final DashboardService dashboardService;
    private final RapportService rapportService;

    public RapportController(DashboardService dashboardService, RapportService rapportService) {
        this.dashboardService = dashboardService;
        this.rapportService = rapportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','MEMBRE_COMMISSION','DIRECTEUR')")
    @Operation(summary = "Tableau de bord global")
    public ApiResponse<DashboardDto> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ApiResponse.success(dashboardService.tableauDeBord(debut, fin));
    }

    @GetMapping("/fournisseur/dashboard")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    @Operation(summary = "Tableau de bord du fournisseur connecté")
    public ApiResponse<FournisseurDashboardDto> dashboardFournisseur() {
        return ApiResponse.success(dashboardService.tableauDeBordFournisseur());
    }

    @GetMapping("/historique")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR')")
    @Operation(summary = "Historique des rapports générés")
    public ApiResponse<List<RapportDto>> historique() {
        return ApiResponse.success(rapportService.historique());
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR')")
    @Operation(summary = "Exporter le rapport de pilotage en PDF")
    public ResponseEntity<byte[]> exporterPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("rapport-marches.pdf").build());
        return ResponseEntity.ok().headers(headers).body(rapportService.exporterPdf(debut, fin));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR')")
    @Operation(summary = "Exporter les données en Excel")
    public ResponseEntity<byte[]> exporterExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("marches-export.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(rapportService.exporterExcel(debut, fin));
    }
}
