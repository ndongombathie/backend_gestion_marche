package com.marchepro.notification.dto;

import com.marchepro.notification.entity.Notification;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String titre,
        String message,
        String type,
        String lien,
        boolean lue,
        Instant dateCreation
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getTitre(), n.getMessage(), n.getType(),
                n.getLien(), n.isLue(), n.getDateCreation());
    }
}
