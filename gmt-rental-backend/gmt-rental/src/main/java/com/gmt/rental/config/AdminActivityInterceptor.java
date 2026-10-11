package com.gmt.rental.config;

import com.gmt.rental.service.admin.AdminActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class AdminActivityInterceptor implements HandlerInterceptor, WebMvcConfigurer {
    private final AdminActivityService activities;
    public AdminActivityInterceptor(AdminActivityService activities) { this.activities = activities; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(this).addPathPatterns("/api/admin/**"); }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        String method = request.getMethod();
        if (!(method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE"))) return;
        String path = request.getRequestURI();
        if (path.endsWith("/logout")) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "" : authentication.getName();
        String[] parts = path.split("/");
        String module = parts.length > 3 ? parts[3] : "administration";
        if (path.endsWith("/account/password")) module = "security";
        String target = parts.length > 4 && parts[4].matches("\\d+") ? module + " #" + parts[4] : "";
        String action = path.endsWith("/account/password") ? "PASSWORD_CHANGE" : switch (method) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "ACTION";
        };
        boolean success = exception == null && response.getStatus() < 400;
        String moduleLabel = switch (module) {
            case "vehicules" -> "véhicule";
            case "categories" -> "catégorie";
            case "demandes" -> "demande de devis";
            case "users" -> "compte utilisateur";
            case "settings" -> "paramètres";
            case "account" -> "compte administrateur";
            case "support" -> "demande d’assistance";
            case "notifications" -> "notification";
            default -> "administration";
        };
        String verb = switch (method) {
            case "POST" -> "Création";
            case "PUT" -> "Modification";
            case "PATCH" -> "Mise à jour";
            case "DELETE" -> "Suppression";
            default -> "Action";
        };
        String description = path.endsWith("/account/password")
                ? (success ? "Changement du mot de passe administrateur" : "Échec du changement de mot de passe administrateur")
                : (success ? verb : "Échec de " + verb.toLowerCase()) + " — " + moduleLabel;
        if (!target.isBlank()) description += " " + target.substring(target.indexOf('#'));
        try {
            activities.record(actor, target, action, module, description, success);
        } catch (RuntimeException ignored) {
            // Auditing must not change the result of the business operation.
        }
    }
}
