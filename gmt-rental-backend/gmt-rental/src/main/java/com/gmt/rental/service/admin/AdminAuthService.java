package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.LoginRequest;
import com.gmt.rental.dto.admin.LoginResponse;
import com.gmt.rental.entity.User;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.security.JwtUtils;
import com.gmt.rental.service.admin.AdminActivityService;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AdminActivityService activities;

    public AdminAuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, AdminActivityService activities) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.activities = activities;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.motDePasse() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail et mot de passe requis");
        }
        User user = users.findByEmail(request.email().trim()).orElse(null);
        if (user == null) {
            activities.record(request.email().trim(), "", "LOGIN_FAILED", "security", "Tentative de connexion refusée", false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean admin = role.equals("ADMIN") || role.equals("ROLE_ADMIN");
        if (!admin || user.getMotDePasse() == null || !passwordEncoder.matches(request.motDePasse(), user.getMotDePasse())) {
            activities.record(user.getEmail(), "", "LOGIN_FAILED", "security", "Tentative de connexion refusée", false);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            activities.record(user.getEmail(), "", "LOGIN_FAILED", "security", "Connexion refusée : adresse e-mail non vérifiée", false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veuillez vérifier votre adresse email avant de vous connecter.");
        }
        if (Boolean.FALSE.equals(user.getActif())) {
            activities.record(user.getEmail(), "", "LOGIN_FAILED", "security", "Connexion refusée : compte désactivé", false);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est désactivé.");
        }
        user.setLastLoginAt(LocalDateTime.now());
        users.save(user);
        activities.record(user.getEmail(), "", "LOGIN", "security", "Connexion administrateur réussie", true);
        return new LoginResponse(jwtUtils.generateToken(user.getEmail()), "Bearer", user.getId(), user.getEmail(), user.getNom(), user.getPrenom(), "ROLE_ADMIN");
    }
}
