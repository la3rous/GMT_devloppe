package com.gmt.rental.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_deliveries", uniqueConstraints = @UniqueConstraint(
        name = "uk_email_delivery_quote_recipient", columnNames = {"quote_request_id", "recipient"}))
public class EmailDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "quote_request_id", nullable = false)
    private Long quoteRequestId;
    @Column(nullable = false, length = 254)
    private String recipient;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;
    @Column(name = "last_error", length = 300)
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getQuoteRequestId() { return quoteRequestId; }
    public void setQuoteRequestId(Long value) { quoteRequestId = value; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String value) { recipient = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String value) { providerMessageId = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
}
