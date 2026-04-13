package com.bookingsystem.exception;

import com.bookingsystem.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .message("Validation failed")
                .status(false)
                .path(request.getRequestURI())
                .data(errors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(UserAlreadyExistsException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomNotAvailable(RoomNotAvailableException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(BookingExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingExpired(BookingExpiredException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.GONE, request);
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBookingState(InvalidBookingStateException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(UnAuthorisedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorised(UnAuthorisedException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return buildErrorResponse("Access Denied", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        return buildErrorResponse("Invalid email or password", HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<ApiResponse<Void>> handleAPIException(APIException e, HttpServletRequest request) {
        return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception: ", e);
        return buildErrorResponse("An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(String message, HttpStatus status, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message(message)
                .status(false)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
