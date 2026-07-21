package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;

@Entity
public class Lru {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public Lru() {}

    public Lru(String name) {
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
