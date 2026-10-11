package com.gmt.rental;

import com.gmt.rental.dto.admin.DashboardAnalytics;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import com.gmt.rental.service.admin.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gmt_dashboard_analytics_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
@Transactional
class AdminDashboardAnalyticsTests {
    @Autowired private AdminDashboardService dashboard;
    @Autowired private UserRepository users;
    @Autowired private VehicleRepository vehicles;
    @Autowired private RentalHistoryRepository rentals;
    @Autowired private QuoteRequestRepository quotes;

    @Test
    void analyticsAggregateRentalsWithoutMixingCurrencyAndExcludeQuotePeriods() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(30);
        User client = new User();
        client.setNom("Client"); client.setPrenom("Test");
        client.setEmail("analytics-" + UUID.randomUUID() + "@gmt.test");
        client.setRole("USER"); client.setActif(true);
        client = users.save(client);

        Vehicle vehicle = new Vehicle();
        vehicle.setMarque("GMT"); vehicle.setModele("Analytics"); vehicle.setStatut("LOUE");
        vehicle = vehicles.save(vehicle);

        saveRental(client, vehicle, today.minusDays(10), today.minusDays(5), "100", "DT");
        saveRental(client, vehicle, today.minusDays(8), today.minusDays(3), "200", "DT");
        QuoteRequest canceled = new QuoteRequest();
        canceled.setNom("Client"); canceled.setPrenom("Test"); canceled.setEmail(client.getEmail());
        canceled.setStatut("ANNULEE"); quotes.save(canceled);

        DashboardAnalytics result = dashboard.analytics(from, today);
        assertEquals(2, result.rentalCount());
        assertEquals(1, result.canceledQuoteCount());
        assertEquals(7, result.occupiedDays()); // overlapping rental periods are unioned
        assertEquals(1, result.vehicleStatuses().get("LOUE"));
        assertEquals(1, result.revenueByVehicleMonth().size());
        assertEquals(new BigDecimal("300.000"), result.revenueByVehicleMonth().getFirst().amount());
        assertEquals("DT", result.revenueByVehicleMonth().getFirst().currency());
        assertEquals(1, result.clientsByRentals().size());
        assertTrue(result.limitations().contains("ne prouvent pas un encaissement"));
    }

    @Test
    void analyticsRejectInvalidAndOversizedPeriods() {
        LocalDate today = LocalDate.now();
        ResponseStatusException reversed = assertThrows(ResponseStatusException.class,
                () -> dashboard.analytics(today, today.minusDays(1)));
        assertEquals(HttpStatus.BAD_REQUEST, reversed.getStatusCode());
        assertThrows(ResponseStatusException.class,
                () -> dashboard.analytics(today.minusDays(366), today));
    }

    private void saveRental(User user, Vehicle vehicle, LocalDate from, LocalDate to, String amount, String currency) {
        RentalHistory rental = new RentalHistory();
        rental.setUser(user); rental.setVehicleEntity(vehicle);
        rental.setVehicle(vehicle.getMarque() + " " + vehicle.getModele());
        rental.setDateDebut(from); rental.setDateFin(to);
        rental.setPrixPaye(new BigDecimal(amount)); rental.setDevise(currency);
        rentals.save(rental);
    }
}
