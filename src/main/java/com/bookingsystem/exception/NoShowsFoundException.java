package com.bookingsystem.exception;

public class NoShowsFoundException extends RuntimeException {
    public NoShowsFoundException(String message) {
        super(message);
    }
}