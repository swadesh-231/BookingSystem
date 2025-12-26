package com.bookingsystem.entity;

import com.bookingsystem.entity.enums.ScreenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Theater {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long theaterId;
    private String name;
    private String location;
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private ScreenType screenType;

    @OneToMany(mappedBy = "theater", fetch = FetchType.LAZY)
    private List<Show> shows;

}
