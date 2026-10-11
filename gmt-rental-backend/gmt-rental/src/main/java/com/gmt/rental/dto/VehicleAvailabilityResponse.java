package com.gmt.rental.dto;

import java.util.List;

public record VehicleAvailabilityResponse(
        boolean available,
        List<UnavailablePeriod> conflictingPeriods,
        String message) {
}
