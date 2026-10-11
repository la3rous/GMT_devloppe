package com.gmt.rental.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentalHistoryResponse(
        Long id,
        String vehicle,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal prixPaye,
        String devise
) {}
