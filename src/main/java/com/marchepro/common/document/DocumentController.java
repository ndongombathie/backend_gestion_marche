package com.marchepro.common.document;

import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.common.exception.ForbiddenException;
import com.marchepro.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Téléversement et téléchargement des pièces justificatives")
public class DocumentController {

    private final DocumentService documentService;
    private final AppelOffreRepository appelOffreRepository;

    public DocumentController(DocumentService documentService, AppelOffreRepository appelOffreRepository) {
        this.documentService = documentService;
        this.appelOffreRepository = appelOffreRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Téléverser un document")
    public ApiResponse<DocumentDto> upload(@RequestParam("fichier") MultipartFile fichier,
                                           @RequestParam("type") DocumentType type,
                                           @RequestParam("entiteType") String entiteType,
                                           @RequestParam("entiteId") Long entiteId) {
        return ApiResponse.success("Document téléversé",
                documentService.stocker(fichier, type, entiteType, entiteId));
    }

    @GetMapping
    @Operation(summary = "Lister les documents d'une entité")
    public ApiResponse<List<DocumentDto>> lister(@RequestParam String entiteType,
                                                 @RequestParam Long entiteId) {
        return ApiResponse.success(documentService.lister(entiteType, entiteId));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Télécharger un document")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Document document = documentService.charger(id);
        verifierAcces(document);
        Resource resource = documentService.ressource(document);
        String contentType = document.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : document.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.getNom(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un document")
    public ApiResponse<Void> supprimer(@PathVariable Long id) {
        documentService.supprimer(id);
        return ApiResponse.success("Document supprimé");
    }

    private void verifierAcces(Document document) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authentifie = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        if (authentifie) {
            return;
        }
        if ("APPEL_OFFRE".equalsIgnoreCase(document.getEntiteType())) {
            AppelOffre appelOffre = appelOffreRepository.findById(document.getEntiteId()).orElse(null);
            if (appelOffre != null && appelOffre.getStatut() == StatutAppelOffre.PUBLIE) {
                return;
            }
        }
        throw new ForbiddenException("Vous devez être connecté pour télécharger ce document");
    }
}
