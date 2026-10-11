package com.gmt.rental.service.admin;

import com.gmt.rental.dto.admin.VehicleRequest;
import com.gmt.rental.entity.Category;
import com.gmt.rental.entity.Vehicle;
import com.gmt.rental.repository.CategoryRepository;
import com.gmt.rental.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminVehicleService {
    private final VehicleRepository vehicles;
    private final CategoryRepository categories;

    public AdminVehicleService(VehicleRepository vehicles, CategoryRepository categories) {
        this.vehicles = vehicles;
        this.categories = categories;
    }

    public List<Vehicle> findAll() { return vehicles.findAll(); }

    public Vehicle findById(Long id) {
        return vehicles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable"));
    }

    public Vehicle create(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        apply(vehicle, request);
        return vehicles.save(vehicle);
    }

    public Vehicle update(Long id, VehicleRequest request) {
        Vehicle vehicle = findById(id);
        apply(vehicle, request);
        return vehicles.save(vehicle);
    }

    public Vehicle updateStatus(Long id, String status) {
        if (!List.of("ACTIF", "INACTIF").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de véhicule invalide");
        }
        Vehicle vehicle = findById(id);
        vehicle.setStatut(status);
        return vehicles.save(vehicle);
    }

    public void deactivate(Long id) { updateStatus(id, "INACTIF"); }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        if (request == null || request.marque() == null || request.marque().isBlank()
                || request.modele() == null || request.modele().isBlank() || request.prixParJour() == null
                || request.categorieId() == null || request.places() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marque, modèle, prix par jour, places et catégorie sont obligatoires");
        }
        if (request.prixParJour().signum() < 0 || request.places() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le prix doit être positif et le nombre de places supérieur à zéro");
        }
        String status = request.statut() == null || request.statut().isBlank() ? "ACTIF" : request.statut().trim().toUpperCase();
        if (!List.of("ACTIF", "INACTIF").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut de véhicule invalide");
        }
        vehicle.setMarque(request.marque().trim());
        vehicle.setModele(request.modele().trim());
        vehicle.setAnnee(request.annee());
        vehicle.setPlaces(request.places());
        vehicle.setTransmission(request.transmission());
        vehicle.setCarburant(request.carburant());
        vehicle.setPrixParJour(request.prixParJour());
        vehicle.setDescription(request.description());
        vehicle.setImage(request.image());
        vehicle.setStatut(status);
        Category category = categories.findById(request.categorieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catégorie introuvable"));
        vehicle.setCategorie(category);
    }
}
