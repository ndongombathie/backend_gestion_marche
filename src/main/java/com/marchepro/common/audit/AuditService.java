package com.marchepro.common.audit;

import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final HistoriqueActionRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AuditService(HistoriqueActionRepository historiqueRepository,
                        UtilisateurRepository utilisateurRepository) {
        this.historiqueRepository = historiqueRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String action, String entite, Long entiteId, String details) {
        try {
            HistoriqueAction historique = new HistoriqueAction();
            historique.setAction(action);
            historique.setEntite(entite);
            historique.setEntiteId(entiteId);
            historique.setDetails(details);
            try {
                Long userId = SecurityUtils.currentUserId();
                utilisateurRepository.findById(userId).ifPresent(historique::setUtilisateur);
            } catch (Exception ignored) {
                // Opération système ou non authentifiée
            }
            historiqueRepository.save(historique);
        } catch (Exception ex) {
            log.warn("Impossible d'enregistrer l'audit {} {}: {}", action, entite, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<HistoriqueAction> lister(String entite, Long entiteId, Pageable pageable) {
        if (entite != null && entiteId != null) {
            return historiqueRepository.findByEntiteAndEntiteIdOrderByDateActionDesc(entite, entiteId, pageable);
        }
        return historiqueRepository.findAllByOrderByDateActionDesc(pageable);
    }
}
