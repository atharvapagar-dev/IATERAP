package com.adtech.iaterap.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "protocol_tags")
public class ProtocolTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String value;

    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lru_id", nullable = false)
    private Lru lru;

    public ProtocolTag() {
    }

    public ProtocolTag(String name, String value, String description, Lru lru) {
        this.name = name;
        this.value = value;
        this.description = description;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Lru getLru() {
        return lru;
    }

    public void setLru(Lru lru) {
        this.lru = lru;
    }
}
