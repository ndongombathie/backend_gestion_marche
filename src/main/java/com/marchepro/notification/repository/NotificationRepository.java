package com.marchepro.notification.repository;

import com.marchepro.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId, Pageable pageable);

    long countByUtilisateurIdAndLueFalse(Long utilisateurId);

    @Modifying
    @Query("UPDATE Notification n SET n.lue = true WHERE n.utilisateur.id = :utilisateurId AND n.lue = false")
    int marquerToutesLues(@Param("utilisateurId") Long utilisateurId);
}
