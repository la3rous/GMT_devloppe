package com.gmt.rental;

import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.entity.Category;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.repository.CategoryRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.NotificationRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.service.admin.AdminCategoryService;
import com.gmt.rental.service.admin.AdminQuoteRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gmt_rental_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class AdminApiIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private QuoteRequestRepository quoteRequestRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private RentalHistoryRepository rentalHistoryRepository;
    @Autowired private AdminCategoryService adminCategoryService;
    @Autowired private AdminQuoteRequestService adminQuoteRequestService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void adminCanCreateAndUpdateUsersWithoutStoringPlaintextPasswords() throws Exception {
        String adminEmail = "user-admin-" + UUID.randomUUID() + "@gmt.test";
        User admin = new User();
        admin.setNom("Admin");
        admin.setEmail(adminEmail);
        admin.setRole("ADMIN");
        admin.setActif(true);
        admin.setEmailVerified(true);
        admin.setMotDePasse(passwordEncoder.encode("admin-test-password"));
        userRepository.save(admin);

        String loginBody = """
                {"email":"%s","motDePasse":"admin-test-password"}
                """.formatted(adminEmail);
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(loginResponse);
        if (!matcher.find()) throw new AssertionError("La réponse de connexion ne contient pas de jeton JWT");
        String auth = "Bearer " + matcher.group(1);

        String userEmail = "managed-user-" + UUID.randomUUID() + "@gmt.test";
        String createUserBody = """
                {"nom":"User","prenom":"Test","email":"%s","motDePasse":"user-test-password","role":"USER","actif":true}
                """.formatted(userEmail);
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(userEmail))
                .andExpect(jsonPath("$.role").value("USER"));

        User savedUser = userRepository.findByEmailIgnoreCase(userEmail).orElseThrow();
        Long userId = savedUser.getId();
        String originalHash = savedUser.getMotDePasse();
        assertNotEquals("user-test-password", originalHash);
        assertTrue(passwordEncoder.matches("user-test-password", originalHash));
        mockMvc.perform(get("/api/admin/users").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(userEmail)));

        String updateBody = """
                {"nom":"User Modified","prenom":"Test","email":"%s","role":"USER","actif":false}
                """.formatted(userEmail);
        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("User Modified"))
                .andExpect(jsonPath("$.actif").value(false));
        savedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(originalHash, savedUser.getMotDePasse(), "Un mot de passe vide doit conserver le hash existant");
        assertFalse(savedUser.getActif());

        String passwordUpdateBody = """
                {"nom":"User Modified","prenom":"Test","email":"%s","motDePasse":"new-test-password","role":"USER","actif":true}
                """.formatted(userEmail);
        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordUpdateBody))
                .andExpect(status().isOk());
        String updatedHash = userRepository.findById(userId).orElseThrow().getMotDePasse();
        assertNotEquals("new-test-password", updatedHash);
        assertTrue(passwordEncoder.matches("new-test-password", updatedHash));

        String adminUserEmail = "managed-admin-" + UUID.randomUUID() + "@gmt.test";
        String createAdminBody = """
                {"nom":"Admin","prenom":"Test","email":"%s","motDePasse":"new-admin-password","role":"ADMIN","actif":true}
                """.formatted(adminUserEmail);
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAdminBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        User savedAdmin = userRepository.findByEmailIgnoreCase(adminUserEmail).orElseThrow();
        assertTrue(passwordEncoder.matches("new-admin-password", savedAdmin.getMotDePasse()));

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Invalid\",\"prenom\":\"User\",\"email\":\"invalid\",\"motDePasse\":\"secret\",\"role\":\"USER\",\"actif\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/users/{id}", 999999)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users").header("Authorization", auth)).andExpect(status().isOk());
    }

    @Test
    @Transactional
    void adminCategoriesAreReturnedNewestFirst() throws Exception {
        for (String name : List.of("Test catégorie A", "Test catégorie B", "Test catégorie C")) {
            Category category = new Category();
            category.setNom(name);
            categoryRepository.save(category);
        }

        List<String> firstThree = adminCategoryService.findAll().stream()
                .map(Category::getNom)
                .limit(3)
                .toList();

        assertEquals(3, firstThree.size());
        assertEquals(List.of("Test catégorie C", "Test catégorie B", "Test catégorie A"), firstThree);
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Test catégorie C"))
                .andExpect(jsonPath("$[1].nom").value("Test catégorie B"))
                .andExpect(jsonPath("$[2].nom").value("Test catégorie A"));
    }

    @Test
    @Transactional
    void adminRoutesRequireJwtAndPublicQuoteContractStillWorks() throws Exception {
        mockMvc.perform(get("/api/admin/vehicules")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(options("/api/parametres")
                        .header("Origin", "http://127.0.0.1:5174")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5174"));
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/parametres")).andExpect(status().isOk());

        Vehicle vehicle = new Vehicle();
        vehicle.setMarque("GMT");
        vehicle.setModele("Test");
        vehicle.setPlaces(5);
        vehicle.setPrixParJour(new BigDecimal("200"));
        vehicle.setStatut("ACTIF");
        Category category = new Category();
        category.setNom("Test " + UUID.randomUUID());
        category = categoryRepository.save(category);
        vehicle.setCategorie(category);
        vehicle = vehicleRepository.save(vehicle);
        mockMvc.perform(get("/api/vehicules")
                        .param("statut", "ACTIF")
                        .param("categorieId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"modele\":\"Test\"")));

        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(5);
        String quoteEmail = "sami-" + UUID.randomUUID() + "@email.com";
        String quoteBody = """
                {"nom":"Ben Ali","prenom":"Sami","email":"%s","telephone":"+216 22 333 444","vehiculeId":%d,"dateDebut":"%s","dateFin":"%s","lieuPriseEnCharge":"Aéroport Tunis-Carthage","lieuRestitution":"Agence GMT Sousse","message":"Test contrat public"}
                """.formatted(quoteEmail, vehicle.getId(), start, end);
        mockMvc.perform(post("/api/public/demandes").contentType(MediaType.APPLICATION_JSON).content(quoteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1000")));
        var savedQuote = quoteRequestRepository.findAll().getFirst();
        User quoteUser = userRepository.findByEmailIgnoreCase(quoteEmail).orElseThrow();
        if (quoteRequestRepository.findAll().size() != 1
                || !"NOUVELLE".equals(savedQuote.getStatut())
                || savedQuote.getUser() == null
                || !savedQuote.getUser().getId().equals(quoteUser.getId())
                || !"USER".equals(quoteUser.getRole())
                || !quoteUser.getEmail().equals(quoteEmail)) {
            throw new AssertionError("La demande publique doit être persistée avec le statut NOUVELLE");
        }

        DateTimeFormatter displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        mockMvc.perform(get("/api/public/demandes/disponibilite")
                        .param("vehiculeId", vehicle.getId().toString())
                        .param("dateDebut", start.toString())
                        .param("dateFin", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.conflictingPeriods").isEmpty());

        mockMvc.perform(post("/api/public/demandes").contentType(MediaType.APPLICATION_JSON).content(quoteBody))
                .andExpect(status().isCreated());

        savedQuote = quoteRequestRepository.findById(savedQuote.getId()).orElseThrow();
        adminQuoteRequestService.updateStatus(savedQuote.getId(), "ACCEPTEE");
        mockMvc.perform(get("/api/public/demandes/disponibilite")
                        .param("vehiculeId", vehicle.getId().toString())
                        .param("dateDebut", start.toString())
                        .param("dateFin", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.conflictingPeriods[0].dateDebut").value(start.toString()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        start.format(displayDate) + " to " + end.format(displayDate))));

        savedQuote.setStatut("ANNULEE");
        quoteRequestRepository.save(savedQuote);
        mockMvc.perform(get("/api/public/demandes/disponibilite")
                        .param("vehiculeId", vehicle.getId().toString())
                        .param("dateDebut", start.toString())
                        .param("dateFin", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
        savedQuote.setStatut("NOUVELLE");
        quoteRequestRepository.save(savedQuote);

        RentalHistory rental = new RentalHistory();
        rental.setUser(quoteUser);
        rental.setVehicle("GMT Test");
        rental.setVehicleEntity(vehicle);
        rental.setDateDebut(end.plusDays(2));
        rental.setDateFin(end.plusDays(4));
        rental.setPrixPaye(new BigDecimal("400"));
        rental.setDevise("DT");
        rentalHistoryRepository.save(rental);

        mockMvc.perform(get("/api/public/demandes/disponibilite")
                        .param("vehiculeId", vehicle.getId().toString())
                        .param("dateDebut", start.plusDays(1).toString())
                        .param("dateFin", end.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictingPeriods.length()").value(1))
                .andExpect(jsonPath("$.conflictingPeriods[0].dateDebut").value(end.plusDays(2).toString()))
                .andExpect(jsonPath("$.conflictingPeriods[0].dateFin").value(end.plusDays(4).toString()));

        RentalHistory legacyRental = new RentalHistory();
        legacyRental.setUser(quoteUser);
        legacyRental.setVehicle("GMT Test");
        legacyRental.setDateDebut(end.plusDays(5));
        legacyRental.setDateFin(end.plusDays(6));
        legacyRental.setPrixPaye(new BigDecimal("200"));
        legacyRental.setDevise("DT");
        rentalHistoryRepository.save(legacyRental);
        mockMvc.perform(get("/api/public/demandes/disponibilite")
                        .param("vehiculeId", vehicle.getId().toString())
                        .param("dateDebut", end.plusDays(5).toString())
                        .param("dateFin", end.plusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.conflictingPeriods[0].dateDebut").value(end.plusDays(5).toString()));

        String adjacentQuoteBody = quoteBody
                .replace("\"dateDebut\":\"" + start + "\"", "\"dateDebut\":\"" + end + "\"")
                .replace("\"dateFin\":\"" + end + "\"", "\"dateFin\":\"" + end.plusDays(2) + "\"");
        mockMvc.perform(post("/api/public/demandes").contentType(MediaType.APPLICATION_JSON).content(adjacentQuoteBody))
                .andExpect(status().isCreated());
        if (userRepository.findAll().stream().filter(user -> user.getEmail().equalsIgnoreCase(quoteEmail)).count() != 1) {
            throw new AssertionError("Une nouvelle demande du même email doit réutiliser le compte existant");
        }

        String missingVehicleBody = quoteBody.replace("\"vehiculeId\":" + vehicle.getId(), "\"vehiculeId\":999999");
        mockMvc.perform(post("/api/public/demandes").contentType(MediaType.APPLICATION_JSON).content(missingVehicleBody))
                .andExpect(status().isNotFound());

        String adminEmail = "admin-" + UUID.randomUUID() + "@gmt.test";
        User admin = new User();
        admin.setNom("Test");
        admin.setEmail(adminEmail);
        admin.setRole("ADMIN");
        admin.setActif(true);
        admin.setEmailVerified(true);
        admin.setMotDePasse(passwordEncoder.encode("test-password"));
        userRepository.save(admin);

        String loginBody = """
                {"email":"%s","motDePasse":"test-password"}
                """.formatted(adminEmail);
        String loginResponse = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(loginResponse);
        if (!matcher.find()) throw new AssertionError("La réponse de connexion ne contient pas de jeton JWT");

        String token = matcher.group(1);
        Long acceptedQuoteId = savedQuote.getId();
        QuoteRequest secondOverlappingQuote = quoteRequestRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(acceptedQuoteId))
                .filter(candidate -> start.equals(candidate.getDateDebut()) && end.equals(candidate.getDateFin()))
                .findFirst().orElseThrow();
        mockMvc.perform(patch("/api/admin/demandes/{id}/statut", savedQuote.getId())
                        .header("Authorization", "Bearer " + token).param("statut", "ACCEPTEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACCEPTEE"));
        mockMvc.perform(patch("/api/admin/demandes/{id}/statut", secondOverlappingQuote.getId())
                        .header("Authorization", "Bearer " + token).param("statut", "ACCEPTEE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.available").value(false));
        assertEquals("NOUVELLE", quoteRequestRepository.findById(secondOverlappingQuote.getId()).orElseThrow().getStatut());

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(quoteEmail)));

        mockMvc.perform(get("/api/admin/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].lu").value(false))
                .andExpect(jsonPath("$[0].devisId").isNumber())
                .andExpect(jsonPath("$[0].vehicule").value("GMT Test"));
        mockMvc.perform(get("/api/admin/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        Long notificationId = notificationRepository.findAll().getFirst().getId();
        mockMvc.perform(patch("/api/admin/notifications/{id}/read", notificationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lu").value(true));
        mockMvc.perform(get("/api/admin/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
        mockMvc.perform(patch("/api/admin/notifications/read-all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
        mockMvc.perform(get("/api/admin/notifications/unread-count").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        for (String name : List.of("Test A", "Test B", "Test C")) {
            mockMvc.perform(post("/api/admin/categories")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nom\":\"" + name + "\",\"description\":\"\"}"))
                    .andExpect(status().isCreated());
        }
        for (int refresh = 0; refresh < 2; refresh++) {
            mockMvc.perform(get("/api/admin/categories").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nom").value("Test C"))
                    .andExpect(jsonPath("$[1].nom").value("Test B"))
                    .andExpect(jsonPath("$[2].nom").value("Test A"));
        }

        mockMvc.perform(get("/api/admin/dashboard/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("totalVehicules")));
        mockMvc.perform(get("/api/admin/dashboard/analytics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/dashboard/analytics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalCount").isNumber())
                .andExpect(jsonPath("$.vehicleStatuses").exists())
                .andExpect(jsonPath("$.limitations").value(org.hamcrest.Matchers.containsString("ne prouvent pas un encaissement")));
        mockMvc.perform(get("/api/admin/dashboard/analytics").header("Authorization", "Bearer " + token)
                        .param("from", "2024-01-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/vehicules/{id}/statut", vehicle.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("statut", "INACTIF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("INACTIF"));
        if (!"INACTIF".equals(vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatut())) {
            throw new AssertionError("Le statut INACTIF doit être persisté après l’appel PATCH");
        }
        mockMvc.perform(get("/api/admin/vehicules/{id}", vehicle.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("INACTIF"));

        mockMvc.perform(patch("/api/admin/vehicules/{id}/statut", vehicle.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("statut", "ACTIF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIF"));
        if (!"ACTIF".equals(vehicleRepository.findById(vehicle.getId()).orElseThrow().getStatut())) {
            throw new AssertionError("Le statut ACTIF doit être persisté après l’appel PATCH");
        }
        mockMvc.perform(get("/api/admin/vehicules/{id}", vehicle.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIF"));
    }

    @Test
    @Transactional
    void adminCanCreateAndReloadUserRentalHistoryForAnExistingVehicle() throws Exception {
        String adminEmail = "rental-admin-" + UUID.randomUUID() + "@gmt.test";
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setRole("ADMIN");
        admin.setActif(true);
        admin.setEmailVerified(true);
        admin.setMotDePasse(passwordEncoder.encode("rental-test-password"));
        userRepository.save(admin);

        User customer = new User();
        customer.setEmail("rental-user-" + UUID.randomUUID() + "@gmt.test");
        customer.setRole("USER");
        customer.setActif(true);
        userRepository.save(customer);

        Category category = new Category();
        category.setNom("Rental " + UUID.randomUUID());
        category = categoryRepository.save(category);

        Vehicle vehicle = new Vehicle();
        vehicle.setMarque("Volkswagen");
        vehicle.setModele("Golf 8");
        vehicle.setPlaces(5);
        vehicle.setPrixParJour(new BigDecimal("200"));
        vehicle.setStatut("ACTIF");
        vehicle.setCategorie(category);
        vehicle = vehicleRepository.save(vehicle);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"rental-test-password"}
                                """.formatted(adminEmail)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(loginResponse);
        if (!matcher.find()) throw new AssertionError("La réponse de connexion ne contient pas de jeton JWT");
        String auth = "Bearer " + matcher.group(1);

        String historyUrl = "/api/admin/users/{userId}/rentals";
        mockMvc.perform(get(historyUrl, customer.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(historyUrl, customer.getId()).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        String start = "2026-10-01";
        String end = "2026-10-05";
        mockMvc.perform(post(historyUrl, customer.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId":%d,"dateDebut":"%s","dateFin":"%s","prixPaye":800,"devise":"DT"}
                                """.formatted(vehicle.getId(), start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicle").value("Volkswagen Golf 8"))
                .andExpect(jsonPath("$.dateDebut").value(start))
                .andExpect(jsonPath("$.dateFin").value(end))
                .andExpect(jsonPath("$.prixPaye").value(800))
                .andExpect(jsonPath("$.devise").value("DT"));

        RentalHistory saved = rentalHistoryRepository.findAllByUserIdOrderByDateDebutDescIdDesc(customer.getId()).getFirst();
        assertEquals(customer.getId(), saved.getUser().getId());
        assertEquals(vehicle.getId(), saved.getVehicleEntity().getId());
        assertEquals(0, saved.getPrixPaye().compareTo(new BigDecimal("800")));
        assertEquals("DT", saved.getDevise());

        mockMvc.perform(get(historyUrl, customer.getId()).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(saved.getId()))
                .andExpect(jsonPath("$[0].vehicle").value("Volkswagen Golf 8"));
        mockMvc.perform(post(historyUrl, customer.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId":999999,"dateDebut":"2026-10-01","dateFin":"2026-10-05","prixPaye":800,"devise":"DT"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(historyUrl, customer.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId":%d,"dateDebut":"2026-10-01","dateFin":"2026-10-01","prixPaye":800,"devise":"DT"}
                                """.formatted(vehicle.getId())))
                .andExpect(status().isBadRequest());
    }
}
