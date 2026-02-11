package com.bookingsystem.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Value("${server.stripe.secret.key}")
    private String stripeSecretKey;
}
