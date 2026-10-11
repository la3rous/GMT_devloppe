package com.gmt.rental.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentalHistoryRequest(
        Long vehicleId,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal prixPaye,
        String devise
) {}
