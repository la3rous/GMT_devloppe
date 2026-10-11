package com.gmt.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rental_history")
public class RentalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicleEntity;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "prix_paye", nullable = false, precision = 12, scale = 3)
    private BigDecimal prixPaye;

    @Column(nullable = false, length = 3)
    private String devise;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }
    public Vehicle getVehicleEntity() { return vehicleEntity; }
    public void setVehicleEntity(Vehicle vehicleEntity) { this.vehicleEntity = vehicleEntity; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public BigDecimal getPrixPaye() { return prixPaye; }
    public void setPrixPaye(BigDecimal prixPaye) { this.prixPaye = prixPaye; }
    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }
}
