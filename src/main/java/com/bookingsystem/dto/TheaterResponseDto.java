package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.ScreenType;
import lombok.*;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheaterResponseDto {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private ScreenType screenType;


//    private List<ShowResponseDto> shows;
}
