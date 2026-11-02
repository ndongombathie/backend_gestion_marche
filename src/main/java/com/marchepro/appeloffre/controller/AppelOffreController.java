package com.marchepro.appeloffre.controller;

import com.marchepro.appeloffre.dto.AppelOffreDto;
import com.marchepro.appeloffre.dto.AppelOffreRequest;
import com.marchepro.appeloffre.dto.ProlongationRequest;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.service.AppelOffreService;
import com.marchepro.common.dto.DecisionRequest;
import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appels-offres")
@Tag(name = "Appels d'offres", description = "Publication et gestion des appels d'offres")
public class AppelOffreController {

    private final AppelOffreService appelOffreService;

    public AppelOffreController(AppelOffreService appelOffreService) {
        this.appelOffreService = appelOffreService;
    }

    @GetMapping("/public")
    @Operation(summary = "Catalogue public des appels d'offres publiés")
    public ApiResponse<PageResponse<AppelOffreDto>> listerPublic(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String organisme,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateLimiteApres,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateLimiteAvant,
            @PageableDefault(size = 9, sort = "datePublication") Pageable pageable) {
        return ApiResponse.success(appelOffreService.listerPublic(
                q, categorie, organisme, dateLimiteApres, dateLimiteAvant, pageable));
    }

    @GetMapping("/public/categories")
    @Operation(summary = "Catégories publiques disponibles")
    public ApiResponse<List<String>> categoriesPubliques() {
        return ApiResponse.success(appelOffreService.categories());
    }

    @GetMapping("/public/organismes")
    @Operation(summary = "Organismes publics disponibles")
    public ApiResponse<List<String>> organismesPublics() {
        return ApiResponse.success(appelOffreService.organismes());
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Détail public d'un appel d'offres")
    public ApiResponse<AppelOffreDto> parIdPublic(@PathVariable Long id) {
        return ApiResponse.success(appelOffreService.parIdPublic(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION')")
    @Operation(summary = "Lister les appels d'offres (espace interne)")
    public ApiResponse<PageResponse<AppelOffreDto>> lister(
            @RequestParam(required = false) StatutAppelOffre statut,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "dateCreation") Pageable pageable) {
        return ApiResponse.success(appelOffreService.lister(statut, q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','RESPONSABLE_SERVICE','DIRECTEUR','MEMBRE_COMMISSION','FOURNISSEUR')")
    @Operation(summary = "Détail d'un appel d'offres")
    public ApiResponse<AppelOffreDto> parId(@PathVariable Long id) {
        return ApiResponse.success(appelOffreService.parId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Créer un appel d'offres à partir d'un marché validé")
    public ResponseEntity<ApiResponse<AppelOffreDto>> creer(@Valid @RequestBody AppelOffreRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Appel d'offres créé", appelOffreService.creer(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Modifier un appel d'offres en brouillon")
    public ApiResponse<AppelOffreDto> modifier(@PathVariable Long id,
                                               @Valid @RequestBody AppelOffreRequest request) {
        return ApiResponse.success("Appel d'offres mis à jour", appelOffreService.modifier(id, request));
    }

    @PostMapping("/{id}/publier")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Publier un appel d'offres")
    public ApiResponse<AppelOffreDto> publier(@PathVariable Long id) {
        return ApiResponse.success("Appel d'offres publié", appelOffreService.publier(id));
    }

    @PostMapping("/{id}/prolonger")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Prolonger la date limite avec justification")
    public ApiResponse<AppelOffreDto> prolonger(@PathVariable Long id,
                                                @Valid @RequestBody ProlongationRequest request) {
        return ApiResponse.success("Date limite prolongée", appelOffreService.prolonger(id, request));
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('RESPONSABLE_SERVICE','ADMINISTRATEUR')")
    @Operation(summary = "Annuler un appel d'offres")
    public ApiResponse<AppelOffreDto> annuler(@PathVariable Long id,
                                              @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("Appel d'offres annulé", appelOffreService.annuler(id, request));
    }
}
