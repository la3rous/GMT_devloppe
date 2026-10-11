package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.admin.DashboardSummary;
import com.gmt.rental.dto.admin.DashboardAnalytics;
import com.gmt.rental.service.admin.AdminDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService service;
    public AdminDashboardController(AdminDashboardService service) { this.service = service; }
    @GetMapping("/stats") public DashboardSummary getStats() { return service.summary(); }

    @GetMapping("/analytics")
    public DashboardAnalytics getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.analytics(from, to);
    }
}
