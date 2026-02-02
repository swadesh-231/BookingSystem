package com.bookingsystem.dto;

import com.bookingsystem.entity.HotelContact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HotelSearchResponse {
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private HotelContact contact;
}
