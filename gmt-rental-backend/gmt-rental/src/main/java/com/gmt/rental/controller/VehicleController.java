package com.gmt.rental.controller;

import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicules")
@CrossOrigin(origins = "http://localhost:5174")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    // GET /api/vehicules
    @GetMapping
    public List<Vehicle> getAllVehicles(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long categorieId) {

        return vehicleService.searchPublicVehicles(statut, categorieId);
    }

    // GET /api/vehicules/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {

        return vehicleService.getPublicVehicleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
