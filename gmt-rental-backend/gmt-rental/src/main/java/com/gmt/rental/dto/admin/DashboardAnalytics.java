package com.gmt.rental.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Dashboard metrics computed from recorded rental history and quote requests only. */
public record DashboardAnalytics(
        LocalDate from,
        LocalDate to,
        long rentalCount,
        long canceledQuoteCount,
        long occupiedDays,
        long possibleFleetDays,
        double estimatedOccupancyPercent,
        Map<String, Long> vehicleStatuses,
        List<VehicleRentalCount> rentalsByVehicle,
        List<VehicleRentalCount> topVehicles,
        List<VehicleOccupancy> occupancyByVehicle,
        List<RevenuePoint> revenueByVehicleMonth,
        List<ClientRentalCount> clientsByRentals,
        List<ClientSpend> clientsBySpend,
        List<RequestTrendPoint> requestsByMonthAndStatus,
        String limitations,
        boolean demoData) {

    public record VehicleRentalCount(Long vehicleId, String vehicleName, long count) { }
    public record VehicleOccupancy(Long vehicleId, String vehicleName, long occupiedDays,
                                   long possibleDays, double estimatedPercent) { }
    public record RevenuePoint(Long vehicleId, String vehicleName, int year, int month,
                               String currency, BigDecimal amount) { }
    public record ClientRentalCount(Long userId, String name, long count) { }
    public record ClientSpend(Long userId, String name, String currency, BigDecimal amount) { }
    public record RequestTrendPoint(int year, int month, String status, long count) { }
}
