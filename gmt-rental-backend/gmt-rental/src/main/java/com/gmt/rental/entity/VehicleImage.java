package com.gmt.rental.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_images")
public class VehicleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicule;

    @Column(name = "chemin_url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "image_principale")
    private Boolean principale = false;

    public VehicleImage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicle vehicule) {
        this.vehicule = vehicule;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getPrincipale() {
        return principale;
    }

    public void setPrincipale(Boolean principale) {
        this.principale = principale;
    }
}
