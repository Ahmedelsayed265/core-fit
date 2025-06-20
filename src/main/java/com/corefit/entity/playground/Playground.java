package com.corefit.entity.playground;

import com.corefit.entity.City;
import com.corefit.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "playgrounds")
public class Playground {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(nullable = false)
    private City city;

    private String address;

    private LocalTime morningShiftStart;

    private LocalTime morningShiftEnd;

    private LocalTime nightShiftStart;

    private LocalTime nightShiftEnd;

    @Column(nullable = false)
    private Double bookingPrice;

    @Builder.Default
    private boolean hasExtraPrice = false;

    private double extraNightPrice;

    @Column(nullable = false)
    private Integer teamMembers;

    @Builder.Default
    @Column(nullable = false)
    private boolean isOpened = true;

    @ElementCollection
    @CollectionTable(name = "playgroud_images", joinColumns = @JoinColumn(name = "playground_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    @OrderColumn(name = "image_order")
    private List<String> images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "playground", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PlaygroundRate> rates;

    private int avgRate;

    @Transient
    private boolean isFavourite;
}