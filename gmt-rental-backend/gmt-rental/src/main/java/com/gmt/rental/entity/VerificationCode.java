package com.gmt.rental.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "verification_codes", indexes = {
    @Index(name = "idx_verification_email_type", columnList = "email, type")
})
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationCodeType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    public VerificationCode() {
    }

    public VerificationCode(String email, String codeHash, VerificationCodeType type, Instant expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.used = false;
        this.attempts = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public VerificationCodeType getType() {
        return type;
    }

    public void setType(VerificationCodeType type) {
        this.type = type;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}

