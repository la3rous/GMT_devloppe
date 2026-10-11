package com.gmt.rental.repository;

import com.gmt.rental.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {

    List<VehicleImage> findByVehiculeId(Long vehiculeId);
}