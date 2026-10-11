package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.admin.RentalHistoryRequest;
import com.gmt.rental.dto.admin.RentalHistoryResponse;
import com.gmt.rental.service.admin.AdminRentalHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users/{userId}/rentals")
public class AdminRentalHistoryController {
    private final AdminRentalHistoryService service;

    public AdminRentalHistoryController(AdminRentalHistoryService service) { this.service = service; }

    @GetMapping
    public List<RentalHistoryResponse> getByUser(@PathVariable Long userId) {
        return service.findByUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalHistoryResponse create(@PathVariable Long userId, @RequestBody RentalHistoryRequest request) {
        return service.create(userId, request);
    }
}
