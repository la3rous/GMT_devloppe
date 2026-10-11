package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.UserSummary;
import com.gmt.rental.entity.User;
import com.gmt.rental.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AdminUserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final com.gmt.rental.service.VerificationService verificationService;
    private final AdminActivityService activities;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminUserService.class);

    public AdminUserService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            com.gmt.rental.service.VerificationService verificationService,
            AdminActivityService activities
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.activities = activities;
    }

    public List<UserSummary> findAll() {
        return users.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    public UserSummary create(User data) {
        validate(data, true);
        ensureEmailAvailable(data.getEmail(), null);

        User user = new User();
        copyProfile(data, user);
        user.setRole(data.getRole());
        user.setMotDePasse(passwordEncoder.encode(data.getMotDePasse()));
        user.setActif(data.getActif() == null || data.getActif());
        user.setEmailVerified(false);

        User savedUser = save(user);
        try {
            verificationService.sendEmailVerificationCode(savedUser.getEmail());
        } catch (Exception e) {
            log.warn("Échec d'envoi de l'e-mail de vérification lors de la création de l'utilisateur {} : {}", savedUser.getEmail(), e.getMessage());
        }

        return toSummary(savedUser);
    }

    @Transactional
    public UserSummary update(Long id, User data) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + id));

        validate(data, false);
        ensureEmailAvailable(data.getEmail(), id);

        if (Boolean.FALSE.equals(data.getActif()) && !Boolean.FALSE.equals(user.getActif())) {
            ensureCanDeactivate(user);
        }
        copyProfile(data, user);
        if (data.getActif() != null) user.setActif(data.getActif());
        if (data.getEmailVerified() != null) user.setEmailVerified(data.getEmailVerified());
        if (data.getMotDePasse() != null && !data.getMotDePasse().isBlank()) {
            user.setMotDePasse(passwordEncoder.encode(data.getMotDePasse()));
            user.setPasswordUpdatedAt(LocalDateTime.now());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            try {
                activities.record(authentication == null ? "" : authentication.getName(), user.getEmail(), "PASSWORD_CHANGE", "security", "Mot de passe d’un compte mis à jour par un administrateur", true);
            } catch (RuntimeException ignored) {
                // Password updates remain available if the activity store is temporarily unavailable.
            }
        }

        return toSummary(save(user));
    }

    /** The legacy DELETE operation deactivates the account and retains all related history. */
    @Transactional
    public void deactivate(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + id));
        if (Boolean.FALSE.equals(user.getActif())) return;
        ensureCanDeactivate(user);
        user.setActif(false);
        users.saveAndFlush(user);
    }

    @Transactional
    public UserSummary setActive(Long id, boolean active) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + id));
        boolean currentlyActive = !Boolean.FALSE.equals(user.getActif());
        if (currentlyActive == active) return toSummary(user);
        if (!active) ensureCanDeactivate(user);
        user.setActif(active);
        return toSummary(users.saveAndFlush(user));
    }

    private void ensureCanDeactivate(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null
                && authentication.getName().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous ne pouvez pas désactiver le compte actuellement connecté.");
        }
        if (isAdmin(user)) {
            users.lockAdministrators();
            if (users.countActiveAdministrators() <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible de désactiver le dernier administrateur actif.");
            }
        }
    }
    private boolean isAdmin(User user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
        return role.equals("ADMIN") || role.equals("ROLE_ADMIN");
    }

    private void validate(User data, boolean creating) {
        if (data == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les données utilisateur sont obligatoires.");
        }
        if (data.getPrenom() == null || data.getPrenom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prénom est obligatoire.");
        }
        if (data.getNom() == null || data.getNom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (data.getEmail() == null || !EMAIL_PATTERN.matcher(data.getEmail().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une adresse e-mail valide est obligatoire.");
        }
        if (creating && (data.getMotDePasse() == null || data.getMotDePasse().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est obligatoire pour créer un compte.");
        }
        if (creating && data.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle doit être USER ou ADMIN.");
        }
        if (creating) {
            String role = data.getRole().trim().toUpperCase(Locale.ROOT);
            if (role.startsWith("ROLE_")) role = role.substring("ROLE_".length());
            if (!ALLOWED_ROLES.contains(role)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle doit être USER ou ADMIN.");
            }
            data.setRole(role);
        }

        data.setNom(data.getNom().trim());
        data.setPrenom(data.getPrenom().trim());
        data.setEmail(data.getEmail().trim().toLowerCase(Locale.ROOT));
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (currentUserId == null || !currentUserId.equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
            }
        });
    }

    private void copyProfile(User source, User target) {
        target.setNom(source.getNom());
        target.setPrenom(source.getPrenom());
        target.setEmail(source.getEmail());
        if (source.getTelephone() != null) {
            target.setTelephone(source.getTelephone());
        }
    }

    private User save(User user) {
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.", exception);
        }
    }

    public void resendVerification(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + id));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette adresse e-mail est déjà vérifiée.");
        }
        verificationService.resendEmailVerificationCode(user.getEmail());
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getTelephone(),
                user.getRole(),
                user.getActif(),
                Boolean.TRUE.equals(user.getEmailVerified())
        );
    }
}
