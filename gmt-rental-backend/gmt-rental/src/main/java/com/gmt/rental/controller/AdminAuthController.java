package com.gmt.rental.controller;

import com.gmt.rental.dto.admin.LoginRequest;
import com.gmt.rental.dto.admin.LoginResponse;
import com.gmt.rental.service.admin.AdminAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthController {
    private final AdminAuthService service;
    public AdminAuthController(AdminAuthService service) { this.service = service; }
    @PostMapping("/login") public LoginResponse login(@RequestBody LoginRequest request) { return service.login(request); }
}
