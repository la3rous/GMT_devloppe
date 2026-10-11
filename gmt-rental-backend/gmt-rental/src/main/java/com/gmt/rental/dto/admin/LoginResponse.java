package com.gmt.rental.dto.admin;

public record LoginResponse(String token, String type, Long id, String email, String nom, String prenom, String role) {}
