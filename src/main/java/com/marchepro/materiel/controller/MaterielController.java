package com.marchepro.materiel.controller;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import com.marchepro.materiel.dto.MaterielDto;
import com.marchepro.materiel.dto.MaterielRequest;
import com.marchepro.materiel.service.MaterielService;
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
@RequestMapping("/api/materiels")
@Tag(name = "Matériels", description = "Catalogue des matériels et équipements de l'organisme")
public class MaterielController {

    private final MaterielService materielService;

    public MaterielController(MaterielService materielService) {
        this.materielService = materielService;
    }

    @GetMapping
    @Operation(summary = "Lister le catalogue des matériels")
    public ApiResponse<PageResponse<MaterielDto>> lister(
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 12, sort = "designation") Pageable pageable) {
        return ApiResponse.success(materielService.lister(categorie, q, pageable));
    }

    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories disponibles")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.success(materielService.categories());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Ajouter un matériel au catalogue")
    public ResponseEntity<ApiResponse<MaterielDto>> creer(@Valid @RequestBody MaterielRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Matériel ajouté", materielService.creer(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier un matériel")
    public ApiResponse<MaterielDto> modifier(@PathVariable Long id, @Valid @RequestBody MaterielRequest request) {
        return ApiResponse.success("Matériel mis à jour", materielService.modifier(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Désactiver un matériel")
    public ApiResponse<Void> desactiver(@PathVariable Long id) {
        materielService.desactiver(id);
        return ApiResponse.success("Matériel désactivé");
    }
}
