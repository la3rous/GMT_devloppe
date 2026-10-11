package com.gmt.rental.service;

import com.gmt.rental.entity.AdminPreference;
import com.gmt.rental.entity.EmailDelivery;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.repository.AdminPreferenceRepository;
import com.gmt.rental.repository.EmailDeliveryRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class QuoteEmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(QuoteEmailNotificationService.class);
    private final AdminPreferenceRepository preferences;
    private final QuoteRequestRepository quotes;
    private final EmailDeliveryRepository deliveries;
    private final EmailService emails;

    public QuoteEmailNotificationService(AdminPreferenceRepository preferences, QuoteRequestRepository quotes,
                                         EmailDeliveryRepository deliveries, EmailService emails) {
        this.preferences = preferences; this.quotes = quotes; this.deliveries = deliveries; this.emails = emails;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onQuoteCreated(QuoteCreatedEvent event) {
        try {
            process(event);
        } catch (Exception exception) {
            // The quote has already committed: email infrastructure must never turn it into a failed request.
            log.error("Traitement de notification du devis {} en échec (type={}).", event.quoteRequestId(), exception.getClass().getSimpleName());
        }
    }

    private void process(QuoteCreatedEvent event) {
        QuoteRequest quote = quotes.findByIdWithVehicle(event.quoteRequestId()).orElse(null);
        if (quote == null) return;
        for (AdminPreference preference : preferences.findAllByQuoteNotificationsTrue()) {
            String recipient = preference.getNotificationEmail();
            if (recipient == null || !validEmail(recipient)) continue;
            recipient = recipient.trim().toLowerCase(Locale.ROOT);
            if (deliveries.existsByQuoteRequestIdAndRecipient(quote.getId(), recipient)) continue;

            EmailDelivery delivery = new EmailDelivery();
            delivery.setQuoteRequestId(quote.getId()); delivery.setRecipient(recipient); delivery.setStatus("PENDING");
            delivery = deliveries.save(delivery);
            try {
                // Persist the unique delivery claim before contacting Resend so concurrent events cannot both send.
                deliveries.saveAndFlush(delivery);
                String appName = emails.getAppName();
                if (appName == null || appName.isBlank()) appName = "Gestion Rental";
                String id = emails.send(recipient, "Nouvelle demande de devis — " + appName,
                        buildHtml(quote, appName), buildText(quote, appName));
                delivery.setStatus("ACCEPTED"); delivery.setProviderMessageId(id); delivery.setLastError(null);
            } catch (Exception exception) {
                delivery.setStatus("FAILED");
                delivery.setLastError(exception instanceof EmailService.EmailNotConfiguredException
                        ? "Service Resend non configuré" : "Échec fournisseur/réseau");
                log.error("Notification de devis {} en échec (type={}).", quote.getId(), exception.getClass().getSimpleName());
            }
            deliveries.save(delivery);
        }
    }

    private String buildHtml(QuoteRequest q, String appName) {
        StringBuilder rows = new StringBuilder();
        row(rows, "Demande", "#" + q.getId());
        row(rows, "Reçue le", q.getDateCreation() == null ? null : q.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        row(rows, "Client", value(q.getPrenom()) + " " + value(q.getNom()));
        row(rows, "E-mail", q.getEmail()); row(rows, "Téléphone", q.getTelephone());
        row(rows, "Véhicule", q.getVehicule() == null ? null : q.getVehicule().getMarque() + " " + q.getVehicule().getModele());
        row(rows, "Début", q.getDateDebut() == null ? null : q.getDateDebut().toString());
        row(rows, "Fin", q.getDateFin() == null ? null : q.getDateFin().toString());
        row(rows, "Prise en charge", q.getLieuPriseEnCharge()); row(rows, "Restitution", q.getLieuRestitution());
        row(rows, "Message", q.getMessage());
        return "<!doctype html><html lang='fr'><meta charset='utf-8'><body style='margin:0;background:#0c1424;font-family:Arial,sans-serif;color:#edf2fa;padding:24px'>"
                + "<main style='max-width:620px;margin:auto;background:#111a2d;border:1px solid #25334d;border-radius:14px;padding:24px'>"
                + "<p style='color:#d8ad63;font-weight:bold'>" + escape(appName) + "</p><h1 style='font-size:22px'>Nouvelle demande de devis reçue</h1>"
                + "<table style='width:100%;border-collapse:collapse'>" + rows + "</table></main></body></html>";
    }

    private String buildText(QuoteRequest q, String appName) {
        return appName + " — Nouvelle demande de devis reçue\nDemande #" + q.getId()
                + "\nClient : " + value(q.getPrenom()) + " " + value(q.getNom())
                + "\nE-mail : " + value(q.getEmail()) + "\nTéléphone : " + value(q.getTelephone())
                + "\nVéhicule : " + (q.getVehicule() == null ? "" : value(q.getVehicule().getMarque()) + " " + value(q.getVehicule().getModele()))
                + "\nDates : " + value(q.getDateDebut()) + " au " + value(q.getDateFin())
                + "\nPrise en charge : " + value(q.getLieuPriseEnCharge()) + "\nRestitution : " + value(q.getLieuRestitution())
                + "\nMessage : " + value(q.getMessage());
    }

    private static void row(StringBuilder html, String label, Object raw) {
        String value = raw == null ? "" : raw.toString().trim(); if (value.isEmpty()) return;
        html.append("<tr><th align='left' style='padding:10px 8px;border-bottom:1px solid #25334d;color:#9aa8bf'>")
                .append(escape(label)).append("</th><td style='padding:10px 8px;border-bottom:1px solid #25334d'>")
                .append(escape(value)).append("</td></tr>");
    }
    private static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private static String value(Object v) { return v == null ? "" : v.toString(); }
    public static boolean validEmail(String email) { return email != null && email.length() <= 254 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }
}
