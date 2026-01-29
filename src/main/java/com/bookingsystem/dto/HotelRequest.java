package com.bookingsystem.dto;

import com.bookingsystem.entity.HotelContact;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelRequest {
    @NotBlank(message = "Hotel name is required")
    private String name;
    @NotBlank(message = "City is required")
    private String city;
    private String[] photos;
    private String[] amenities;
    @NotNull(message = "Contact information is required")
    @Valid
    private HotelContact contact;
}
