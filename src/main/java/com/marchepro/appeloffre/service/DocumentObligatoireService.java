package com.marchepro.appeloffre.service;

import com.marchepro.appeloffre.dto.DocumentObligatoireDto;
import com.marchepro.appeloffre.dto.DocumentObligatoireRequest;
import com.marchepro.appeloffre.dto.DocumentObligatoireSuiviDto;
import com.marchepro.appeloffre.entity.AppelOffre;
import com.marchepro.appeloffre.entity.DocumentObligatoire;
import com.marchepro.appeloffre.entity.StatutAppelOffre;
import com.marchepro.appeloffre.repository.AppelOffreRepository;
import com.marchepro.appeloffre.repository.DocumentObligatoireRepository;
import com.marchepro.common.audit.AuditService;
import com.marchepro.common.document.DocumentDto;
import com.marchepro.common.document.DocumentRepository;
import com.marchepro.common.document.DocumentType;
import com.marchepro.common.exception.BusinessException;
import com.marchepro.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentObligatoireService {

    private static final List<DocumentType> TYPES_PAR_DEFAUT = List.of(
            DocumentType.OFFRE_TECHNIQUE,
            DocumentType.OFFRE_FINANCIERE,
            DocumentType.DOCUMENT_ADMINISTRATIF);

    private final DocumentObligatoireRepository documentObligatoireRepository;
    private final AppelOffreRepository appelOffreRepository;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;

    public DocumentObligatoireService(DocumentObligatoireRepository documentObligatoireRepository,
                                      AppelOffreRepository appelOffreRepository,
                                      DocumentRepository documentRepository,
                                      AuditService auditService) {
        this.documentObligatoireRepository = documentObligatoireRepository;
        this.appelOffreRepository = appelOffreRepository;
        this.documentRepository = documentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DocumentObligatoireDto> lister(Long appelOffreId) {
        return documentObligatoireRepository.findByAppelOffreIdOrderByOrdreAscIdAsc(appelOffreId)
                .stream().map(DocumentObligatoireDto::from).toList();
    }

    @Transactional
    public DocumentObligatoireDto ajouter(Long appelOffreId, DocumentObligatoireRequest request) {
        AppelOffre appelOffre = chargerAppelOffreModifiable(appelOffreId);
        String libelle = request.libelle().trim();
        verifierLibelleUnique(appelOffreId, libelle, null);

        DocumentObligatoire document = new DocumentObligatoire();
        document.setAppelOffre(appelOffre);
        appliquer(document, request, libelle);
        if (request.ordre() == null) {
            document.setOrdre((int) documentObligatoireRepository.countByAppelOffreId(appelOffreId) + 1);
        }
        documentObligatoireRepository.save(document);
        auditService.enregistrer("AJOUT_DOCUMENT_OBLIGATOIRE", "APPEL_OFFRE", appelOffreId, libelle);
        return DocumentObligatoireDto.from(document);
    }

    @Transactional
    public DocumentObligatoireDto modifier(Long appelOffreId, Long id, DocumentObligatoireRequest request) {
        DocumentObligatoire document = charger(appelOffreId, id);
        AppelOffre appelOffre = chargerAppelOffreModifiable(appelOffreId);
        String libelle = request.libelle().trim();
        verifierLibelleUnique(appelOffre.getId(), libelle, id);

        appliquer(document, request, libelle);
        documentObligatoireRepository.save(document);
        auditService.enregistrer("MODIFICATION_DOCUMENT_OBLIGATOIRE", "APPEL_OFFRE",
                appelOffre.getId(), libelle);
        return DocumentObligatoireDto.from(document);
    }

    @Transactional
    public void supprimer(Long appelOffreId, Long id) {
        DocumentObligatoire document = charger(appelOffreId, id);
        AppelOffre appelOffre = chargerAppelOffreModifiable(appelOffreId);
        if (documentRepository.existsByDocumentObligatoireId(id)) {
            throw new BusinessException("Ce document obligatoire possède déjà des fichiers téléversés");
        }
        documentObligatoireRepository.delete(document);
        auditService.enregistrer("SUPPRESSION_DOCUMENT_OBLIGATOIRE", "APPEL_OFFRE",
                appelOffre.getId(), document.getLibelle());
    }

    @Transactional
    public List<DocumentObligatoireDto> remplacer(Long appelOffreId, List<DocumentObligatoireRequest> requests) {
        AppelOffre appelOffre = chargerAppelOffreModifiable(appelOffreId);
        List<DocumentObligatoire> existants =
                documentObligatoireRepository.findByAppelOffreIdOrderByOrdreAscIdAsc(appelOffreId);
        for (DocumentObligatoire existant : existants) {
            if (documentRepository.existsByDocumentObligatoireId(existant.getId())) {
                throw new BusinessException("Des fichiers sont déjà téléversés pour ce dossier : "
                        + existant.getLibelle());
            }
        }

        Set<String> libelles = new HashSet<>();
        for (DocumentObligatoireRequest request : requests) {
            String libelle = request.libelle().trim();
            if (!libelles.add(libelle.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("Le document obligatoire est défini en double : " + libelle);
            }
        }

        documentObligatoireRepository.deleteAll(existants);
        List<DocumentObligatoireDto> resultat = new ArrayList<>();
        int ordre = 1;
        for (DocumentObligatoireRequest request : requests) {
            DocumentObligatoire document = new DocumentObligatoire();
            document.setAppelOffre(appelOffre);
            String libelle = request.libelle().trim();
            appliquer(document, request, libelle);
            document.setOrdre(request.ordre() != null ? request.ordre() : ordre);
            ordre++;
            documentObligatoireRepository.save(document);
            resultat.add(DocumentObligatoireDto.from(document));
        }
        auditService.enregistrer("CONFIGURATION_DOCUMENTS_OBLIGATOIRES", "APPEL_OFFRE", appelOffreId,
                resultat.size() + " document(s) obligatoire(s)");
        return resultat;
    }

    @Transactional(readOnly = true)
    public List<DocumentObligatoireSuiviDto> suivre(Long appelOffreId, List<DocumentDto> documents) {
        List<DocumentObligatoire> definitions =
                documentObligatoireRepository.findByAppelOffreIdOrderByOrdreAscIdAsc(appelOffreId);
        if (definitions.isEmpty()) {
            return suiviParDefaut(documents);
        }
        List<DocumentObligatoireSuiviDto> suivi = new ArrayList<>();
        for (DocumentObligatoire definition : definitions) {
            List<DocumentDto> fichiers = documents.stream()
                    .filter(document -> correspond(document, definition))
                    .toList();
            suivi.add(DocumentObligatoireSuiviDto.from(DocumentObligatoireDto.from(definition),
                    fichiers, !fichiers.isEmpty()));
        }
        return suivi;
    }

    @Transactional(readOnly = true)
    public void verifierCompletion(Long appelOffreId, List<DocumentDto> documents) {
        List<DocumentObligatoire> definitions =
                documentObligatoireRepository.findByAppelOffreIdOrderByOrdreAscIdAsc(appelOffreId);
        if (definitions.isEmpty()) {
            verifierTypesParDefaut(documents);
            return;
        }
        List<String> manquants = new ArrayList<>();
        for (DocumentObligatoire definition : definitions) {
            if (definition.isObligatoire()
                    && documents.stream().noneMatch(document -> correspond(document, definition))) {
                manquants.add(definition.getLibelle());
            }
        }
        if (!manquants.isEmpty()) {
            throw new BusinessException("Veuillez joindre les documents obligatoires suivants : "
                    + String.join(", ", manquants));
        }
    }

    private List<DocumentObligatoireSuiviDto> suiviParDefaut(List<DocumentDto> documents) {
        List<DocumentObligatoireSuiviDto> suivi = new ArrayList<>();
        int ordre = 1;
        for (DocumentType type : TYPES_PAR_DEFAUT) {
            List<DocumentDto> fichiers = documents.stream()
                    .filter(document -> document.type() == type)
                    .toList();
            suivi.add(new DocumentObligatoireSuiviDto(null, libelleParDefaut(type),
                    descriptionParDefaut(type), type, true, ordre++, fichiers, !fichiers.isEmpty()));
        }
        return suivi;
    }

    private void verifierTypesParDefaut(List<DocumentDto> documents) {
        List<String> manquants = new ArrayList<>();
        for (DocumentType type : TYPES_PAR_DEFAUT) {
            boolean present = documents.stream().anyMatch(document -> document.type() == type);
            if (!present) {
                manquants.add(libelleParDefaut(type));
            }
        }
        if (!manquants.isEmpty()) {
            throw new BusinessException("Veuillez joindre les documents obligatoires suivants : "
                    + String.join(", ", manquants));
        }
    }

    private boolean correspond(DocumentDto document, DocumentObligatoire definition) {
        if (document.documentObligatoireId() != null) {
            return document.documentObligatoireId().equals(definition.getId());
        }
        return definition.getType() != null && document.type() == definition.getType();
    }

    private void appliquer(DocumentObligatoire document, DocumentObligatoireRequest request, String libelle) {
        document.setLibelle(libelle);
        document.setDescription(request.description());
        document.setType(request.type());
        document.setObligatoire(request.obligatoire() == null || request.obligatoire());
        if (request.ordre() != null) {
            document.setOrdre(request.ordre());
        }
    }

    private void verifierLibelleUnique(Long appelOffreId, String libelle, Long id) {
        boolean existe = id == null
                ? documentObligatoireRepository.existsByAppelOffreIdAndLibelleIgnoreCase(appelOffreId, libelle)
                : documentObligatoireRepository.existsByAppelOffreIdAndLibelleIgnoreCaseAndIdNot(
                        appelOffreId, libelle, id);
        if (existe) {
            throw new BusinessException("Un document obligatoire porte déjà ce libellé : " + libelle);
        }
    }

    private DocumentObligatoire charger(Long appelOffreId, Long id) {
        DocumentObligatoire document = documentObligatoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document obligatoire introuvable"));
        if (!document.getAppelOffre().getId().equals(appelOffreId)) {
            throw new ResourceNotFoundException("Document obligatoire introuvable");
        }
        return document;
    }

    private AppelOffre chargerAppelOffreModifiable(Long appelOffreId) {
        AppelOffre appelOffre = appelOffreRepository.findById(appelOffreId)
                .orElseThrow(() -> new ResourceNotFoundException("Appel d'offres introuvable"));
        if (appelOffre.getStatut() != StatutAppelOffre.BROUILLON) {
            throw new BusinessException(
                    "Les documents obligatoires ne peuvent être modifiés que sur un appel d'offres en brouillon");
        }
        return appelOffre;
    }

    private String libelleParDefaut(DocumentType type) {
        return switch (type) {
            case OFFRE_TECHNIQUE -> "Offre technique";
            case OFFRE_FINANCIERE -> "Offre financière";
            case DOCUMENT_ADMINISTRATIF -> "Documents administratifs";
            default -> "Document obligatoire";
        };
    }

    private String descriptionParDefaut(DocumentType type) {
        return switch (type) {
            case OFFRE_TECHNIQUE -> "Mémoire technique, descriptifs et fiches produits.";
            case OFFRE_FINANCIERE -> "Bordereau des prix et devis détaillé.";
            case DOCUMENT_ADMINISTRATIF -> "NINEA, RCCM, attestations fiscales et sociales.";
            default -> null;
        };
    }
}
