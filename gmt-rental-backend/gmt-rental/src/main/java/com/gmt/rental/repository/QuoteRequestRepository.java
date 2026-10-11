package com.gmt.rental.repository;

import com.gmt.rental.entity.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    java.util.Optional<QuoteRequest> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = "vehicule")
    @Query("select request from QuoteRequest request where request.id = :id")
    java.util.Optional<QuoteRequest> findByIdWithVehicle(@Param("id") Long id);

    @Query("""
            SELECT request FROM QuoteRequest request
            WHERE request.vehicule.id = :vehicleId
              AND request.dateDebut < :dateFin
              AND request.dateFin > :dateDebut
            ORDER BY request.dateDebut, request.dateFin
            """)
    List<QuoteRequest> findDateOverlapsByVehicleId(
            @Param("vehicleId") Long vehicleId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    interface MonthlyStatusAggregate {
        Integer getYear(); Integer getMonth(); String getStatus(); Long getRequestCount();
    }

    @Query("""
            select year(request.dateCreation) as year, month(request.dateCreation) as month,
                   request.statut as status, count(request) as requestCount
            from QuoteRequest request
            where request.dateCreation >= :from and request.dateCreation < :toExclusive
            group by year(request.dateCreation), month(request.dateCreation), request.statut
            order by year(request.dateCreation), month(request.dateCreation)
            """)
    List<MonthlyStatusAggregate> countMonthlyByStatus(@Param("from") java.time.LocalDateTime from,
                                                       @Param("toExclusive") java.time.LocalDateTime toExclusive);

    long countByStatutIgnoreCase(String statut);
    long countByStatutIgnoreCaseAndDateCreationBetween(String statut, java.time.LocalDateTime from,
                                                        java.time.LocalDateTime to);
}
