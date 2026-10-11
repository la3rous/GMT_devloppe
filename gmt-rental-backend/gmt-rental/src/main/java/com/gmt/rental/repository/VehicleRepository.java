package com.gmt.rental.repository;

import com.gmt.rental.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    interface StatusAggregate { String getStatus(); Long getVehicleCount(); }

    @Query("select vehicle.statut as status, count(vehicle) as vehicleCount from Vehicle vehicle group by vehicle.statut")
    List<StatusAggregate> countByStatus();

    List<Vehicle> findByStatut(String statut);

    List<Vehicle> findByCategorieId(Long categorieId);

    List<Vehicle> findByStatutAndCategorieId(String statut, Long categorieId);

    @Query("select vehicle from Vehicle vehicle where vehicle.statut = :status "
            + "and (vehicle.categorie is null or vehicle.categorie.actif is null or vehicle.categorie.actif = true)")
    List<Vehicle> findPublicVehiclesByStatus(@Param("status") String status);

    @Query("select vehicle from Vehicle vehicle where vehicle.statut = :status and vehicle.categorie.id = :categoryId "
            + "and (vehicle.categorie.actif is null or vehicle.categorie.actif = true)")
    List<Vehicle> findPublicVehiclesByStatusAndCategory(
            @Param("status") String status, @Param("categoryId") Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vehicle FROM Vehicle vehicle WHERE vehicle.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);
}
