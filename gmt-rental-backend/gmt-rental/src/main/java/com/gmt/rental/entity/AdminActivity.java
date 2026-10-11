package com.gmt.rental.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_activity", indexes = {
        @Index(name = "idx_admin_activity_time", columnList = "occurred_at"),
        @Index(name = "idx_admin_activity_module", columnList = "module")
})
public class AdminActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_email", length = 320)
    private String actorEmail;

    @Column(name = "target_label", length = 320)
    private String targetLabel;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 64)
    private String module;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 16)
    private String result;

    @PrePersist
    protected void onCreate() { if (occurredAt == null) occurredAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
    public String getTargetLabel() { return targetLabel; }
    public void setTargetLabel(String targetLabel) { this.targetLabel = targetLabel; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
