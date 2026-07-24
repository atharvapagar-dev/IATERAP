package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "signal_ranges")
public class SignalRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private BigDecimal minimumValue;

    @NotNull
    private BigDecimal maximumValue;

    private String severity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signal_library_id", nullable = false)
    private SignalLibrary signalLibrary;

    public SignalRange() {
    }

    public SignalRange(
            String name,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            String severity,
            SignalLibrary signalLibrary
    ) {
        this.name = name;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.severity = severity;
        this.signalLibrary = signalLibrary;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(BigDecimal minimumValue) {
        this.minimumValue = minimumValue;
    }

    public BigDecimal getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(BigDecimal maximumValue) {
        this.maximumValue = maximumValue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public SignalLibrary getSignalLibrary() {
        return signalLibrary;
    }

    public void setSignalLibrary(SignalLibrary signalLibrary) {
        this.signalLibrary = signalLibrary;
    }
}
