package com.gmt.rental.dto.admin;

import java.time.LocalDateTime;

public final class AccountDtos {
    private AccountDtos() {}
    public record ProfileResponse(Long id, String nom, String prenom, String email, String role,
                                  Boolean emailVerified, LocalDateTime createdAt, LocalDateTime lastLoginAt,
                                  LocalDateTime passwordUpdatedAt) {}
    public record ProfileUpdate(String nom, String prenom) {}
    public record EmailChange(String email) {}
    public record EmailCodeVerification(String email, String code) {}
    public record EmailChangeResult(ProfileResponse profile, String token) {}
    public record PasswordChange(String currentPassword, String newPassword, String confirmation) {}
    public record PreferencesResponse(String theme, boolean quoteNotifications, String notificationEmail) {}
    public record PreferencesUpdate(String theme, boolean quoteNotifications, String notificationEmail) {}
    public record EmailTestRequest(String email) {}
    public record EmailTestResponse(String status, String message, String messageId) {}
}
