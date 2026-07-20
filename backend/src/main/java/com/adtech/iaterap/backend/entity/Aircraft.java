package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;

@Entity
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String manufacturer;

    public Aircraft() {}

    public Aircraft(String name, String manufacturer) {
        this.name = name;
        this.manufacturer = manufacturer;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
}
