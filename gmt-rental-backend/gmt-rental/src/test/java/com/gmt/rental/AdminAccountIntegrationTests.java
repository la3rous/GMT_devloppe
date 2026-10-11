package com.gmt.rental;

import com.gmt.rental.entity.User;
import com.gmt.rental.repository.AdminPreferenceRepository;
import com.gmt.rental.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gmt_rental_account_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.mail.host=localhost",
        "spring.mail.username=",
        "spring.mail.password=",
        "gmt.email.resend-api-key=",
        "gmt.email.from=",
        "gmt.support.email=",
        "gmt.jwt.secret=test-only-jwt-signing-key-long-enough-for-hmac"
})
@AutoConfigureMockMvc
class AdminAccountIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private AdminPreferenceRepository preferences;
    @Autowired private PasswordEncoder passwords;

    @Test
    void adminProfilePreferencesPasswordActivityAndSupportAreProtectedAndPersistent() throws Exception {
        String email = "account-admin-" + UUID.randomUUID() + "@gmt.test";
        User admin = new User();
        admin.setNom("Administrateur"); admin.setPrenom("Compte"); admin.setEmail(email);
        admin.setRole("ADMIN"); admin.setActif(true); admin.setEmailVerified(true);
        admin.setMotDePasse(passwords.encode("current-password"));
        users.save(admin);

        mockMvc.perform(get("/api/admin/account/profile"))
                .andExpect(status().isUnauthorized());

        String login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"motDePasse\":\"current-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(login);
        assertTrue(matcher.find(), "La réponse doit contenir un jeton d’accès.");
        String auth = "Bearer " + matcher.group(1);

        mockMvc.perform(get("/api/admin/account/profile").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.motDePasse").doesNotExist());

        mockMvc.perform(put("/api/admin/account/profile").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\" \",\"prenom\":\"Compte\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/admin/account/profile").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"Mis à jour\",\"prenom\":\"Compte\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nom").value("Mis à jour"));
        assertEquals("Mis à jour", users.findByEmail(email).orElseThrow().getNom());

        mockMvc.perform(put("/api/admin/account/preferences").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"theme\":\"light\",\"quoteNotifications\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.quoteNotifications").value(false));
        assertFalse(preferences.findByUserId(admin.getId()).orElseThrow().isQuoteNotifications());
        mockMvc.perform(get("/api/admin/account/preferences").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("light"));

        mockMvc.perform(put("/api/admin/account/password").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"New-password-123\",\"confirmation\":\"New-password-123\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/account/password").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"current-password\",\"newPassword\":\"New-password-123\",\"confirmation\":\"New-password-123\"}"))
                .andExpect(status().isNoContent());
        User changed = users.findById(admin.getId()).orElseThrow();
        assertTrue(passwords.matches("New-password-123", changed.getMotDePasse()));
        assertNotNull(changed.getPasswordUpdatedAt());

        mockMvc.perform(get("/api/admin/activity").param("action", "LOGIN").param("keyword", email).header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].action").value("LOGIN"));
        mockMvc.perform(get("/api/admin/activity/export").param("action", "LOGIN").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Date et heure")));

        mockMvc.perform(post("/api/admin/support").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Compte Admin\",\"email\":\"" + email + "\",\"subject\":\"Question test\",\"category\":\"Technique\",\"description\":\"Description de test suffisamment détaillée.\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        mockMvc.perform(get("/api/admin/support/mine").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("Question test"));
        mockMvc.perform(post("/api/admin/support").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Compte Admin\",\"email\":\"" + email + "\",\"subject\":\"Question test\",\"category\":\"Technique\",\"description\":\"court\"}"))
                .andExpect(status().isBadRequest());
    }
}
