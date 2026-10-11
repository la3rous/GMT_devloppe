package com.gmt.rental.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_notifications_quote_request", columnNames = "quote_request_id")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean lu;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_request_id", nullable = false)
    private QuoteRequest devis;

    public Notification() {
    }

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public QuoteRequest getDevis() {
        return devis;
    }

    public void setDevis(QuoteRequest devis) {
        this.devis = devis;
    }
}
