package com.gmt.rental.dto;

import java.time.LocalDate;

public record UnavailablePeriod(LocalDate dateDebut, LocalDate dateFin) {
}
