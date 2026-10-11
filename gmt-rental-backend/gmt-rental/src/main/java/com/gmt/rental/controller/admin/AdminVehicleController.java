package com.gmt.rental.controller.admin;

import com.gmt.rental.dto.admin.VehicleRequest;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.service.admin.AdminVehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicules")
public class AdminVehicleController {
    private final AdminVehicleService service;
    public AdminVehicleController(AdminVehicleService service) { this.service = service; }

    @GetMapping public List<Vehicle> getAll() { return service.findAll(); }
    @GetMapping("/{id}") public Vehicle getById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping public ResponseEntity<Vehicle> create(@RequestBody VehicleRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @PutMapping("/{id}") public Vehicle update(@PathVariable Long id, @RequestBody VehicleRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/statut") public Vehicle updateStatus(@PathVariable Long id, @RequestParam String statut) { return service.updateStatus(id, statut); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deactivate(@PathVariable Long id) { service.deactivate(id); }
}
