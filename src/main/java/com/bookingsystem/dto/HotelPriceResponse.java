package com.bookingsystem.dto;

import com.bookingsystem.entity.HotelContact;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class HotelPriceResponse {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private HotelContact contact;
    private Double price;
}
