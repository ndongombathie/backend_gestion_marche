package com.marchepro.notification.controller;

import com.marchepro.common.response.ApiResponse;
import com.marchepro.common.response.PageResponse;
import com.marchepro.notification.dto.NotificationDto;
import com.marchepro.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notifications de l'utilisateur connecté")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lister mes notifications")
    public ApiResponse<PageResponse<NotificationDto>> lister(
            @PageableDefault(size = 15) Pageable pageable) {
        return ApiResponse.success(notificationService.mesNotifications(pageable));
    }

    @GetMapping("/non-lues/count")
    @Operation(summary = "Compter mes notifications non lues")
    public ApiResponse<Map<String, Long>> compterNonLues() {
        return ApiResponse.success(Map.of("nonLues", notificationService.compterNonLues()));
    }

    @PutMapping("/{id}/lire")
    @Operation(summary = "Marquer une notification comme lue")
    public ApiResponse<Void> marquerLue(@PathVariable Long id) {
        notificationService.marquerLue(id);
        return ApiResponse.success("Notification marquée comme lue");
    }

    @PutMapping("/lire-tout")
    @Operation(summary = "Marquer toutes mes notifications comme lues")
    public ApiResponse<Map<String, Integer>> marquerToutesLues() {
        return ApiResponse.success(Map.of("misesAJour", notificationService.marquerToutesLues()));
    }
}
