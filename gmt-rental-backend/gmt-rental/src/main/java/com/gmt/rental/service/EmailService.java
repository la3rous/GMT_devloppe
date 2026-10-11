package com.gmt.rental.service;

import com.resend.core.exception.ResendException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final String apiKey;
    private final String fromEmail;
    private final String appName;
    private final JavaMailSender smtpSender;
    private final String smtpFrom;
    private final String smtpUsername;
    private final String smtpPassword;
    private final ResendEmailGateway resendGateway;

    public EmailService(
            @Value("${gmt.email.resend-api-key:}") String apiKey,
            @Value("${gmt.email.from:}") String fromEmail,
            @Value("${gmt.email.app-name:Gestion Rental}") String appName,
            JavaMailSender smtpSender,
            @Value("${gmt.mail.from:${spring.mail.username:}}") String smtpFrom,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword,
            ResendEmailGateway resendGateway) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.appName = appName == null || appName.isBlank() ? "Gestion Rental" : appName.trim();
        this.smtpSender = smtpSender;
        this.smtpFrom = smtpFrom == null ? "" : smtpFrom.trim();
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword.trim();
        this.resendGateway = resendGateway;
    }

    @PostConstruct
    void reportConfiguration() {
        if (!isConfigured()) log.warn("Service Resend non configuré : définir RESEND_API_KEY et EMAIL_FROM avant tout envoi.");
    }

    public boolean isConfigured() { return !apiKey.isBlank() && !fromEmail.isBlank(); }
    public String getAppName() { return appName; }

    public String send(String recipient, String subject, String html, String text) {
        if (!isConfigured()) throw new EmailNotConfiguredException();
        try {
            String messageId = resendGateway.send(apiKey, fromEmail, recipient, subject, html, text);
            if (messageId == null || messageId.isBlank()) {
                throw new EmailProviderException("Le fournisseur n’a pas confirmé l’acceptation de l’e-mail.");
            }
            log.info("Resend a accepté un e-mail transactionnel (id={}).", messageId);
            return messageId;
        } catch (EmailProviderException exception) {
            throw exception;
        } catch (ResendException exception) {
            log.error("Échec Resend (httpStatus={}, errorName={}, errorType={}).",
                    exception.getStatusCode(), safeLogValue(exception.getErrorName()), exception.getClass().getSimpleName());
            throw new EmailProviderException("Resend a refusé ou n’a pas pu accepter l’e-mail.", exception);
        } catch (RuntimeException exception) {
            log.error("Échec d’envoi Resend (type={}).", exception.getClass().getSimpleName());
            throw new EmailProviderException("Resend a refusé ou n’a pas pu accepter l’e-mail.", exception);
        }
    }

    public void sendVerificationOtpEmail(String recipient, String code, int expirationMinutes) {
        sendOtp(recipient, "Votre code de vérification GMT Rental", "Vérification de votre compte", code, expirationMinutes);
    }

    public void sendPasswordResetOtpEmail(String recipient, String code, int expirationMinutes) {
        sendOtp(recipient, "Réinitialisation de votre mot de passe GMT Rental", "Réinitialisation du mot de passe", code, expirationMinutes);
    }

    public void sendEmailChangeOtpEmail(String recipient, String code, int expirationMinutes) {
        sendOtp(recipient, "Confirmez votre nouvelle adresse GMT Rental", "Confirmation de votre adresse e-mail", code, expirationMinutes);
    }

    private void sendOtp(String recipient, String subject, String title, String code, int expirationMinutes) {
        String safeCode = escape(code);
        String html = "<div style='font-family:Arial,sans-serif;background:#111a2d;color:#edf2fa;padding:28px;border-radius:12px'>"
                + "<p style='color:#d8ad63;font-weight:bold'>" + escape(appName) + "</p><h2>" + title + "</h2>"
                + "<p>Votre code de confirmation est :</p><strong style='font-size:28px;color:#d8ad63;letter-spacing:6px'>" + safeCode + "</strong>"
                + "<p>Ce code expire dans " + expirationMinutes + " minutes. Ne le partagez avec personne.</p></div>";
        String text = appName + " — " + title + ". Votre code est " + code + ". Il expire dans " + expirationMinutes + " minutes.";
        if (isConfigured()) {
            send(recipient, subject, html, text);
            return;
        }
        // Keep the existing account verification/reset flow operational for deployments still using SMTP.
        if (smtpUsername.isBlank() || smtpPassword.isBlank() || smtpFrom.isBlank()) {
            throw new EmailNotConfiguredException();
        }
        try {
            MimeMessage message = smtpSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(smtpFrom); helper.setTo(recipient.trim()); helper.setSubject(subject); helper.setText(text, html);
            smtpSender.send(message);
        } catch (MessagingException | RuntimeException exception) {
            log.error("Envoi SMTP de l’e-mail de compte en échec (type={}).", exception.getClass().getSimpleName());
            throw new RuntimeException("Impossible d’envoyer l’e-mail de compte. Configurez Resend ou le SMTP.", exception);
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) return "";
        String safe = value.replace(apiKey, "[REDACTED]")
                .replaceAll("(?i)re_[a-z0-9_-]{12,}", "[REDACTED]")
                .replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer [REDACTED]")
                .replaceAll("[\\r\\n\\t]", " ");
        return safe.length() > 500 ? safe.substring(0, 500) + "…" : safe;
    }

    public static class EmailNotConfiguredException extends RuntimeException {
        public EmailNotConfiguredException() { super("Le service e-mail n’est pas configuré. Définissez RESEND_API_KEY et EMAIL_FROM sur le backend."); }
    }
    public static class EmailProviderException extends RuntimeException {
        public EmailProviderException(String message) { super(message); }
        public EmailProviderException(String message, Throwable cause) { super(message, cause); }
    }
}
