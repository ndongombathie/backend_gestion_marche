package com.marchepro.common.document;

import com.marchepro.appeloffre.entity.DocumentObligatoire;
import com.marchepro.appeloffre.repository.DocumentObligatoireRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.offre.entity.Offre;
import com.marchepro.offre.repository.OffreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "zip");
    private static final Set<String> EXTENSIONS_IMAGES = Set.of("png", "jpg", "jpeg");
    private static final long TAILLE_MAX = 10L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentObligatoireRepository documentObligatoireRepository;
    private final OffreRepository offreRepository;
    private final AuditService auditService;
    private final Path racine;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentObligatoireRepository documentObligatoireRepository,
                           OffreRepository offreRepository,
                           AuditService auditService,
                           @Value("${app.upload.dir}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.documentObligatoireRepository = documentObligatoireRepository;
        this.offreRepository = offreRepository;
        this.auditService = auditService;
        this.racine = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de créer le dossier de stockage des fichiers", ex);
        }
    }

    @Transactional
    public DocumentDto stocker(MultipartFile fichier, DocumentType type, String entiteType, Long entiteId) {
        return stocker(fichier, type, entiteType, entiteId, null);
    }

    @Transactional
    public DocumentDto stocker(MultipartFile fichier, DocumentType type, String entiteType, Long entiteId,
                               Long documentObligatoireId) {
        DocumentObligatoire documentObligatoire = documentObligatoireId == null
                ? null : verifierDocumentObligatoire(documentObligatoireId, entiteType, entiteId);
        if (type == null && documentObligatoire != null) {
            type = documentObligatoire.getType();
        }
        if (type == null) {
            type = DocumentType.AUTRE;
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new BusinessException("Le fichier dépasse la taille maximale de 10 Mo", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String nomOriginal = fichier.getOriginalFilename() == null ? "document" : fichier.getOriginalFilename();
        nomOriginal = Paths.get(nomOriginal).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = extension(nomOriginal);
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new BusinessException("Format de fichier non autorisé. Formats acceptés : " + EXTENSIONS_AUTORISEES);
        }

        String dossier = entiteType.toUpperCase(Locale.ROOT) + "/" + entiteId;
        String nomStockage = dossier + "/" + UUID.randomUUID() + "." + extension;
        Path destination = racine.resolve(nomStockage).normalize();
        if (!destination.startsWith(racine)) {
            throw new BusinessException("Chemin de fichier invalide");
        }
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(fichier.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("Erreur lors de l'enregistrement du fichier");
        }

        Document document = new Document();
        document.setNom(nomOriginal);
        document.setNomStockage(nomStockage);
        document.setContentType(fichier.getContentType());
        document.setTaille(fichier.getSize());
        document.setType(type);
        document.setEntiteType(entiteType.toUpperCase(Locale.ROOT));
        document.setEntiteId(entiteId);
        document.setDocumentObligatoireId(documentObligatoireId);
        documentRepository.save(document);

        auditService.enregistrer("UPLOAD_DOCUMENT", entiteType.toUpperCase(Locale.ROOT), entiteId,
                "Fichier ajouté : " + nomOriginal + " (" + type + ")");
        return DocumentDto.from(document);
    }

    @Transactional
    public DocumentDto stockerImage(MultipartFile fichier, String entiteType, Long entiteId) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        String nomOriginal = fichier.getOriginalFilename() == null ? "image" : fichier.getOriginalFilename();
        String extension = extension(Paths.get(nomOriginal).getFileName().toString());
        if (!EXTENSIONS_IMAGES.contains(extension)) {
            throw new BusinessException("Format d'image non autorisé. Formats acceptés : JPG, JPEG, PNG");
        }
        String contentType = fichier.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("Le fichier téléversé doit être une image");
        }
        String typeEntite = entiteType.toUpperCase(Locale.ROOT);
        documentRepository.findByEntiteTypeAndEntiteIdOrderByDateUploadDesc(typeEntite, entiteId)
                .forEach(document -> supprimer(document.getId()));
        return stocker(fichier, DocumentType.IMAGE_MATERIEL, typeEntite, entiteId, null);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> lister(String entiteType, Long entiteId) {
        return documentRepository.findByEntiteTypeAndEntiteIdOrderByDateUploadDesc(
                        entiteType.toUpperCase(Locale.ROOT), entiteId)
                .stream().map(DocumentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Document charger(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable"));
    }

    @Transactional(readOnly = true)
    public Resource ressource(Document document) {
        try {
            Path chemin = racine.resolve(document.getNomStockage()).normalize();
            if (!chemin.startsWith(racine)) {
                throw new BusinessException("Chemin de fichier invalide");
            }
            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Fichier introuvable sur le serveur");
            }
            return resource;
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Fichier introuvable sur le serveur");
        }
    }

    @Transactional
    public void supprimer(Long id) {
        Document document = charger(id);
        try {
            Files.deleteIfExists(racine.resolve(document.getNomStockage()).normalize());
        } catch (IOException ignored) {
            // Le fichier peut déjà être absent : on supprime quand même la métadonnée
        }
        documentRepository.delete(document);
    }

    private DocumentObligatoire verifierDocumentObligatoire(Long documentObligatoireId, String entiteType,
                                                            Long entiteId) {
        DocumentObligatoire documentObligatoire = documentObligatoireRepository.findById(documentObligatoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Document obligatoire introuvable"));

        Long appelOffreAttendu;
        if ("APPEL_OFFRE".equalsIgnoreCase(entiteType)) {
            appelOffreAttendu = entiteId;
        } else if ("OFFRE".equalsIgnoreCase(entiteType)) {
            Offre offre = offreRepository.findById(entiteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable"));
            appelOffreAttendu = offre.getAppelOffre().getId();
        } else {
            throw new BusinessException("Un document obligatoire ne peut être rattaché qu'à une offre");
        }

        if (!documentObligatoire.getAppelOffre().getId().equals(appelOffreAttendu)) {
            throw new BusinessException("Ce document obligatoire n'appartient pas à cet appel d'offres");
        }
        return documentObligatoire;
    }

    private String extension(String nom) {
        int index = nom.lastIndexOf('.');
        if (index < 0 || index == nom.length() - 1) {
            return "";
        }
        return nom.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
