package com.bookingsystem.service.impl;

import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.User;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.service.TransactionalService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionalServiceImpl implements TransactionalService {
        private final BookingRepository bookingRepository;

        @Override
        public String getCheckoutSession(Booking booking, String successUrl, String failureUrl) {
                User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                                .getPrincipal();
                try {
                        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                                        .setName(user.getName())
                                        .setEmail(user.getEmail())
                                        .build();
                        Customer customer = Customer.create(customerParams);
                        SessionCreateParams sessionParams = SessionCreateParams.builder()
                                        .setMode(SessionCreateParams.Mode.PAYMENT)
                                        .setBillingAddressCollection(
                                                        SessionCreateParams.BillingAddressCollection.REQUIRED)
                                        .setCustomer(customer.getId())
                                        .setSuccessUrl(successUrl)
                                        .setCancelUrl(failureUrl)
                                        .addLineItem(
                                                        SessionCreateParams.LineItem.builder()
                                                                        .setQuantity(1L)
                                                                        .setPriceData(
                                                                                        SessionCreateParams.LineItem.PriceData
                                                                                                        .builder()
                                                                                                        .setCurrency("inr")
                                                                                                        .setUnitAmount(Math
                                                                                                                        .max(booking.getAmount()
                                                                                                                                        .multiply(BigDecimal
                                                                                                                                                        .valueOf(100))
                                                                                                                                        .longValue(),
                                                                                                                                        5000L))
                                                                                                        .setProductData(
                                                                                                                        SessionCreateParams.LineItem.PriceData.ProductData
                                                                                                                                        .builder()
                                                                                                                                        .setName(booking.getHotel()
                                                                                                                                                        .getName()
                                                                                                                                                        + " : "
                                                                                                                                                        + booking.getRoom()
                                                                                                                                                                        .getType())
                                                                                                                                        .setDescription("Booking ID: "
                                                                                                                                                        + booking.getId())
                                                                                                                                        .build())
                                                                                                        .build())
                                                                        .build())
                                        .build();
                        Session session = Session.create(sessionParams);

                        booking.setPaymentSessionId(session.getId());
                        bookingRepository.save(booking);
                        return session.getUrl();
                } catch (StripeException e) {
                        throw new RuntimeException(e);
                }
        }
}
