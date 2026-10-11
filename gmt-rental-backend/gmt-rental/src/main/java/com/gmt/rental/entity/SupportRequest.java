package com.gmt.rental.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_support_requests", indexes = @Index(name = "idx_support_created_at", columnList = "created_at"))
public class SupportRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_name", nullable = false, length = 160)
    private String senderName;

    @Column(name = "sender_email", nullable = false, length = 320)
    private String senderEmail;

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
