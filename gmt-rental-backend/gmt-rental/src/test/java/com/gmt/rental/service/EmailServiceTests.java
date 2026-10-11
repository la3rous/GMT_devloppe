package com.gmt.rental.service;

import com.resend.core.exception.ResendException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailServiceTests {
    private JavaMailSender smtp;
    private ResendEmailGateway resend;

    @BeforeEach
    void setUp() {
        smtp = mock(JavaMailSender.class);
        resend = mock(ResendEmailGateway.class);
        when(smtp.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void reportsSuccessOnlyAfterResendReturnsMessageId() throws Exception {
        EmailService service = service("re_test_key", "Gestion Rental <notify@gmt.test>", "", "");
        when(resend.send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("email-id-123");

        assertEquals("email-id-123", service.send("admin@gmt.test", "Test", "<p>Hi</p>", "Hi"));
        verifyNoInteractions(smtp);
    }

    @Test
    void resendFailureDoesNotSendAgainThroughSmtp() throws Exception {
        EmailService service = service("re_test_key", "Gestion Rental <notify@gmt.test>", "smtp-user", "smtp-pass");
        when(resend.send(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new ResendException(403, "{\"name\":\"validation_error\",\"message\":\"domain is not verified\"}"));

        EmailService.EmailProviderException error = assertThrows(EmailService.EmailProviderException.class,
                () -> service.send("admin@gmt.test", "Test", "<p>Hi</p>", "Hi"));
        assertInstanceOf(ResendException.class, error.getCause());
        verifyNoInteractions(smtp);
    }

    @Test
    void usesSmtpOnlyWhenResendIsAbsentAndSmtpCredentialsAreConfigured() {
        EmailService service = service("", "", "smtp-user", "smtp-pass");

        assertDoesNotThrow(() -> service.sendVerificationOtpEmail("user@gmt.test", "123456", 10));
        verify(smtp, times(1)).send(any(MimeMessage.class));
        verifyNoInteractions(resend);
    }

    @Test
    void doesNotAttemptSmtpWhenResendAndSmtpAreNotConfigured() {
        EmailService service = service("", "", "", "");

        assertThrows(EmailService.EmailNotConfiguredException.class,
                () -> service.sendVerificationOtpEmail("user@gmt.test", "123456", 10));
        verifyNoInteractions(smtp, resend);
    }

    private EmailService service(String key, String from, String smtpUser, String smtpPassword) {
        return new EmailService(key, from, "Gestion Rental", smtp, "sender@gmt.test", smtpUser, smtpPassword, resend);
    }
}
