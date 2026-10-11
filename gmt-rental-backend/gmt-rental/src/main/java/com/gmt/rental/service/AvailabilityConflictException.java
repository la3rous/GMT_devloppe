package com.gmt.rental.service;

import com.gmt.rental.dto.UnavailablePeriod;

import java.util.List;

public class AvailabilityConflictException extends RuntimeException {
    private final List<UnavailablePeriod> conflictingPeriods;

    public AvailabilityConflictException(String message, List<UnavailablePeriod> conflictingPeriods) {
        super(message);
        this.conflictingPeriods = List.copyOf(conflictingPeriods);
    }

    public List<UnavailablePeriod> getConflictingPeriods() {
        return conflictingPeriods;
    }
}
