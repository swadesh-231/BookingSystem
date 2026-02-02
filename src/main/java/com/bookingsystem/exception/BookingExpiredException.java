package com.bookingsystem.exception;

public class BookingExpiredException extends APIException {

    public BookingExpiredException() {
        super("Booking has already expired");
    }

    public BookingExpiredException(String message) {
        super(message);
    }
}
