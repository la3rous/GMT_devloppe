package com.gmt.rental;

import com.gmt.rental.entity.AdminPreference;
import com.gmt.rental.entity.Category;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.AdminPreferenceRepository;
import com.gmt.rental.repository.CategoryRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import com.gmt.rental.security.DatabaseUserDetailsService;
import com.gmt.rental.service.admin.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gmt_user_deactivation_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "gmt.dashboard.demo-data=false",
        "gmt.jwt.secret=user-deactivation-test-jwt-signing-key-long-enough"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminUserDeactivationIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private AdminPreferenceRepository preferences;
    @Autowired private CategoryRepository categories;
    @Autowired private VehicleRepository vehicles;
    @Autowired private QuoteRequestRepository quotes;
    @Autowired private RentalHistoryRepository rentals;
    @Autowired private AdminUserService adminUsers;
    @Autowired private DatabaseUserDetailsService userDetailsService;
    @Autowired private PasswordEncoder passwords;

    @Test
    @Transactional
    void deleteEndpointDeactivatesIdempotentlyAndPreservesRelatedRecords() throws Exception {
        User actor = createUser("Actor", "ADMIN", true, true);
        User customer = createUser("Customer", "USER", true, true);
        Vehicle vehicle = createVehicle();

        QuoteRequest quote = new QuoteRequest();
        quote.setUser(customer);
        quote.setVehicule(vehicle);
        quote.setNom(customer.getNom());
        quote.setPrenom(customer.getPrenom());
        quote.setEmail(customer.getEmail());
        quote.setDateDebut(LocalDate.now().plusDays(3));
        quote.setDateFin(LocalDate.now().plusDays(6));
        quote = quotes.saveAndFlush(quote);
        Long quoteId = quote.getId();

        RentalHistory rental = new RentalHistory();
        rental.setUser(customer);
        rental.setVehicle("GMT Berline");
        rental.setVehicleEntity(vehicle);
        rental.setDateDebut(LocalDate.now().minusDays(6));
        rental.setDateFin(LocalDate.now().minusDays(3));
        rental.setPrixPaye(new BigDecimal("450"));
        rental.setDevise("TND");
        Long rentalId = rentals.saveAndFlush(rental).getId();

        AdminPreference preference = new AdminPreference();
        preference.setUser(customer);
        preference.setTheme("dark");
        Long preferenceId = preferences.saveAndFlush(preference).getId();

        String actorToken = login(actor);
        mockMvc.perform(delete("/api/admin/users/{id}", customer.getId()).header("Authorization", actorToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/users/{id}", customer.getId()).header("Authorization", actorToken))
                .andExpect(status().isNoContent());

        User retained = users.findById(customer.getId()).orElseThrow();
        assertFalse(retained.getActif());
        assertEquals(customer.getId(), quotes.findById(quoteId).orElseThrow().getUser().getId());
        assertEquals(customer.getId(), rentals.findById(rentalId).orElseThrow().getUser().getId());
        assertEquals(customer.getId(), preferences.findById(preferenceId).orElseThrow().getUser().getId());
        mockMvc.perform(get("/api/admin/users").header("Authorization", actorToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(customer.getEmail())));
        mockMvc.perform(delete("/api/admin/users/{id}", 999999L).header("Authorization", actorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void disabledAdminCannotLoginOrUseAnAlreadyIssuedTokenAndActiveAdminStillCan() throws Exception {
        User operator = createUser("Operator", "ADMIN", true, true);
        User targetAdmin = createUser("Target", "ADMIN", true, true);
        String operatorToken = login(operator);
        String oldTargetToken = login(targetAdmin);

        mockMvc.perform(delete("/api/admin/users/{id}", targetAdmin.getId()).header("Authorization", operatorToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/users").header("Authorization", oldTargetToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"motDePasse\":\"test-password\"}".formatted(targetAdmin.getEmail())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", operatorToken))
                .andExpect(status().isOk());

        UserDetails disabledPrincipal = userDetailsService.loadUserByUsername(targetAdmin.getEmail());
        assertFalse(disabledPrincipal.isEnabled());
    }

    @Test
    @Transactional
    void accountCanBeReactivatedAndMissingAccountsReturnNotFound() throws Exception {
        User actor = createUser("Operator", "ADMIN", true, true);
        User customer = createUser("Inactive", "USER", false, true);
        String actorToken = login(actor);

        mockMvc.perform(patch("/api/admin/users/{id}/actif", customer.getId())
                        .header("Authorization", actorToken).param("actif", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));
        assertTrue(users.findById(customer.getId()).orElseThrow().getActif());
        mockMvc.perform(patch("/api/admin/users/{id}/actif", 999999L)
                        .header("Authorization", actorToken).param("actif", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void selfAndLastActiveAdminCannotBeDeactivatedThroughEitherAdminPath() throws Exception {
        User actor = createUser("OnlyAdmin", "ADMIN", true, true);
        String actorToken = login(actor);
        String updateBody = "{\"nom\":\"OnlyAdmin\",\"prenom\":\"Test\",\"email\":\"renamed-%s\",\"role\":\"ADMIN\",\"actif\":false}"
                .formatted(actor.getEmail());

        mockMvc.perform(delete("/api/admin/users/{id}", actor.getId()).header("Authorization", actorToken))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/admin/users/{id}", actor.getId()).header("Authorization", actorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isConflict());

        users.findAll().stream()
                .filter(this::isAdmin)
                .filter(admin -> !admin.getId().equals(actor.getId()))
                .forEach(admin -> admin.setActif(false));
        users.flush();
        ResponseStatusException lastAdminError = assertThrows(ResponseStatusException.class,
                () -> adminUsers.deactivate(actor.getId()));
        assertEquals(409, lastAdminError.getStatusCode().value());
        assertTrue(users.findById(actor.getId()).orElseThrow().getActif());
    }

    private User createUser(String prefix, String role, boolean active, boolean verified) {
        User user = new User();
        user.setNom(prefix);
        user.setPrenom("Test");
        user.setEmail(prefix.toLowerCase() + "-" + UUID.randomUUID() + "@gmt.test");
        user.setRole(role);
        user.setActif(active);
        user.setEmailVerified(verified);
        user.setMotDePasse(passwords.encode("test-password"));
        return users.saveAndFlush(user);
    }

    private Vehicle createVehicle() {
        Category category = new Category();
        category.setNom("Test category " + UUID.randomUUID());
        category = categories.saveAndFlush(category);
        Vehicle vehicle = new Vehicle();
        vehicle.setMarque("GMT");
        vehicle.setModele("Berline");
        vehicle.setStatut("ACTIF");
        vehicle.setCategorie(category);
        return vehicles.saveAndFlush(vehicle);
    }

    private String login(User user) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"motDePasse\":\"test-password\"}".formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        assertTrue(matcher.find(), "La réponse de connexion doit contenir le JWT");
        return "Bearer " + matcher.group(1);
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && ("ADMIN".equalsIgnoreCase(user.getRole())
                || "ROLE_ADMIN".equalsIgnoreCase(user.getRole()));
    }
}
