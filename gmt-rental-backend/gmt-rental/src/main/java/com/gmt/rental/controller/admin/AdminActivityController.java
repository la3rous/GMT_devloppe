package com.gmt.rental.controller.admin;

import com.gmt.rental.entity.AdminActivity;
import com.gmt.rental.service.admin.AdminActivityService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/activity")
public class AdminActivityController {
    private final AdminActivityService activities;
    public AdminActivityController(AdminActivityService activities) { this.activities = activities; }

    @GetMapping public Page<AdminActivity> search(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return activities.search(keyword, module, action, from, to, page, size);
    }

    @GetMapping("/export") public ResponseEntity<String> export(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = activities.exportCsv(keyword, module, action, from, to);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=historique-activite.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(csv);
    }
}
