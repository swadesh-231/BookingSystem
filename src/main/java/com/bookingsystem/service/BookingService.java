package com.bookingsystem.service;

import com.bookingsystem.dto.BookingRequest;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.GuestDto;
import com.bookingsystem.dto.HotelReport;
import com.bookingsystem.entity.enums.BookingStatus;
import com.stripe.model.Event;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    BookingResponse initialiseBooking(BookingRequest bookingRequest);
    BookingResponse addGuests(Long bookingId, List<GuestDto> guestList);
    String initiatePayments(Long bookingId);
    void capturePayment(Event event);
    void cancelBooking(Long bookingId);
    BookingStatus getBookingStatus(Long bookingId);
    List<BookingResponse> getAllBookingsByHotelId(Long hotelId);
    HotelReport getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate);
    List<BookingResponse> getMyBookings();
}
