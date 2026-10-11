package com.gmt.rental.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminNotificationDTO(
        Long id,
        String type,
        String message,
        boolean lu,
        LocalDateTime dateCreation,
        Long devisId,
        String nom,
        String prenom,
        String vehicule,
        LocalDate dateDebut,
        LocalDate dateFin
) {
}
