package com.gmt.rental.service;

import com.gmt.rental.dto.verification.VerificationResponse;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.VerificationCode;
import com.gmt.rental.entity.VerificationCodeType;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VerificationCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_RESENDS_PER_HOUR = 5;

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationService(
            VerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void sendEmailChangeCode(String currentEmail, String requestedEmail) {
        if (currentEmail == null || requestedEmail == null || requestedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail obligatoire.");
        }
        String target = requestedEmail.trim().toLowerCase(Locale.ROOT);
        if (!target.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse e-mail invalide.");
        }
        if (userRepository.findByEmailIgnoreCase(target).filter(user -> !user.getEmail().equalsIgnoreCase(currentEmail)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
        }
        createAndSendOtp(target, VerificationCodeType.EMAIL_CHANGE);
    }

    @Transactional
    public void verifyEmailChangeCode(String currentEmail, String requestedEmail, String rawCode) {
        if (requestedEmail == null || requestedEmail.isBlank() || rawCode == null || !rawCode.trim().matches("\\d{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse et code de vérification valides requis.");
        }
        String target = requestedEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(currentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable."));
        if (userRepository.findByEmailIgnoreCase(target).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
        }
        VerificationCode active = verificationCodeRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(target, VerificationCodeType.EMAIL_CHANGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun code actif. Demandez-en un nouveau."));
        if (active.isExpired() || active.getAttempts() >= MAX_ATTEMPTS) {
            active.setUsed(true);
            verificationCodeRepository.save(active);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code a expiré. Demandez-en un nouveau.");
        }
        if (!passwordEncoder.matches(rawCode.trim(), active.getCodeHash())) {
            active.setAttempts(active.getAttempts() + 1);
            if (active.getAttempts() >= MAX_ATTEMPTS) active.setUsed(true);
            verificationCodeRepository.save(active);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code incorrect.");
        }
        active.setUsed(true);
        active.setUsedAt(Instant.now());
        verificationCodeRepository.save(active);
        user.setEmail(target);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Generates and dispatches a 6-digit OTP code for email verification.
     */
    @Transactional
    public void sendEmailVerificationCode(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est obligatoire.");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            // Generic response to prevent user enumeration
            log.info("Demande de code OTP pour un email non enregistré : {}", email);
            return;
        }

        User user = userOptional.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette adresse e-mail est déjà vérifiée.");
        }

        createAndSendOtp(email, VerificationCodeType.EMAIL_VERIFICATION);
    }

    /**
     * Resends a verification code respecting cooldown and hourly limits.
     */
    @Transactional
    public void resendEmailVerificationCode(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est obligatoire.");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            // Generic response to prevent user enumeration
            log.info("Demande de renvoi OTP pour email inconnu : {}", email);
            return;
        }

        User user = userOptional.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette adresse e-mail est déjà vérifiée.");
        }

        createAndSendOtp(email, VerificationCodeType.EMAIL_VERIFICATION);
    }

    /**
     * Verifies the 6-digit OTP code for email verification.
     */
    @Transactional
    public VerificationResponse verifyEmailCode(String rawEmail, String rawCode) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adresse e-mail est obligatoire.");
        }
        if (rawCode == null || rawCode.trim().length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de vérification doit comporter 6 chiffres.");
        }

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String code = rawCode.trim();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compte introuvable pour cette adresse e-mail."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new VerificationResponse(true, "Votre adresse e-mail est déjà vérifiée.");
        }

        VerificationCode activeCode = verificationCodeRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(email, VerificationCodeType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun code actif trouvé. Veuillez demander un nouveau code."));

        if (activeCode.isExpired()) {
            activeCode.setUsed(true);
            verificationCodeRepository.save(activeCode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code a expiré. Veuillez demander un nouveau code.");
        }

        if (activeCode.getAttempts() >= MAX_ATTEMPTS) {
            activeCode.setUsed(true);
            verificationCodeRepository.save(activeCode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre maximal de tentatives dépassé. Veuillez demander un nouveau code.");
        }

        if (!passwordEncoder.matches(code, activeCode.getCodeHash())) {
            activeCode.setAttempts(activeCode.getAttempts() + 1);
            int remainingAttempts = MAX_ATTEMPTS - activeCode.getAttempts();
            if (remainingAttempts <= 0) {
                activeCode.setUsed(true);
            }
            verificationCodeRepository.save(activeCode);

            if (remainingAttempts <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code incorrect. Nombre maximal de tentatives atteint, ce code est invalidé.");
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code incorrect. " + remainingAttempts + " tentative(s) restante(s).");
            }
        }

        // Code is valid
        activeCode.setUsed(true);
        activeCode.setUsedAt(Instant.now());
        verificationCodeRepository.save(activeCode);

        // Email verification must not override an administrator's account deactivation.
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Adresse e-mail vérifiée avec succès pour l'utilisateur id={}", user.getId());
        return new VerificationResponse(true, "Votre adresse e-mail a été vérifiée avec succès. Vous pouvez maintenant vous connecter.");
    }

    /**
     * Internal method to generate, hash, persist, and send OTP.
     */
    private void createAndSendOtp(String email, VerificationCodeType type) {
        // Rate limiting: Hourly limit
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        long countLastHour = verificationCodeRepository.countByEmailIgnoreCaseAndTypeAndCreatedAtAfter(email, type, oneHourAgo);
        if (countLastHour >= MAX_RESENDS_PER_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de demandes de code. Veuillez réessayer dans une heure.");
        }

        // Rate limiting: Cooldown check
        Optional<VerificationCode> lastCodeOpt = verificationCodeRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(email, type);

        if (lastCodeOpt.isPresent()) {
            VerificationCode lastCode = lastCodeOpt.get();
            long secondsSinceLast = Duration.between(lastCode.getCreatedAt(), Instant.now()).getSeconds();
            if (secondsSinceLast < COOLDOWN_SECONDS) {
                long waitTime = COOLDOWN_SECONDS - secondsSinceLast;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Veuillez patienter " + waitTime + " secondes avant de demander un nouveau code.");
            }
        }

        // Invalidate old unused codes
        List<VerificationCode> oldCodes = verificationCodeRepository.findByEmailIgnoreCaseAndTypeAndUsedFalse(email, type);
        for (VerificationCode oldCode : oldCodes) {
            oldCode.setUsed(true);
        }
        verificationCodeRepository.saveAll(oldCodes);

        // Generate 6-digit random code securely
        int numericCode = 100000 + secureRandom.nextInt(900000);
        String plainCode = String.valueOf(numericCode);
        String hashedCode = passwordEncoder.encode(plainCode);

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(OTP_EXPIRATION_MINUTES));
        VerificationCode newVerificationCode = new VerificationCode(email, hashedCode, type, expiresAt);
        verificationCodeRepository.save(newVerificationCode);

        // Dispatch real email
        if (type == VerificationCodeType.EMAIL_VERIFICATION) {
            emailService.sendVerificationOtpEmail(email, plainCode, OTP_EXPIRATION_MINUTES);
        } else if (type == VerificationCodeType.EMAIL_CHANGE) {
            emailService.sendEmailChangeOtpEmail(email, plainCode, OTP_EXPIRATION_MINUTES);
        } else {
            emailService.sendPasswordResetOtpEmail(email, plainCode, OTP_EXPIRATION_MINUTES);
        }
    }
}

