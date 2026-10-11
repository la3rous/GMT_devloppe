package com.gmt.rental;

import com.gmt.rental.entity.User;
import com.gmt.rental.entity.VerificationCode;
import com.gmt.rental.entity.VerificationCodeType;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VerificationCodeRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gmt_rental_otp_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.mail.host=localhost",
        "spring.mail.port=25",
        "spring.mail.username=test-smtp-user",
        "spring.mail.password=test-smtp-password",
        "spring.mail.from=test-smtp-user@gmt.test",
        "gmt.jwt.secret=test-only-jwt-signing-key-long-enough-for-hmac"
})
@AutoConfigureMockMvc
class EmailVerificationIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private VerificationCodeRepository verificationCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JavaMailSender mockMailSender;

    @TestConfiguration
    static class TestMailConfig {
        @Bean
        @Primary
        public JavaMailSender javaMailSender() {
            JavaMailSender sender = mock(JavaMailSender.class);
            when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
            return sender;
        }
    }

    @BeforeEach
    void resetMock() {
        reset(mockMailSender);
        when(mockMailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @Transactional
    void unverifiedUserCannotLoginUntilEmailIsVerified() throws Exception {
        String email = "unverified-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("Unverified");
        user.setRole("ADMIN");
        user.setActif(true);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("secret123"));
        userRepository.save(user);

        // Incorrect password -> 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"motDePasse\":\"wrong-password\"}".formatted(email)))
                .andExpect(status().isUnauthorized());

        // Correct password but unverified -> 403 Forbidden
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"motDePasse\":\"secret123\"}".formatted(email)))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("Veuillez vérifier votre adresse email avant de vous connecter."));
    }

    @Test
    @Transactional
    void sendOtpGeneratesHashedCodeAndSendsEmail() throws Exception {
        String email = "newuser-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("New");
        user.setPrenom("User");
        user.setRole("USER");
        user.setActif(false);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("password123"));
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/verification/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify that JavaMailSender sent a MimeMessage
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockMailSender, times(1)).send(captor.capture());
        MimeMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);

        // Verify DB persistence of code
        VerificationCode codeEntity = verificationCodeRepository
                .findTopByEmailIgnoreCaseAndTypeAndUsedFalseOrderByCreatedAtDesc(email, VerificationCodeType.EMAIL_VERIFICATION)
                .orElseThrow();

        assertNotNull(codeEntity.getCodeHash());
        assertFalse(codeEntity.isUsed());
        assertEquals(0, codeEntity.getAttempts());
        assertEquals(email, codeEntity.getEmail());
        assertEquals(VerificationCodeType.EMAIL_VERIFICATION, codeEntity.getType());
        assertTrue(codeEntity.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @Transactional
    void validOtpVerifiesEmailAndKeepsAnActiveAccountActive() throws Exception {
        String email = "verify-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("ToVerify");
        user.setRole("USER");
        user.setActif(true);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("pwd123"));
        userRepository.save(user);

        // Generate known OTP
        String plainCode = "654321";
        VerificationCode code = new VerificationCode(
                email,
                passwordEncoder.encode(plainCode),
                VerificationCodeType.EMAIL_VERIFICATION,
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );
        verificationCodeRepository.save(code);

        // Verify with valid code
        mockMvc.perform(post("/api/auth/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, plainCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Check user is now verified and active
        User verifiedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(verifiedUser.getEmailVerified());
        assertTrue(verifiedUser.getActif());

        // Check code is marked used
        VerificationCode usedCode = verificationCodeRepository.findById(code.getId()).orElseThrow();
        assertTrue(usedCode.isUsed());
        assertNotNull(usedCode.getUsedAt());
    }

    @Test
    @Transactional
    void validOtpCannotReactivateAnAdministrativelyDeactivatedAccount() throws Exception {
        String email = "disabled-verify-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("Disabled");
        user.setRole("USER");
        user.setActif(false);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("pwd123"));
        userRepository.save(user);

        String plainCode = "654322";
        VerificationCode code = new VerificationCode(email, passwordEncoder.encode(plainCode),
                VerificationCodeType.EMAIL_VERIFICATION, Instant.now().plus(5, ChronoUnit.MINUTES));
        verificationCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, plainCode)))
                .andExpect(status().isOk());

        User verifiedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(verifiedUser.getEmailVerified());
        assertFalse(verifiedUser.getActif());
    }

    @Test
    @Transactional
    void incorrectOtpIncrementsAttemptsAndRejects() throws Exception {
        String email = "incorrect-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("Fail");
        user.setRole("USER");
        user.setActif(false);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("pwd123"));
        userRepository.save(user);

        VerificationCode code = new VerificationCode(
                email,
                passwordEncoder.encode("123456"),
                VerificationCodeType.EMAIL_VERIFICATION,
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );
        verificationCodeRepository.save(code);

        // Submit wrong code
        mockMvc.perform(post("/api/auth/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"999999\"}".formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Code incorrect. 4 tentative(s) restante(s)."));

        VerificationCode updatedCode = verificationCodeRepository.findById(code.getId()).orElseThrow();
        assertEquals(1, updatedCode.getAttempts());
        assertFalse(updatedCode.isUsed());
    }

    @Test
    @Transactional
    void expiredOtpIsRejected() throws Exception {
        String email = "expired-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("Expired");
        user.setRole("USER");
        user.setActif(false);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("pwd123"));
        userRepository.save(user);

        // Expired 10 minutes ago
        VerificationCode code = new VerificationCode(
                email,
                passwordEncoder.encode("123456"),
                VerificationCodeType.EMAIL_VERIFICATION,
                Instant.now().minus(10, ChronoUnit.MINUTES)
        );
        verificationCodeRepository.save(code);

        mockMvc.perform(post("/api/auth/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"123456\"}".formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Le code a expiré. Veuillez demander un nouveau code."));
    }

    @Test
    @Transactional
    void resendInvalidatesOldCodeAndEnforcesCooldown() throws Exception {
        String email = "cooldown-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("Cooldown");
        user.setRole("USER");
        user.setActif(false);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("pwd123"));
        userRepository.save(user);

        // First send
        mockMvc.perform(post("/api/auth/verification/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isOk());

        // Immediate resend (< 60s) -> 429 Too Many Requests
        mockMvc.perform(post("/api/auth/verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @Transactional
    void passwordResetCodeCannotBeUsedForEmailVerification() throws Exception {
        String email = "segregation-" + UUID.randomUUID() + "@gmt.test";
        User user = new User();
        user.setEmail(email);
        user.setNom("Segregation");
        user.setRole("USER");
        user.setActif(false);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode("pwd123"));
        userRepository.save(user);

        String plainCode = "888888";
        // Code created for PASSWORD_RESET, not EMAIL_VERIFICATION
        VerificationCode resetCode = new VerificationCode(
                email,
                passwordEncoder.encode(plainCode),
                VerificationCodeType.PASSWORD_RESET,
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );
        verificationCodeRepository.save(resetCode);

        // Try to verify email with password reset code -> should be rejected because no EMAIL_VERIFICATION code exists
        mockMvc.perform(post("/api/auth/verification/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, plainCode)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Aucun code actif trouvé. Veuillez demander un nouveau code."));
    }
}

