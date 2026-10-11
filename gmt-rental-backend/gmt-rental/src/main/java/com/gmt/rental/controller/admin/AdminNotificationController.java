package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.AdminNotificationDTO;
import com.gmt.rental.service.admin.AdminNotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final AdminNotificationService service;

    public AdminNotificationController(AdminNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminNotificationDTO> getRecent() {
        return service.findRecent();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(service.countUnread());
    }

    @PatchMapping("/{id}/read")
    public AdminNotificationDTO markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    @PatchMapping("/read-all")
    public ReadAllResponse markAllAsRead() {
        return new ReadAllResponse(service.markAllAsRead());
    }

    public record UnreadCountResponse(long count) {
    }

    public record ReadAllResponse(int updated) {
    }
}
