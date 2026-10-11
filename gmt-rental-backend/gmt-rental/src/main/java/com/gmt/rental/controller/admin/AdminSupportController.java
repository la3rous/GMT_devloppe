package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.admin.SupportRequestDto;
import com.gmt.rental.entity.SupportRequest;
import com.gmt.rental.repository.SupportRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.gmt.rental.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController {
    private static final Logger log = LoggerFactory.getLogger(AdminSupportController.class);
    private static final Set<String> CATEGORIES = Set.of("Compte", "Véhicule", "Devis", "Technique", "Autre");
    private final SupportRequestRepository requests;
    private final String supportEmail;
    private final String applicationVersion;
    private final String appName;
    private final EmailService emails;

    public AdminSupportController(SupportRequestRepository requests,
            @Value("${gmt.support.email:}") String supportEmail,
            @Value("${app.version:}") String applicationVersion,
            @Value("${gmt.email.app-name:Gestion Rental}") String appName,
            EmailService emails) {
        this.requests = requests; this.supportEmail = supportEmail; this.applicationVersion = applicationVersion;
        this.appName = appName == null || appName.isBlank() ? "Gestion Rental" : appName.trim(); this.emails = emails;
    }

    @GetMapping("/info") public Map<String, String> info() {
        return Map.of("supportEmail", supportEmail == null ? "" : supportEmail, "version", applicationVersion == null ? "" : applicationVersion);
    }

    @GetMapping("/mine") public List<SupportRequest> mine(Authentication auth) {
        return requests.findTop20BySenderEmailOrderByCreatedAtDesc(auth.getName().trim().toLowerCase());
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(Authentication auth, @RequestBody SupportRequestDto body) {
        if (body == null || blank(body.name()) || blank(body.email()) || blank(body.subject()) || blank(body.category()) || blank(body.description()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les champs sont obligatoires.");
        if (body.name().trim().length() > 160 || body.subject().trim().length() > 180 || body.description().trim().length() < 20 || body.description().trim().length() > 5000
                || !body.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vérifiez les champs du formulaire (description : 20 à 5000 caractères).");
        if (!CATEGORIES.contains(body.category())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie invalide.");
        String actor = auth.getName().trim().toLowerCase();
        if (requests.countBySenderEmailAndCreatedAtAfter(actor, LocalDateTime.now().minusHours(1)) >= 5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite atteinte : réessayez dans une heure.");
        SupportRequest request = new SupportRequest();
        request.setSenderName(body.name().trim()); request.setSenderEmail(actor);
        request.setSubject(body.subject().trim()); request.setCategory(body.category()); request.setDescription(body.description().trim());
        SupportRequest saved = requests.save(request);
        String emailStatus = "NOT_CONFIGURED";
        String message = "Demande enregistrée, mais aucun destinataire d’assistance n’est configuré.";
        if (supportEmail != null && supportEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            try {
                String html = "<div style='font-family:Arial,sans-serif;background:#111a2d;color:#edf2fa;padding:24px;border-radius:12px'>"
                        + "<p style='color:#d8ad63;font-weight:bold'>" + escape(appName) + "</p><h1>Nouvelle demande d’assistance #" + saved.getId() + "</h1>"
                        + "<p><strong>Catégorie :</strong> " + escape(saved.getCategory()) + "</p>"
                        + "<p><strong>Sujet :</strong> " + escape(saved.getSubject()) + "</p>"
                        + "<p><strong>Nom :</strong> " + escape(saved.getSenderName()) + "</p>"
                        + "<p><strong>E-mail :</strong> " + escape(saved.getSenderEmail()) + "</p>"
                        + "<p><strong>Date :</strong> " + escape(String.valueOf(saved.getCreatedAt())) + "</p>"
                        + "<p><strong>Description :</strong></p><p style='white-space:pre-wrap'>" + escape(saved.getDescription()) + "</p></div>";
                String text = "Nouvelle demande d’assistance #" + saved.getId() + " — " + appName + "\n"
                        + "Catégorie : " + saved.getCategory() + "\nSujet : " + saved.getSubject()
                        + "\nNom : " + saved.getSenderName() + "\nE-mail : " + saved.getSenderEmail()
                        + "\nDate : " + saved.getCreatedAt() + "\n\n" + saved.getDescription();
                emails.send(supportEmail.trim(), "Nouvelle demande d’assistance — " + appName, html, text);
                emailStatus = "ACCEPTED";
                message = "Demande enregistrée. Resend a accepté l’e-mail de notification.";
            } catch (Exception exception) {
                emailStatus = exception instanceof EmailService.EmailNotConfiguredException ? "NOT_CONFIGURED" : "FAILED";
                message = emailStatus.equals("NOT_CONFIGURED")
                        ? "Demande enregistrée, mais le service d’e-mail n’est pas configuré."
                        : "Demande enregistrée, mais l’e-mail n’a pas pu être envoyé. Vérifiez la configuration Resend.";
                log.error("Notification e-mail de la demande d’assistance {} en échec (type={}).", saved.getId(), exception.getClass().getSimpleName());
            }
        } else if (supportEmail != null && !supportEmail.isBlank()) {
            emailStatus = "INVALID_RECIPIENT";
            message = "Demande enregistrée, mais l’adresse de réception d’assistance configurée est invalide.";
        }
        return Map.of("id", saved.getId(), "createdAt", saved.getCreatedAt(), "emailStatus", emailStatus, "message", message);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
