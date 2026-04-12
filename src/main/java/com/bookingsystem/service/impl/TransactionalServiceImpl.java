package com.bookingsystem.service.impl;

import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.User;
import com.bookingsystem.exception.APIException;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.service.TransactionalService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.bookingsystem.security.utils.AuthUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionalServiceImpl implements TransactionalService {
    private final BookingRepository bookingRepository;

    @Value("${app.stripe.currency}")
    private String stripeCurrency;

    @Value("${app.stripe.minimum-amount}")
    private long stripeMinimumAmount;

    @Override
    @Transactional
    public String getCheckoutSession(Booking booking, String successUrl, String failureUrl) {
        User user = getCurrentUser();
        try {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();
            Customer customer = Customer.create(customerParams);

            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(failureUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(stripeCurrency)
                                                    .setUnitAmount(
                                                            Math.max(
                                                                    booking.getAmount()
                                                                            .multiply(BigDecimal.valueOf(100))
                                                                            .longValue(),
                                                                    stripeMinimumAmount))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(booking.getHotel().getName()
                                                                            + " : " + booking.getRoom().getType())
                                                                    .setDescription("Booking ID: " + booking.getId())
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(sessionParams);
            booking.setPaymentSessionId(session.getId());
            bookingRepository.save(booking);
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed: {}", e.getMessage());
            throw new APIException("Payment session creation failed. Please try again.");
        }
    }
}
