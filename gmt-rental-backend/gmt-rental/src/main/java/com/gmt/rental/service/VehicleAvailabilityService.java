package com.gmt.rental.service;

import com.gmt.rental.dto.UnavailablePeriod;
import com.gmt.rental.dto.VehicleAvailabilityResponse;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.RentalHistory;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import com.gmt.rental.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class VehicleAvailabilityService {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuoteRequestRepository quoteRequests;
    private final RentalHistoryRepository rentalHistory;
    private final VehicleRepository vehicles;

    public VehicleAvailabilityService(
            QuoteRequestRepository quoteRequests,
            RentalHistoryRepository rentalHistory,
            VehicleRepository vehicles) {
        this.quoteRequests = quoteRequests;
        this.rentalHistory = rentalHistory;
        this.vehicles = vehicles;
    }

    public VehicleAvailabilityResponse checkAvailability(Long vehicleId, LocalDate dateDebut, LocalDate dateFin) {
        validatePeriod(dateDebut, dateFin);
        Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Le véhicule sélectionné n'existe plus. Veuillez en choisir un autre."));
        if (!"ACTIF".equalsIgnoreCase(vehicle.getStatut())
                || (vehicle.getCategorie() != null && Boolean.FALSE.equals(vehicle.getCategorie().getActif()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Le véhicule sélectionné n'est plus disponible dans le catalogue.");
        }

        List<UnavailablePeriod> conflicts = findConflictingPeriods(vehicle, dateDebut, dateFin, null);
        return new VehicleAvailabilityResponse(
                conflicts.isEmpty(),
                conflicts,
                conflicts.isEmpty() ? null : formatConflictMessage(conflicts));
    }

    private List<UnavailablePeriod> findConflictingPeriods(
            Vehicle vehicle, LocalDate dateDebut, LocalDate dateFin, Long excludedQuoteId) {
        Stream<UnavailablePeriod> rentalConflicts = rentalHistory
                .findDateOverlapsByVehicleId(
                        vehicle.getId(),
                        (vehicle.getMarque() + " " + vehicle.getModele()).trim(),
                        dateDebut,
                        dateFin)
                .stream()
                .map(this::toUnavailablePeriod);
        Stream<UnavailablePeriod> requestConflicts = quoteRequests
                .findDateOverlapsByVehicleId(vehicle.getId(), dateDebut, dateFin)
                .stream()
                .filter(request -> isAccepted(request.getStatut()))
                .filter(request -> excludedQuoteId == null || !excludedQuoteId.equals(request.getId()))
                .map(this::toUnavailablePeriod);

        return Stream.concat(rentalConflicts, requestConflicts)
                .distinct()
                .sorted((first, second) -> {
                    int startComparison = first.dateDebut().compareTo(second.dateDebut());
                    return startComparison != 0
                            ? startComparison
                            : first.dateFin().compareTo(second.dateFin());
                })
                .toList();
    }

    public void ensureAvailable(Vehicle vehicle, LocalDate dateDebut, LocalDate dateFin) {
        ensureAvailable(vehicle, dateDebut, dateFin, null);
    }

    public void ensureAvailable(Vehicle vehicle, LocalDate dateDebut, LocalDate dateFin, Long excludedQuoteId) {
        validatePeriod(dateDebut, dateFin);
        List<UnavailablePeriod> conflicts = findConflictingPeriods(vehicle, dateDebut, dateFin, excludedQuoteId);
        if (!conflicts.isEmpty()) {
            throw new AvailabilityConflictException(formatConflictMessage(conflicts), conflicts);
        }
    }

    private boolean isAccepted(String status) {
        return status != null && "ACCEPTEE".equals(status.trim().toUpperCase(Locale.ROOT));
    }

    private UnavailablePeriod toUnavailablePeriod(QuoteRequest request) {
        return new UnavailablePeriod(request.getDateDebut(), request.getDateFin());
    }

    private UnavailablePeriod toUnavailablePeriod(RentalHistory rental) {
        return new UnavailablePeriod(rental.getDateDebut(), rental.getDateFin());
    }

    private void validatePeriod(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || !dateFin.isAfter(dateDebut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être après la date de début.");
        }
    }

    private String formatConflictMessage(List<UnavailablePeriod> conflicts) {
        String periods = conflicts.stream()
                .map(period -> DISPLAY_DATE.format(period.dateDebut()) + " to "
                        + DISPLAY_DATE.format(period.dateFin()))
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
        return "This car is already rented or reserved from " + periods
                + ". Please choose different dates or another vehicle.";
    }
}
