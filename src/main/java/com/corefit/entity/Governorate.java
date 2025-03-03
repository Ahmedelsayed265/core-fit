package com.corefit.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "governorats")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Governorate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
}
