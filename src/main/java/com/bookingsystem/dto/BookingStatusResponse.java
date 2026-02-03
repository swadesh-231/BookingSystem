package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingStatusResponse {
    private BookingStatus bookingStatus;
}
