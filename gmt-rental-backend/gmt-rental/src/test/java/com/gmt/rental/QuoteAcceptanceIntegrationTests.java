package com.gmt.rental;

import com.gmt.rental.entity.Category;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.CategoryRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import com.gmt.rental.service.AvailabilityConflictException;
import com.gmt.rental.service.VehicleAvailabilityService;
import com.gmt.rental.service.admin.AdminQuoteRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gmt_quote_acceptance;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "gmt.dashboard.demo-data=false"
})
@ActiveProfiles("local")
class QuoteAcceptanceIntegrationTests {
    @Autowired private CategoryRepository categories;
    @Autowired private VehicleRepository vehicles;
    @Autowired private QuoteRequestRepository quotes;
    @Autowired private RentalHistoryRepository rentals;
    @Autowired private UserRepository users;
    @Autowired private VehicleAvailabilityService availability;
    @Autowired private AdminQuoteRequestService adminQuotes;

    @Test
    void onlyExplicitAcceptanceBlocksAndLegacyStatusesRemainNonBlocking() {
        Vehicle vehicle = vehicle(true, true);
        LocalDate start = LocalDate.now().plusDays(30);
        for (String status : new String[]{"NOUVELLE", "EN_COURS", "TRAITEE", "REFUSEE", "ANNULEE"}) {
            QuoteRequest request = quote(vehicle, start, start.plusDays(3), status);
            assertTrue(availability.checkAvailability(vehicle.getId(), start, start.plusDays(3)).available(), status);
        }

        quote(vehicle, start, start.plusDays(3), "ACCEPTEE");
        assertFalse(availability.checkAvailability(vehicle.getId(), start, start.plusDays(3)).available());
    }

    @Test
    void overlappingRequestsCannotBothBeAcceptedAndFailureKeepsPendingStatus() {
        Vehicle vehicle = vehicle(true, true);
        LocalDate start = LocalDate.now().plusDays(40);
        QuoteRequest first = quote(vehicle, start, start.plusDays(4), "NOUVELLE");
        QuoteRequest second = quote(vehicle, start.plusDays(2), start.plusDays(5), "NOUVELLE");

        adminQuotes.updateStatus(first.getId(), "ACCEPTEE");
        assertThrows(AvailabilityConflictException.class,
                () -> adminQuotes.updateStatus(second.getId(), "ACCEPTEE"));
        assertEquals("ACCEPTEE", quotes.findById(first.getId()).orElseThrow().getStatut());
        assertEquals("NOUVELLE", quotes.findById(second.getId()).orElseThrow().getStatut());
    }

    @Test
    void separatePeriodsCanBothBeAccepted() {
        Vehicle vehicle = vehicle(true, true);
        LocalDate start = LocalDate.now().plusDays(50);
        QuoteRequest first = quote(vehicle, start, start.plusDays(3), "NOUVELLE");
        QuoteRequest second = quote(vehicle, start.plusDays(3), start.plusDays(6), "NOUVELLE");

        adminQuotes.updateStatus(first.getId(), "ACCEPTEE");
        adminQuotes.updateStatus(second.getId(), "ACCEPTEE");

        assertEquals("ACCEPTEE", quotes.findById(first.getId()).orElseThrow().getStatut());
        assertEquals("ACCEPTEE", quotes.findById(second.getId()).orElseThrow().getStatut());
    }

    @Test
    void inactiveVehicleOrCategoryCannotBeAccepted() {
        LocalDate start = LocalDate.now().plusDays(60);
        Vehicle inactiveVehicle = vehicle(false, true);
        QuoteRequest vehicleQuote = quote(inactiveVehicle, start, start.plusDays(2), "NOUVELLE");
        assertThrows(ResponseStatusException.class, () -> adminQuotes.updateStatus(vehicleQuote.getId(), "ACCEPTEE"));
        assertEquals("NOUVELLE", quotes.findById(vehicleQuote.getId()).orElseThrow().getStatut());

        Vehicle inactiveCategoryVehicle = vehicle(true, false);
        QuoteRequest categoryQuote = quote(inactiveCategoryVehicle, start, start.plusDays(2), "NOUVELLE");
        assertThrows(ResponseStatusException.class, () -> adminQuotes.updateStatus(categoryQuote.getId(), "ACCEPTEE"));
        assertEquals("NOUVELLE", quotes.findById(categoryQuote.getId()).orElseThrow().getStatut());
    }

