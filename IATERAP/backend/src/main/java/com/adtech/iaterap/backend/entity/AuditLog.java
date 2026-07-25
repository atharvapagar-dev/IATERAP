package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actor;

    private String action;

    private String entityName;

    private String entityId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String beforeState;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String afterState;

    private String metadata;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(
            String actor,
            String action,
            String entityName,
            String entityId,
            String beforeState,
            String afterState,
            String metadata
    ) {
        this.actor = actor;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.metadata = metadata;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public void setBeforeState(String beforeState) {
        this.beforeState = beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public void setAfterState(String afterState) {
        this.afterState = afterState;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
