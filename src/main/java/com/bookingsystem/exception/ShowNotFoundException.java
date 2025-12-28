package com.bookingsystem.exception;

public class ShowNotFoundException extends RuntimeException {
    public ShowNotFoundException(Long id) {
        super("Show not found with id: " + id);
    }
}