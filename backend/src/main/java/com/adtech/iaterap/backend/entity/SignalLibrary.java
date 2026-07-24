package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "signal_libraries")
public class SignalLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String description;

    private String engineeringUnit;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interface_id", nullable = false)
    private Interface interfaceEntity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lru_id", nullable = false)
    private Lru lru;

    public SignalLibrary() {
    }

    public SignalLibrary(
            String name,
            String description,
            String engineeringUnit,
            Interface interfaceEntity,
            Lru lru
    ) {
        this.name = name;
        this.description = description;
        this.engineeringUnit = engineeringUnit;
        this.interfaceEntity = interfaceEntity;
        this.lru = lru;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEngineeringUnit() {
        return engineeringUnit;
    }

    public void setEngineeringUnit(String engineeringUnit) {
        this.engineeringUnit = engineeringUnit;
    }

    public Interface getInterfaceEntity() {
        return interfaceEntity;
    }

    public void setInterfaceEntity(Interface interfaceEntity) {
        this.interfaceEntity = interfaceEntity;
    }

    public Lru getLru() {
        return lru;
    }

    public void setLru(Lru lru) {
        this.lru = lru;
    }
}
