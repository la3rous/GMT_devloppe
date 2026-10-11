package com.gmt.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taux_eur_dt", nullable = false)
    private BigDecimal tauxEurDt;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getTauxEurDt() { return tauxEurDt; }
    public void setTauxEurDt(BigDecimal tauxEurDt) { this.tauxEurDt = tauxEurDt; }
    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
