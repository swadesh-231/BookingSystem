package com.bookingsystem.dto;

import com.bookingsystem.entity.Hotel;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class HotelPriceDto {
    private Hotel hotel;
    private Double price;
}
