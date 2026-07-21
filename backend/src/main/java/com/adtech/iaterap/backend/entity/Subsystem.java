package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;

@Entity
public class Subsystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public Subsystem() {}

    public Subsystem(String name) {
        this.name = name;
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
}