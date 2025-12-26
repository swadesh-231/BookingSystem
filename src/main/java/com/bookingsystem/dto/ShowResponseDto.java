package com.bookingsystem.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShowResponseDto {

    private Long showId;
    private LocalDateTime showTime;
    private Double price;

    // Movie info (flattened)
    private Long movieId;
    private String movieName;

    // Theater info (flattened)
    private Long theaterId;
    private String theaterName;
    private String location;

    // Optional booking info
    private List<Long> bookingIds;
}
