package com.bookingsystem.exception;

public class TheaterNotFoundException extends RuntimeException {
    public TheaterNotFoundException(Long id) {
        super("Theater not found with id: " + id);
    }
}