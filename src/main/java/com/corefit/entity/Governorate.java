package com.corefit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "governorats")
@AllArgsConstructor
public class Governorate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;

    /// /////////////////
    public Governorate() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

