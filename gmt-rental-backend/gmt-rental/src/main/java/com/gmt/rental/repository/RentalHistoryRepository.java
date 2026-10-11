package com.gmt.rental.repository;

import com.gmt.rental.entity.RentalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;

public interface RentalHistoryRepository extends JpaRepository<RentalHistory, Long> {
    List<RentalHistory> findAllByUserIdOrderByDateDebutDescIdDesc(Long userId);

    @Query("""
            SELECT rental FROM RentalHistory rental
            WHERE (rental.vehicleEntity.id = :vehicleId
                   OR (rental.vehicleEntity IS NULL
                       AND LOWER(rental.vehicle) = LOWER(:vehicleName)))
              AND rental.dateDebut < :dateFin
              AND rental.dateFin > :dateDebut
            ORDER BY rental.dateDebut, rental.dateFin
            """)
    List<RentalHistory> findDateOverlapsByVehicleId(
            @Param("vehicleId") Long vehicleId,
            @Param("vehicleName") String vehicleName,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    interface VehicleRentalAggregate {
        Long getVehicleId(); String getVehicleName(); Long getRentalCount();
    }
    interface ClientRentalAggregate {
        Long getUserId(); String getClientName(); Long getRentalCount();
    }
    interface ClientSpendAggregate {
        Long getUserId(); String getClientName(); String getCurrency(); BigDecimal getAmount();
    }
    interface RevenueAggregate {
        Long getVehicleId(); String getVehicleName(); Integer getYear(); Integer getMonth();
        String getCurrency(); BigDecimal getAmount();
    }
    interface RentalPeriodAggregate {
        Long getVehicleId(); String getVehicleName(); LocalDate getDateDebut(); LocalDate getDateFin();
    }

    @Query("""
            select v.id as vehicleId,
                   case when v.id is null then r.vehicle else concat(v.marque, concat(' ', v.modele)) end as vehicleName,
                   count(r) as rentalCount
            from RentalHistory r left join r.vehicleEntity v
            where r.dateDebut >= :from and r.dateDebut <= :to
            group by v.id, v.marque, v.modele, case when v.id is null then r.vehicle else concat(v.marque, concat(' ', v.modele)) end
            order by count(r) desc
            """)
    List<VehicleRentalAggregate> countRentalsByVehicle(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByDateDebutBetween(LocalDate from, LocalDate to);

    @Query("""
            select u.id as userId, concat(u.prenom, concat(' ', u.nom)) as clientName, count(r) as rentalCount
            from RentalHistory r join r.user u
            where r.dateDebut >= :from and r.dateDebut <= :to
            group by u.id, u.prenom, u.nom
            order by count(r) desc
            """)
    List<ClientRentalAggregate> countRentalsByClient(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("""
            select u.id as userId, concat(u.prenom, concat(' ', u.nom)) as clientName,
                   r.devise as currency, sum(r.prixPaye) as amount
            from RentalHistory r join r.user u
            where r.dateDebut >= :from and r.dateDebut <= :to
            group by u.id, u.prenom, u.nom, r.devise
            order by sum(r.prixPaye) desc
            """)
    List<ClientSpendAggregate> sumSpendByClientAndCurrency(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select v.id as vehicleId,
                   case when v.id is null then r.vehicle else concat(v.marque, concat(' ', v.modele)) end as vehicleName,
                   year(r.dateDebut) as year, month(r.dateDebut) as month,
                   r.devise as currency, sum(r.prixPaye) as amount
            from RentalHistory r left join r.vehicleEntity v
            where r.dateDebut >= :from and r.dateDebut <= :to
            group by v.id, v.marque, v.modele, case when v.id is null then r.vehicle else concat(v.marque, concat(' ', v.modele)) end,
                     year(r.dateDebut), month(r.dateDebut), r.devise
            order by year(r.dateDebut), month(r.dateDebut)
            """)
    List<RevenueAggregate> sumRevenueByVehicleMonthAndCurrency(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select v.id as vehicleId,
                   case when v.id is null then r.vehicle else concat(v.marque, concat(' ', v.modele)) end as vehicleName,
                   r.dateDebut as dateDebut, r.dateFin as dateFin
            from RentalHistory r left join r.vehicleEntity v
            where r.dateDebut < :toExclusive and r.dateFin > :from
            """)
    List<RentalPeriodAggregate> findPeriodsIntersecting(@Param("from") LocalDate from,
                                                         @Param("toExclusive") LocalDate toExclusive);
}
