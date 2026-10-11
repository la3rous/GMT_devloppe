package com.gmt.rental.config;

import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Small, repeatable dashboard fixtures for the isolated local H2 profile only. */
@Configuration
@Profile("local")
@ConditionalOnProperty(name = "gmt.dashboard.demo-data", havingValue = "true")
public class LocalDashboardDemoData {
    private static final String DEMO_EMAIL = "dashboard.demo@gmt.test";

    @Bean
    ApplicationRunner seedLocalDashboardData(UserRepository users, VehicleRepository vehicles,
                                             RentalHistoryRepository rentals, QuoteRequestRepository quotes) {
        return args -> seed(users, vehicles, rentals, quotes);
    }

    void seed(UserRepository users, VehicleRepository vehicles,
              RentalHistoryRepository rentals, QuoteRequestRepository quotes) {
        if (users.findByEmail(DEMO_EMAIL).isPresent()) return;

        User client = new User();
        client.setNom("Démo");
        client.setPrenom("Client");
        client.setEmail(DEMO_EMAIL);
        client.setRole("USER");
        client.setActif(true);
        client = users.save(client);

        Vehicle sedan = vehicle(vehicles, "GMT", "Sedan Démo", "ACTIF");
        Vehicle suv = vehicle(vehicles, "GMT", "SUV Démo", "ACTIF");
        Vehicle city = vehicle(vehicles, "GMT", "City Démo", "ACTIF");
        List<Vehicle> fleet = List.of(sedan, suv, city);

        LocalDate currentMonth = YearMonth.now().atDay(1);
        for (int offset = 5; offset >= 0; offset--) {
            LocalDate start = currentMonth.minusMonths(offset).plusDays(2);
            Vehicle selected = fleet.get(offset % fleet.size());
            saveRental(rentals, client, selected, start, new BigDecimal("320.000"));
            if (offset % 2 == 0) {
                saveRental(rentals, client, suv, start.plusDays(5), new BigDecimal("480.000"));
            }

            QuoteRequest quote = new QuoteRequest();
            quote.setNom("Démo");
            quote.setPrenom("Client");
            quote.setEmail(DEMO_EMAIL);
            quote.setVehicule(selected);
            quote.setDateDebut(start.plusDays(10));
            quote.setDateFin(start.plusDays(13));
            quote.setPrixTotal(new BigDecimal("450.000"));
            quote.setStatut(offset % 3 == 0 ? "ANNULEE" : offset % 2 == 0 ? "TRAITEE" : "NOUVELLE");
            quotes.save(quote);
            quote.setDateCreation(start.atTime(10, 0));
            quotes.save(quote);
        }
    }

    private Vehicle vehicle(VehicleRepository vehicles, String brand, String model, String status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setMarque(brand);
        vehicle.setModele(model);
        vehicle.setStatut(status);
        return vehicles.save(vehicle);
    }

    private void saveRental(RentalHistoryRepository rentals, User client, Vehicle vehicle,
                            LocalDate start, BigDecimal amount) {
        RentalHistory rental = new RentalHistory();
        rental.setUser(client);
        rental.setVehicleEntity(vehicle);
        rental.setVehicle(vehicle.getMarque() + " " + vehicle.getModele());
        rental.setDateDebut(start);
        rental.setDateFin(start.plusDays(3));
        rental.setPrixPaye(amount);
        rental.setDevise("TND");
        rentals.save(rental);
    }
}
