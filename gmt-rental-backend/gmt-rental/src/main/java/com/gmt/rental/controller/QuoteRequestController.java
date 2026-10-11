package com.gmt.rental.controller;

import com.gmt.rental.dto.QuoteRequestDTO;
import com.gmt.rental.dto.VehicleAvailabilityResponse;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.service.AvailabilityConflictException;
import com.gmt.rental.service.QuoteRequestService;
import com.gmt.rental.service.VehicleAvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public/demandes")
@CrossOrigin(origins = "http://localhost:5174")
public class QuoteRequestController {

    private final QuoteRequestService quoteRequestService;
    private final VehicleAvailabilityService vehicleAvailabilityService;

    public QuoteRequestController(
            QuoteRequestService quoteRequestService,
            VehicleAvailabilityService vehicleAvailabilityService) {
        this.quoteRequestService = quoteRequestService;
        this.vehicleAvailabilityService = vehicleAvailabilityService;
    }

    @GetMapping("/disponibilite")
    public VehicleAvailabilityResponse checkAvailability(
            @RequestParam Long vehiculeId,
            @RequestParam LocalDate dateDebut,
            @RequestParam LocalDate dateFin) {
        return vehicleAvailabilityService.checkAvailability(vehiculeId, dateDebut, dateFin);
    }

    @PostMapping
    public ResponseEntity<QuoteRequest> createQuote(
            @RequestBody QuoteRequestDTO dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        QuoteRequest savedQuote =
                quoteRequestService.createQuote(dto, idempotencyKey);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedQuote);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(AvailabilityConflictException.class)
    public ResponseEntity<VehicleAvailabilityResponse> handleAvailabilityConflict(
            AvailabilityConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new VehicleAvailabilityResponse(false, exception.getConflictingPeriods(), exception.getMessage()));
    }

}
