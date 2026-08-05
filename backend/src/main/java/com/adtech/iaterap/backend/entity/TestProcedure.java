package com.adtech.iaterap.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_procedures")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TestProcedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "lru_id", nullable = false)
    private Lru lru;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotBlank
    @Column(name = "approval_status", nullable = false)
    private String approvalStatus = "DRAFT";

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public TestProcedure() {}

    public Long getId() { return id; }
    public Lru getLru() { return lru; }
    public void setLru(Lru lru) { this.lru = lru; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String v) { this.approvalStatus = v; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer v) { this.versionNumber = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}