package com.bookingsystem.exception;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final String field;
    private final Object value;

    public UserNotFoundException(String field, Object value) {
        super(String.format("User not found with %s : %s", field, value));
        this.field = field;
        this.value = value;
    }

    public UserNotFoundException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }
}