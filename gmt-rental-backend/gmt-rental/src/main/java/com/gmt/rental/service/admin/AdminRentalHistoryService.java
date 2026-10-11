package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.RentalHistoryRequest;
import com.gmt.rental.dto.admin.RentalHistoryResponse;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminRentalHistoryService {
    private static final Set<String> CURRENCIES = Set.of("DT", "EUR");
    private final RentalHistoryRepository rentals;
    private final UserRepository users;
    private final VehicleRepository vehicles;

    public AdminRentalHistoryService(RentalHistoryRepository rentals, UserRepository users, VehicleRepository vehicles) {
        this.rentals = rentals;
        this.users = users;
        this.vehicles = vehicles;
    }

    public List<RentalHistoryResponse> findByUser(Long userId) {
        ensureUserExists(userId);
        return rentals.findAllByUserIdOrderByDateDebutDescIdDesc(userId).stream().map(this::toResponse).toList();
    }

    public RentalHistoryResponse create(Long userId, RentalHistoryRequest data) {
        User user = ensureUserExists(userId);
        if (data == null || data.vehicleId() == null
                || data.dateDebut() == null || data.dateFin() == null || data.prixPaye() == null
                || data.devise() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les détails de la location sont obligatoires.");
        }
        if (!data.dateFin().isAfter(data.dateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit être après la date de début.");
        }
        if (data.prixPaye().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix payé doit être supérieur à zéro.");
        }
        String devise = data.devise().trim().toUpperCase(Locale.ROOT);
        if (!CURRENCIES.contains(devise)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La devise doit être DT ou EUR.");
        }

        Vehicle vehicle = vehicles.findById(data.vehicleId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable : " + data.vehicleId()));
        RentalHistory rental = new RentalHistory();
        rental.setUser(user);
        rental.setVehicleEntity(vehicle);
        rental.setVehicle((vehicle.getMarque() + " " + vehicle.getModele()).trim());
        rental.setDateDebut(data.dateDebut());
        rental.setDateFin(data.dateFin());
        rental.setPrixPaye(data.prixPaye());
        rental.setDevise(devise);
        return toResponse(rentals.save(rental));
    }

    private User ensureUserExists(Long userId) {
        return users.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + userId));
    }

    private RentalHistoryResponse toResponse(RentalHistory rental) {
        return new RentalHistoryResponse(rental.getId(), rental.getVehicle(), rental.getDateDebut(),
                rental.getDateFin(), rental.getPrixPaye(), rental.getDevise());
    }
}
