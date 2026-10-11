package com.gmt.rental.dto;

import java.time.LocalDate;

public class QuoteRequestDTO {

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    private Long vehiculeId;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private String lieuPriseEnCharge;
    private String lieuRestitution;

    private String message;

    public QuoteRequestDTO() {
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

    public Long getVehiculeId() {
        return vehiculeId;
    }

    public void setVehiculeId(Long vehiculeId) {
        this.vehiculeId = vehiculeId;
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
}