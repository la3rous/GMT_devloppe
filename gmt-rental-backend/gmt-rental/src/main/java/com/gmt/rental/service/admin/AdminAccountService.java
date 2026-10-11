package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.AccountDtos.*;
import com.gmt.rental.entity.AdminPreference;
import com.gmt.rental.entity.User;
import com.gmt.rental.repository.AdminPreferenceRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.service.VerificationService;
import com.gmt.rental.security.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class AdminAccountService {
    private final UserRepository users;
    private final AdminPreferenceRepository preferences;
    private final PasswordEncoder passwords;
    private final VerificationService verification;
    private final JwtUtils jwtUtils;

    public AdminAccountService(UserRepository users, AdminPreferenceRepository preferences, PasswordEncoder passwords, VerificationService verification, JwtUtils jwtUtils) {
        this.users = users; this.preferences = preferences; this.passwords = passwords; this.verification = verification; this.jwtUtils = jwtUtils;
    }

    public ProfileResponse profile(String email) { return toProfile(findAdmin(email)); }

    @Transactional
    public ProfileResponse updateProfile(String email, ProfileUpdate update) {
        if (update == null || blank(update.nom()) || blank(update.prenom()) || update.nom().trim().length() > 100 || update.prenom().trim().length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prénom et le nom (100 caractères maximum) sont obligatoires.");
        User user = findAdmin(email);
        user.setNom(update.nom().trim()); user.setPrenom(update.prenom().trim());
        return toProfile(users.save(user));
    }

    public void sendEmailCode(String currentEmail, EmailChange request) {
        if (request == null || request.email() == null || request.email().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail requise.");
        String target = request.email().trim().toLowerCase(Locale.ROOT);
        if (target.equalsIgnoreCase(currentEmail)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette adresse est déjà celle de votre compte.");
        verification.sendEmailChangeCode(currentEmail, target);
    }

    @Transactional
    public EmailChangeResult confirmEmailChange(String currentEmail, EmailCodeVerification request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse et code obligatoires.");
        verification.verifyEmailChangeCode(currentEmail, request.email(), request.code());
        String newEmail = request.email().trim().toLowerCase(Locale.ROOT);
        return new EmailChangeResult(profile(newEmail), jwtUtils.generateToken(newEmail));
    }

    @Transactional
    public void changePassword(String email, PasswordChange request) {
        if (request == null || blank(request.currentPassword()) || blank(request.newPassword()) || blank(request.confirmation()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les champs du mot de passe sont obligatoires.");
        if (!request.newPassword().equals(request.confirmation())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La confirmation ne correspond pas.");
        if (request.newPassword().length() < 8 || request.newPassword().length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouveau mot de passe doit contenir entre 8 et 100 caractères.");
        User user = findAdmin(email);
        if (user.getMotDePasse() == null || !passwords.matches(request.currentPassword(), user.getMotDePasse()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        user.setMotDePasse(passwords.encode(request.newPassword()));
        user.setPasswordUpdatedAt(java.time.LocalDateTime.now());
        users.save(user);
    }

    public PreferencesResponse getPreferences(String email) {
        User user = findAdmin(email);
        AdminPreference preference = preferences.findByUserId(user.getId()).orElseGet(() -> {
            AdminPreference created = new AdminPreference(); created.setUser(user); return preferences.save(created);
        });
        return new PreferencesResponse(preference.getTheme(), preference.isQuoteNotifications(), preference.getNotificationEmail());
    }

    @Transactional
    public PreferencesResponse updatePreferences(String email, PreferencesUpdate update) {
        if (update == null || !"dark".equals(update.theme()) && !"light".equals(update.theme()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thème invalide.");
        String notificationEmail = update.notificationEmail() == null ? "" : update.notificationEmail().trim();
        if (!notificationEmail.isEmpty() && (notificationEmail.length() > 254 || !notificationEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L’adresse e-mail de réception est invalide.");
        User user = findAdmin(email);
        AdminPreference preference = preferences.findByUserId(user.getId()).orElseGet(() -> { AdminPreference p = new AdminPreference(); p.setUser(user); return p; });
        preference.setTheme(update.theme()); preference.setQuoteNotifications(update.quoteNotifications());
        preference.setNotificationEmail(notificationEmail.isEmpty() ? null : notificationEmail);
        AdminPreference saved = preferences.save(preference);
        return new PreferencesResponse(saved.getTheme(), saved.isQuoteNotifications(), saved.getNotificationEmail());
    }

    private User findAdmin(String email) {
        User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable."));
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
        if (!role.equals("ADMIN") && !role.equals("ROLE_ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte administrateur requis.");
        return user;
    }
    private ProfileResponse toProfile(User u) { return new ProfileResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole(), u.getEmailVerified(), u.getCreatedAt(), u.getLastLoginAt(), u.getPasswordUpdatedAt()); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
