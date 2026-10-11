package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.DashboardSummary;
import com.gmt.rental.dto.admin.DashboardAnalytics;
import com.gmt.rental.repository.CategoryRepository;
import com.gmt.rental.repository.QuoteRequestRepository;
import com.gmt.rental.repository.VehicleRepository;
import com.gmt.rental.repository.RentalHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AdminDashboardService {
    private final VehicleRepository vehicles;
    private final CategoryRepository categories;
    private final QuoteRequestRepository requests;
    private final RentalHistoryRepository rentals;
    private final boolean demoData;

    public AdminDashboardService(VehicleRepository vehicles, CategoryRepository categories, QuoteRequestRepository requests,
                                 RentalHistoryRepository rentals,
                                 @Value("${gmt.dashboard.demo-data:false}") boolean demoData) {
        this.vehicles = vehicles;
        this.categories = categories;
        this.requests = requests;
        this.rentals = rentals;
        this.demoData = demoData;
    }

    public DashboardSummary summary() {
        long totalRequests = requests.count();
        long pending = requests.countByStatutIgnoreCase("NOUVELLE");
        return new DashboardSummary(vehicles.count(), categories.count(), pending, totalRequests);
    }

    public DashboardAnalytics analytics(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate to = requestedTo == null ? LocalDate.now() : requestedTo;
        LocalDate from = requestedFrom == null ? to.withDayOfYear(1) : requestedFrom;
        if (from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de début doit précéder la date de fin.");
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période analytique ne peut pas dépasser 366 jours.");
        }
        LocalDate toExclusive = to.plusDays(1);

        List<DashboardAnalytics.VehicleRentalCount> byVehicle = rentals.countRentalsByVehicle(from, to).stream()
                .map(row -> new DashboardAnalytics.VehicleRentalCount(row.getVehicleId(), row.getVehicleName(), row.getRentalCount()))
                .toList();
        long rentalCount = rentals.countByDateDebutBetween(from, to);
        List<DashboardAnalytics.ClientRentalCount> clientsByRentals = rentals.countRentalsByClient(from, to, PageRequest.of(0, 10)).stream()
                .map(row -> new DashboardAnalytics.ClientRentalCount(row.getUserId(), cleanName(row.getClientName()), row.getRentalCount())).toList();

        Map<String, List<DashboardAnalytics.ClientSpend>> spendsByCurrency = new TreeMap<>();
        rentals.sumSpendByClientAndCurrency(from, to).forEach(row -> spendsByCurrency
                .computeIfAbsent(currency(row.getCurrency()), ignored -> new ArrayList<>())
                .add(new DashboardAnalytics.ClientSpend(row.getUserId(), cleanName(row.getClientName()),
                        currency(row.getCurrency()), row.getAmount())));
        List<DashboardAnalytics.ClientSpend> clientsBySpend = spendsByCurrency.values().stream()
                .flatMap(rows -> rows.stream().limit(10)).toList();

        List<DashboardAnalytics.RevenuePoint> revenue = rentals.sumRevenueByVehicleMonthAndCurrency(from, to).stream()
                .map(row -> new DashboardAnalytics.RevenuePoint(row.getVehicleId(), row.getVehicleName(), row.getYear(), row.getMonth(),
                        currency(row.getCurrency()), row.getAmount())).toList();
        List<DashboardAnalytics.RequestTrendPoint> trend = requests.countMonthlyByStatus(
                        from.atStartOfDay(), toExclusive.atStartOfDay()).stream()
                .map(row -> new DashboardAnalytics.RequestTrendPoint(row.getYear(), row.getMonth(),
                        row.getStatus() == null ? "NON_RENSEIGNE" : row.getStatus(), row.getRequestCount())).toList();

        Map<String, Long> statusCounts = new TreeMap<>();
        vehicles.countByStatus().forEach(row -> statusCounts.merge(
                row.getStatus() == null || row.getStatus().isBlank() ? "Non renseigné" : row.getStatus(),
                row.getVehicleCount(), Long::sum));
        long canceledQuotes = requests.countByStatutIgnoreCaseAndDateCreationBetween("ANNULEE",
                from.atStartOfDay(), toExclusive.atStartOfDay().minusNanos(1));
        long fleetSize = vehicles.count();
        long possibleDays = fleetSize * ChronoUnit.DAYS.between(from, toExclusive);
        Map<Long, List<LocalDate[]>> periodsByVehicle = new HashMap<>();
        Map<Long, String> vehicleNames = new HashMap<>();
        rentals.findPeriodsIntersecting(from, toExclusive).forEach(period -> {
            if (period.getVehicleId() == null || period.getDateDebut() == null || period.getDateFin() == null) return;
            vehicleNames.putIfAbsent(period.getVehicleId(), period.getVehicleName());
            LocalDate clippedStart = period.getDateDebut().isBefore(from) ? from : period.getDateDebut();
            LocalDate clippedEnd = period.getDateFin().isAfter(toExclusive) ? toExclusive : period.getDateFin();
            if (clippedEnd.isAfter(clippedStart)) periodsByVehicle.computeIfAbsent(period.getVehicleId(), ignored -> new ArrayList<>())
                    .add(new LocalDate[]{clippedStart, clippedEnd});
        });

        List<DashboardAnalytics.VehicleOccupancy> occupancy = new ArrayList<>();
        long occupiedDays = 0;
        for (Map.Entry<Long, List<LocalDate[]>> entry : periodsByVehicle.entrySet()) {
            List<LocalDate[]> spans = entry.getValue();
            spans.sort(Comparator.comparing(span -> span[0]));
            LocalDate start = null, end = null;
            long uniqueDays = 0;
            for (LocalDate[] span : spans) {
                if (end == null || span[0].isAfter(end)) {
                    if (start != null) uniqueDays += ChronoUnit.DAYS.between(start, end);
                    start = span[0]; end = span[1];
                } else if (span[1].isAfter(end)) end = span[1];
            }
            if (start != null) uniqueDays += ChronoUnit.DAYS.between(start, end);
            occupiedDays += uniqueDays;
            long perVehicleDays = ChronoUnit.DAYS.between(from, toExclusive);
            occupancy.add(new DashboardAnalytics.VehicleOccupancy(entry.getKey(), vehicleNames.get(entry.getKey()), uniqueDays,
                    perVehicleDays, percent(uniqueDays, perVehicleDays)));
        }
        occupancy.sort(Comparator.comparingDouble(DashboardAnalytics.VehicleOccupancy::estimatedPercent).reversed());
        occupancy = occupancy.stream().limit(10).map(row -> new DashboardAnalytics.VehicleOccupancy(row.vehicleId(),
                vehicleNames.get(row.vehicleId()), row.occupiedDays(), row.possibleDays(), row.estimatedPercent())).toList();

        return new DashboardAnalytics(from, to, rentalCount, canceledQuotes, occupiedDays, possibleDays,
                percent(occupiedDays, possibleDays), statusCounts, byVehicle, byVehicle.stream().limit(10).toList(), occupancy,
                revenue, clientsByRentals, clientsBySpend, trend,
                "Estimations fondées sur rental_history (périodes [date_debut,date_fin), montants selon date_debut et devise enregistrée) et quote_requests (date_creation, statut courant). Les devis ne sont pas des locations; les montants ne prouvent pas un encaissement. L’occupation exclut les historiques sans vehicle_id et rapporte les jours enregistrés au parc actuel, faute d’historique de flotte.", demoData);
    }

    private static double percent(long value, long total) { return total == 0 ? 0 : BigDecimal.valueOf(value * 100.0 / total).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue(); }
    private static String currency(String value) { return value == null || value.isBlank() ? "INCONNUE" : value.trim().toUpperCase(Locale.ROOT); }
    private static String cleanName(String value) { return value == null || value.isBlank() ? "Client sans nom" : value.trim(); }
}
