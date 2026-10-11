package com.gmt.rental.service.admin;

import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.VehicleRepository;
import com.gmt.rental.service.VehicleAvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminQuoteRequestService {
    private final QuoteRequestRepository requests;
    private final VehicleRepository vehicles;
    private final VehicleAvailabilityService availability;

    public AdminQuoteRequestService(QuoteRequestRepository requests, VehicleRepository vehicles,
                                    VehicleAvailabilityService availability) {
        this.requests = requests;
        this.vehicles = vehicles;
        this.availability = availability;
    }

    public List<QuoteRequest> findAll(String status) {
        return requests.findAll().stream()
                .filter(request -> status == null || status.isBlank() || status.equalsIgnoreCase(request.getStatut()))
                .sorted(Comparator.comparing(QuoteRequest::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public QuoteRequest findById(Long id) { return requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de devis introuvable")); }

    @Transactional
    public QuoteRequest updateStatus(Long id, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("NOUVELLE", "EN_COURS", "TRAITEE", "ANNULEE", "ACCEPTEE", "REFUSEE")
                .contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de demande invalide");
        }
        QuoteRequest request = requests.findByIdWithVehicle(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de devis introuvable"));

        if ("ACCEPTEE".equals(normalizedStatus)) {
            if (request.getVehicule() == null || request.getVehicule().getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette demande ne peut pas être acceptée car aucun véhicule n’y est associé.");
            }
            Vehicle vehicle = vehicles.findByIdForUpdate(request.getVehicule().getId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT,
                            "Cette demande ne peut pas être acceptée car le véhicule n’existe plus."));
            if (!"ACTIF".equalsIgnoreCase(vehicle.getStatut())
                    || (vehicle.getCategorie() != null && Boolean.FALSE.equals(vehicle.getCategorie().getActif()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette demande ne peut pas être acceptée car le véhicule ou sa catégorie est inactif.");
            }
            availability.ensureAvailable(vehicle, request.getDateDebut(), request.getDateFin(), request.getId());
            request.setVehicule(vehicle);
        }

        request.setStatut(normalizedStatus);
        return requests.save(request);
    }
}
