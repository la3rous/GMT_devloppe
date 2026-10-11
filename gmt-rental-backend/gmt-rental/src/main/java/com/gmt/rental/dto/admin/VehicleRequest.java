package com.gmt.rental.dto.admin;

import java.math.BigDecimal;

public record VehicleRequest(
        String marque,
        String modele,
        Integer annee,
        Integer places,
        String transmission,
        String carburant,
        BigDecimal prixParJour,
        String description,
        String image,
        String statut,
        Long categorieId
) {}
