package com.marchepro.materiel.service;

import com.marchepro.common.audit.AuditService;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.materiel.dto.MaterielDto;
import com.marchepro.materiel.dto.MaterielRequest;
import com.marchepro.materiel.entity.Materiel;
import com.marchepro.materiel.repository.MaterielRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MaterielService {

    private final MaterielRepository materielRepository;
    private final AuditService auditService;

    public MaterielService(MaterielRepository materielRepository, AuditService auditService) {
        this.materielRepository = materielRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<MaterielDto> lister(String categorie, String q, Pageable pageable) {
        return PageResponse.from(materielRepository.rechercher(categorie, q, pageable), MaterielDto::from);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return materielRepository.listerCategories();
    }

    @Transactional
    public MaterielDto creer(MaterielRequest request) {
        Materiel materiel = new Materiel();
        appliquer(materiel, request);
        materielRepository.save(materiel);
        auditService.enregistrer("CREATION_MATERIEL", "MATERIEL", materiel.getId(), materiel.getDesignation());
        return MaterielDto.from(materiel);
    }

    @Transactional
    public MaterielDto modifier(Long id, MaterielRequest request) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel introuvable"));
        appliquer(materiel, request);
        materielRepository.save(materiel);
        auditService.enregistrer("MODIFICATION_MATERIEL", "MATERIEL", id, materiel.getDesignation());
        return MaterielDto.from(materiel);
    }

    @Transactional
    public void desactiver(Long id) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel introuvable"));
        materiel.setActif(false);
        materielRepository.save(materiel);
        auditService.enregistrer("DESACTIVATION_MATERIEL", "MATERIEL", id, materiel.getDesignation());
    }

    private void appliquer(Materiel materiel, MaterielRequest request) {
        materiel.setDesignation(request.designation().trim());
        materiel.setCategorie(request.categorie().trim());
        materiel.setDescription(request.description());
        materiel.setCaracteristiques(request.caracteristiques());
        materiel.setActif(true);
    }
}
