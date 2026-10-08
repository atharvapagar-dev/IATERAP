package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Immutable synthetic event evidence used by the reliability-trend layer. */
@Entity
@Immutable
@Table(name = "event_log")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "synthetic_lru_instance_id", nullable = false, updatable = false)
    private Long syntheticLruInstanceId;

    @NotNull
    @Column(name = "lru_id", nullable = false, updatable = false)
    private Long lruId;

    @NotBlank
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @NotBlank
    @Column(nullable = false, updatable = false)
    private String severity = "INFO";

    @NotNull
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String payload;

    @NotBlank
    @Column(name = "generated_by", nullable = false, updatable = false)
    private String generatedBy = "SYSTEM";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected EventLog() { }

    public EventLog(Long syntheticLruInstanceId, Long lruId, String eventType,
                    String severity, LocalDateTime timestamp, String payload, String generatedBy) {
        this.syntheticLruInstanceId = syntheticLruInstanceId;
        this.lruId = lruId;
        this.eventType = eventType;
        this.severity = severity;
        this.timestamp = timestamp;
        this.payload = payload;
        this.generatedBy = generatedBy;
    }

    public Long getId() { return id; }
    public Long getSyntheticLruInstanceId() { return syntheticLruInstanceId; }
    public Long getLruId() { return lruId; }
    public String getEventType() { return eventType; }
    public String getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPayload() { return payload; }
    public String getGeneratedBy() { return generatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
