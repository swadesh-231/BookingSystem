package com.bookingsystem.service;

import com.bookingsystem.dto.BookingRequestDto;
import com.bookingsystem.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingRequestDto requestDto);
    void confirmBooking(Long bookingId);
    void cancelBooking(Long bookingId);
    BookingResponseDto getBookingById(Long bookingId);
    List<BookingResponseDto> getBookingsByUser(Long userId);
    List<BookingResponseDto> getBookingsByShow(Long showId);
}
