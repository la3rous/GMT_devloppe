package com.gmt.rental.dto.admin;

public record UserSummary(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String role,
        Boolean actif,
        Boolean emailVerified
) {}
