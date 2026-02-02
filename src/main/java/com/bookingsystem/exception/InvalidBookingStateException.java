package com.bookingsystem.exception;

public class InvalidBookingStateException extends APIException {

    public InvalidBookingStateException() {
        super("Booking is not in RESERVED state, cannot add guests");
    }

    public InvalidBookingStateException(String message) {
        super(message);
    }
}
