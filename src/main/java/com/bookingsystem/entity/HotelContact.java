package com.bookingsystem.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class HotelContact {
    private String address;
    private String phoneNumber;
    private String email;
    private String location;
}
