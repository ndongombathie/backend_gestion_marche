package com.marchepro.portail;

import com.marchepro.appeloffre.dto.PublicStatsDto;
import com.marchepro.appeloffre.service.AppelOffreService;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.materiel.dto.MaterielDto;
import com.marchepro.materiel.service.MaterielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Portail public", description = "Données publiques du portail MarchéPro")
public class PublicController {

    private final AppelOffreService appelOffreService;
    private final MaterielService materielService;

    public PublicController(AppelOffreService appelOffreService, MaterielService materielService) {
        this.appelOffreService = appelOffreService;
        this.materielService = materielService;
    }

    @GetMapping("/statistiques")
    @Operation(summary = "Statistiques générales du portail")
    public ApiResponse<PublicStatsDto> statistiques() {
        return ApiResponse.success(appelOffreService.statistiquesPublic());
    }

    @GetMapping("/materiels")
    @Operation(summary = "Catalogue public des matériels")
    public ApiResponse<List<MaterielDto>> materiels(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String categorie) {
        return ApiResponse.success(materielService.lister(categorie, q, PageRequest.of(0, 24)).contenu());
    }
}