    @Test
    void rentalHistoryStillBlocksAcceptance() {
        Vehicle vehicle = vehicle(true, true);
        LocalDate start = LocalDate.now().plusDays(70);
        User user = new User();
        user.setEmail("quote-test-" + UUID.randomUUID() + "@gmt.test");
        user.setRole("USER");
        users.save(user);

        RentalHistory rental = new RentalHistory();
        rental.setUser(user);
        rental.setVehicle(vehicle.getMarque() + " " + vehicle.getModele());
        rental.setVehicleEntity(vehicle);
        rental.setDateDebut(start.plusDays(1));
        rental.setDateFin(start.plusDays(3));
        rental.setPrixPaye(new BigDecimal("100"));
        rental.setDevise("DT");
        rentals.save(rental);

        QuoteRequest request = quote(vehicle, start, start.plusDays(4), "NOUVELLE");
        assertThrows(AvailabilityConflictException.class,
                () -> adminQuotes.updateStatus(request.getId(), "ACCEPTEE"));
        assertEquals("NOUVELLE", quotes.findById(request.getId()).orElseThrow().getStatut());
    }

    @Test
    void concurrentAcceptancesForSameVehicleCannotBothCommit() throws Exception {
        Vehicle vehicle = vehicle(true, true);
        LocalDate start = LocalDate.now().plusDays(80);
        QuoteRequest first = quote(vehicle, start, start.plusDays(4), "NOUVELLE");
        QuoteRequest second = quote(vehicle, start.plusDays(1), start.plusDays(5), "NOUVELLE");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> attemptAcceptance(first.getId(), ready, startTogether));
            var secondResult = executor.submit(() -> attemptAcceptance(second.getId(), ready, startTogether));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Deux demandes doivent être prêtes à concourir");
            startTogether.countDown();
            assertTrue(firstResult.get(20, TimeUnit.SECONDS) ^ secondResult.get(20, TimeUnit.SECONDS),
                    "Une seule demande chevauchante doit être acceptée");
        }

        long accepted = quotes.findAll().stream()
                .filter(request -> "ACCEPTEE".equals(request.getStatut()))
                .filter(request -> vehicle.getId().equals(request.getVehicule().getId()))
                .filter(request -> request.getDateDebut().equals(start)
                        || request.getDateDebut().equals(start.plusDays(1)))
                .count();
        assertEquals(1, accepted);
    }

    private boolean attemptAcceptance(Long id, CountDownLatch ready, CountDownLatch startTogether) throws Exception {
        ready.countDown();
        if (!startTogether.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Barrière expirée");
        try {
            adminQuotes.updateStatus(id, "ACCEPTEE");
            return true;
        } catch (AvailabilityConflictException expected) {
            return false;
        }
    }

    private Vehicle vehicle(boolean active, boolean activeCategory) {
        Category category = new Category();
        category.setNom("Catégorie test " + UUID.randomUUID());
        category.setActif(activeCategory);
        category = categories.save(category);

        Vehicle vehicle = new Vehicle();
        vehicle.setMarque("GMT");
        vehicle.setModele("Test " + UUID.randomUUID());
        vehicle.setStatut(active ? "ACTIF" : "INACTIF");
        vehicle.setCategorie(category);
        return vehicles.save(vehicle);
    }

    private QuoteRequest quote(Vehicle vehicle, LocalDate start, LocalDate end, String status) {
        QuoteRequest request = new QuoteRequest();
        request.setNom("Test");
        request.setPrenom("Client");
        request.setEmail("quote-" + UUID.randomUUID() + "@gmt.test");
        request.setVehicule(vehicle);
        request.setDateDebut(start);
        request.setDateFin(end);
        request.setStatut(status);
        return quotes.saveAndFlush(request);
    }
}
