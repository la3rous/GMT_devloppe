package com.gmt.rental.service;

import com.gmt.rental.dto.QuoteRequestDTO;
import com.gmt.rental.entity.QuoteRequest;
import com.gmt.rental.entity.Notification;
import com.gmt.rental.entity.User;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.NotificationRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.UserRepository;
import com.gmt.rental.repository.VehicleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuoteRequestService {

    private final QuoteRequestRepository quoteRequestRepository;
    private final NotificationRepository notificationRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehicleAvailabilityService vehicleAvailabilityService;
    private final ApplicationEventPublisher events;

    public QuoteRequestService(
            QuoteRequestRepository quoteRequestRepository,
            NotificationRepository notificationRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VehicleAvailabilityService vehicleAvailabilityService,
            ApplicationEventPublisher events) {

        this.quoteRequestRepository = quoteRequestRepository;
        this.notificationRepository = notificationRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.vehicleAvailabilityService = vehicleAvailabilityService;
        this.events = events;
    }

    @Transactional
    public QuoteRequest createQuote(QuoteRequestDTO dto) {
        return createQuote(dto, null);
    }

    @Transactional
    public QuoteRequest createQuote(QuoteRequestDTO dto, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idempotencyKey.length() > 80) throw new IllegalArgumentException("Clé de requête invalide.");
            Optional<QuoteRequest> previous = quoteRequestRepository.findByIdempotencyKey(idempotencyKey);
            if (previous.isPresent()) return previous.get();
        }

        if (dto == null || dto.getVehiculeId() == null) {
            throw new IllegalArgumentException("Veuillez sélectionner un véhicule.");
        }
        if (isBlank(dto.getNom()) || isBlank(dto.getPrenom()) || isBlank(dto.getEmail()) || isBlank(dto.getTelephone())) {
            throw new IllegalArgumentException("Le nom, le prénom, l’email et le téléphone sont obligatoires.");
        }
        if (!dto.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("L’adresse email est invalide.");
        }
        if (isBlank(dto.getLieuPriseEnCharge()) || isBlank(dto.getLieuRestitution())) {
            throw new IllegalArgumentException("Les lieux de prise en charge et de restitution sont obligatoires.");
        }
        if (dto.getDateDebut() == null || dto.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (dto.getDateDebut().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date de début ne peut pas être dans le passé.");
        }

        // Chercher le véhicule
        Vehicle vehicle = vehicleRepository
                .findByIdForUpdate(dto.getVehiculeId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Le véhicule sélectionné n'existe plus. Veuillez en choisir un autre."));
        if (!"ACTIF".equalsIgnoreCase(vehicle.getStatut())
                || (vehicle.getCategorie() != null && Boolean.FALSE.equals(vehicle.getCategorie().getActif()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Le véhicule sélectionné n'est plus disponible dans le catalogue.");
        }

        // Vérifier les dates
        long nombreJours = ChronoUnit.DAYS.between(
                dto.getDateDebut(),
                dto.getDateFin()
        );

        if (nombreJours <= 0) {
            throw new IllegalArgumentException(
                    "La date de fin doit être après la date de début"
            );
        }

        vehicleAvailabilityService.ensureAvailable(
                vehicle, dto.getDateDebut(), dto.getDateFin());

        // Le tarif peut être absent pour une demande de devis : le prix sera
        // déterminé par l'équipe, et le total restera null jusque-là.
        BigDecimal prixTotal = vehicle.getPrixParJour() == null
                ? null
                : vehicle.getPrixParJour().multiply(BigDecimal.valueOf(nombreJours));

        // Créer l'entité
        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);

        quoteRequest.setNom(dto.getNom());
        quoteRequest.setPrenom(dto.getPrenom());
        quoteRequest.setEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
        quoteRequest.setTelephone(dto.getTelephone());
        quoteRequest.setUser(findOrCreateUser(dto));

        quoteRequest.setVehicule(vehicle);

        quoteRequest.setDateDebut(dto.getDateDebut());
        quoteRequest.setDateFin(dto.getDateFin());

        quoteRequest.setLieuPriseEnCharge(
                dto.getLieuPriseEnCharge()
        );

        quoteRequest.setLieuRestitution(
                dto.getLieuRestitution()
        );

        quoteRequest.setMessage(dto.getMessage());

        quoteRequest.setPrixTotal(prixTotal);

        // Sauvegarder dans PostgreSQL
        QuoteRequest savedQuote = quoteRequestRepository.save(quoteRequest);

        Notification notification = new Notification();
        notification.setType("NOUVEAU_DEVIS");
        notification.setMessage("Nouveau devis reçu ! Un client vient d'envoyer une demande de location.");
        notification.setDevis(savedQuote);
        notification.setLu(false);
        notificationRepository.save(notification);

        events.publishEvent(new QuoteCreatedEvent(savedQuote.getId()));

        return savedQuote;
    }

    private User findOrCreateUser(QuoteRequestDTO dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
            if (role.equals("ADMIN") || role.equals("ROLE_ADMIN")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cette adresse e-mail est réservée à un compte administrateur.");
            }
            return user;
        }

        User user = new User();
        user.setNom(dto.getNom().trim());
        user.setPrenom(dto.getPrenom().trim());
        user.setEmail(email);
        user.setTelephone(dto.getTelephone().trim());
        user.setRole("USER");
        user.setActif(true);
        user.setEmailVerified(false);
        user.setMotDePasse(passwordEncoder.encode(UUID.randomUUID().toString()));
        return userRepository.save(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
