package com.gmt.rental.service.admin;

import com.gmt.rental.dto.AdminNotificationDTO;
import com.gmt.rental.entity.Notification;
import com.gmt.rental.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminNotificationService {
    private final NotificationRepository notifications;

    public AdminNotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationDTO> findRecent() {
        return notifications.findTop50ByOrderByDateCreationDescIdDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return notifications.countByLuFalse();
    }

    @Transactional
    public AdminNotificationDTO markAsRead(Long id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        if (!notification.isLu()) {
            notification.setLu(true);
            notifications.save(notification);
        }
        return toDto(notification);
    }

    @Transactional
    public int markAllAsRead() {
        return notifications.markAllAsRead();
    }

    private AdminNotificationDTO toDto(Notification notification) {
        var quote = notification.getDevis();
        var vehicle = quote.getVehicule();
        String vehicleName = vehicle == null ? "Véhicule non spécifié" : vehicle.getMarque() + " " + vehicle.getModele();
        return new AdminNotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isLu(),
                notification.getDateCreation(),
                quote.getId(),
                quote.getNom(),
                quote.getPrenom(),
                vehicleName,
                quote.getDateDebut(),
                quote.getDateFin()
        );
    }
}
