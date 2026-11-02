package com.marchepro.notification.service;

import com.marchepro.common.exception.ForbiddenException;
import com.marchepro.common.exception.ResourceNotFoundException;
import com.marchepro.common.response.PageResponse;
import com.marchepro.notification.dto.NotificationDto;
import com.marchepro.notification.entity.Notification;
import com.marchepro.notification.repository.NotificationRepository;
import com.marchepro.security.SecurityUtils;
import com.marchepro.utilisateur.entity.Role;
import com.marchepro.utilisateur.entity.StatutUtilisateur;
import com.marchepro.utilisateur.entity.Utilisateur;
import com.marchepro.utilisateur.repository.UtilisateurRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UtilisateurRepository utilisateurRepository) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public void envoyer(Utilisateur utilisateur, String titre, String message, String type, String lien) {
        if (utilisateur == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setLien(lien);
        notificationRepository.save(notification);
    }

    @Transactional
    public void envoyerAuRole(Role role, String titre, String message, String type, String lien) {
        List<Utilisateur> destinataires = utilisateurRepository.findByRoleAndStatut(role, StatutUtilisateur.ACTIF);
        destinataires.forEach(u -> envoyer(u, titre, message, type, lien));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> mesNotifications(Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByUtilisateurIdOrderByDateCreationDesc(SecurityUtils.currentUserId(), pageable),
                NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long compterNonLues() {
        return notificationRepository.countByUtilisateurIdAndLueFalse(SecurityUtils.currentUserId());
    }

    @Transactional
    public void marquerLue(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable"));
        if (!notification.getUtilisateur().getId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Cette notification ne vous appartient pas");
        }
        notification.setLue(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int marquerToutesLues() {
        return notificationRepository.marquerToutesLues(SecurityUtils.currentUserId());
    }
}
