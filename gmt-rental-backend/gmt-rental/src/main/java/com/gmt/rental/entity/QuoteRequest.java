package com.gmt.rental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_requests")
public class QuoteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String prenom;

    private String email;

    private String telephone;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicule;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "lieu_depart")
    private String lieuPriseEnCharge;

    @Column(name = "lieu_retour")
    private String lieuRestitution;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "prix_total")
    private BigDecimal prixTotal;

    @ColumnTransformer(read = "upper(statut)", write = "lower(?)")
    private String statut;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "idempotency_key", unique = true, length = 80)
    private String idempotencyKey;

    public QuoteRequest() {
    }

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();

        if (statut == null) {
            statut = "NOUVELLE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Vehicle getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicle vehicule) {
        this.vehicule = vehicule;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieuPriseEnCharge() {
        return lieuPriseEnCharge;
    }

    public void setLieuPriseEnCharge(String lieuPriseEnCharge) {
        this.lieuPriseEnCharge = lieuPriseEnCharge;
    }

    public String getLieuRestitution() {
        return lieuRestitution;
    }

    public void setLieuRestitution(String lieuRestitution) {
        this.lieuRestitution = lieuRestitution;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getPrixTotal() {
        return prixTotal;
    }

    public void setPrixTotal(BigDecimal prixTotal) {
        this.prixTotal = prixTotal;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
