package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponseDto {
    private Long bookingId;
    private Integer numberOfSeats;
    private Double price;
    private BookingStatus bookingStatus;
    private LocalDateTime bookingTime;
    private List<String> seatNumbers;
    private Long showId;
    private Long userId;
}
