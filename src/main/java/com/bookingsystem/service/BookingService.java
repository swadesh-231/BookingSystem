package com.bookingsystem.service;

import com.bookingsystem.dto.BookingRequest;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.GuestDto;

import java.util.List;

public interface BookingService {
    BookingResponse initialiseBooking(BookingRequest bookingRequest);
    BookingResponse addGuests(Long bookingId, List<GuestDto> guestList);
}
