package com.marchepro.common.audit;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('ADMINISTRATEUR','DIRECTEUR')")
@Tag(name = "Audit", description = "Journal des opérations sensibles")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    public record AuditDto(Long id, String utilisateur, String action, String entite, Long entiteId,
                           String details, Instant dateAction) {
    }

    @GetMapping
    @Operation(summary = "Consulter le journal d'audit")
    public ApiResponse<PageResponse<AuditDto>> lister(
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) Long entiteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(
                auditService.lister(entite, entiteId, pageable),
                h -> new AuditDto(h.getId(),
                        h.getUtilisateur() != null ? h.getUtilisateur().getNomComplet() : "Système",
                        h.getAction(), h.getEntite(), h.getEntiteId(), h.getDetails(), h.getDateAction())));
    }
}
