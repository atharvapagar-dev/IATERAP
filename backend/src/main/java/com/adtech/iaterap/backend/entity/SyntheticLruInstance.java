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

/** A clearly-labelled simulated physical instance of an engineering LRU. */
@Entity
@Table(name = "synthetic_lru_instance")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SyntheticLruInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "lru_id", nullable = false)
    private Long lruId;

    @NotBlank
    @Column(name = "instance_code", nullable = false, unique = true)
    private String instanceCode;

    @NotBlank
    @Column(name = "lifecycle_status", nullable = false)
    private String lifecycleStatus = "ACTIVE";

    @Column(name = "generator_seed")
    private Long generatorSeed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    public SyntheticLruInstance() { }

    public SyntheticLruInstance(Long lruId, String instanceCode, Long generatorSeed) {
        this.lruId = lruId;
        this.instanceCode = instanceCode;
        this.generatorSeed = generatorSeed;
    }

    public Long getId() { return id; }
    public Long getLruId() { return lruId; }
    public void setLruId(Long lruId) { this.lruId = lruId; }
    public String getInstanceCode() { return instanceCode; }
    public void setInstanceCode(String instanceCode) { this.instanceCode = instanceCode; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(String lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public Long getGeneratorSeed() { return generatorSeed; }
    public void setGeneratorSeed(Long generatorSeed) { this.generatorSeed = generatorSeed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRetiredAt() { return retiredAt; }
    public void setRetiredAt(LocalDateTime retiredAt) { this.retiredAt = retiredAt; }
}
