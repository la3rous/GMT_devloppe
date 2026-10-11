package com.gmt.rental;

import com.gmt.rental.entity.AdminPreference;
import com.gmt.rental.entity.EmailDelivery;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.repository.AdminPreferenceRepository;
import com.gmt.rental.repository.EmailDeliveryRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.service.EmailService;
import com.gmt.rental.service.QuoteCreatedEvent;
import com.gmt.rental.service.QuoteEmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuoteEmailNotificationServiceTests {
    private AdminPreferenceRepository preferences;
    private QuoteRequestRepository quotes;
    private EmailDeliveryRepository deliveries;
    private EmailService emails;
    private QuoteEmailNotificationService service;
    private AdminPreference preference;
    private QuoteRequest quote;

    @BeforeEach
    void setUp() {
        preferences = mock(AdminPreferenceRepository.class);
        quotes = mock(QuoteRequestRepository.class);
        deliveries = mock(EmailDeliveryRepository.class);
        emails = mock(EmailService.class);
        service = new QuoteEmailNotificationService(preferences, quotes, deliveries, emails);

        preference = new AdminPreference();
        preference.setNotificationEmail("notifications@gmt.test");
        quote = new QuoteRequest();
        quote.setId(41L);
        quote.setNom("Client");
        quote.setPrenom("GMT");
        quote.setEmail("client@gmt.test");
        when(quotes.findByIdWithVehicle(41L)).thenReturn(Optional.of(quote));
        when(preferences.findAllByQuoteNotificationsTrue()).thenReturn(List.of(preference));
        when(deliveries.existsByQuoteRequestIdAndRecipient(41L, "notifications@gmt.test")).thenReturn(false);
        when(deliveries.save(any(EmailDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emails.send(anyString(), anyString(), anyString(), anyString())).thenReturn("resend-message-id");
        when(emails.getAppName()).thenReturn("Gestion Rental");
    }

    @Test
    void sendsAndRecordsAcceptanceForAnEnabledNotification() {
        service.onQuoteCreated(new QuoteCreatedEvent(41L));

        verify(emails).send(eq("notifications@gmt.test"), contains("Nouvelle demande"), contains("#41"), contains("Demande #41"));
        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(deliveries, times(2)).save(captor.capture());
        assertEquals("ACCEPTED", captor.getValue().getStatus());
        assertEquals("resend-message-id", captor.getValue().getProviderMessageId());
    }

    @Test
    void skipsDisabledOrInvalidDestinations() {
        when(preferences.findAllByQuoteNotificationsTrue()).thenReturn(List.of());
        service.onQuoteCreated(new QuoteCreatedEvent(41L));
        verifyNoInteractions(emails);

        when(preferences.findAllByQuoteNotificationsTrue()).thenReturn(List.of(preference));
        preference.setNotificationEmail("invalid");
        service.onQuoteCreated(new QuoteCreatedEvent(41L));
        verifyNoInteractions(emails);
        verify(deliveries, never()).save(any());
    }

    @Test
    void doesNotRepeatAnAlreadyTrackedNotification() {
        when(deliveries.existsByQuoteRequestIdAndRecipient(41L, "notifications@gmt.test")).thenReturn(true);

        service.onQuoteCreated(new QuoteCreatedEvent(41L));

        verifyNoInteractions(emails);
        verify(deliveries, never()).save(any());
    }

    @Test
    void keepsFailedDeliveryTrackedWithoutPropagatingTheProviderError() {
        when(emails.send(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new EmailService.EmailProviderException("refus fournisseur"));

        service.onQuoteCreated(new QuoteCreatedEvent(41L));

        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(deliveries, times(2)).save(captor.capture());
        assertTrue("FAILED".equals(captor.getValue().getStatus())
                && "Échec fournisseur/réseau".equals(captor.getValue().getLastError()));
    }
}
