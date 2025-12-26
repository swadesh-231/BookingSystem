package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.ScreenType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheaterRequestDto {
    @NotBlank(message = "Theater name is required")
    @Size(min = 2, max = 100, message = "Theater name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Capacity is required")
    @Min(value = 10, message = "Capacity must be at least 10")
    private Integer capacity;

    @NotNull(message = "Screen type is required")
    private ScreenType screenType;

}
