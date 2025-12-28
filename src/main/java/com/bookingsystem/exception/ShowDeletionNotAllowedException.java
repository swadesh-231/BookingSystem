package com.bookingsystem.exception;

public class ShowDeletionNotAllowedException extends RuntimeException {
    public ShowDeletionNotAllowedException(String message) {
        super(message);
    }
}