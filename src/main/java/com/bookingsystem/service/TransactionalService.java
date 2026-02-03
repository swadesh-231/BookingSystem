package com.bookingsystem.service;

import com.bookingsystem.entity.Booking;

public interface TransactionalService {
    String getCheckoutSession(Booking booking, String successUrl, String failureUrl);
}
