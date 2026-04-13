package com.bookingsystem.strategy.impl;

import com.bookingsystem.entity.Inventory;
import com.bookingsystem.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy {
    private final PricingStrategy pricingStrategy;

    private static final java.util.Set<java.time.MonthDay> HOLIDAYS = java.util.Set.of(
            java.time.MonthDay.of(1, 1),   // New Year's Eve (assuming travel happens then)
            java.time.MonthDay.of(12, 24), // Christmas Eve
            java.time.MonthDay.of(12, 25), // Christmas Day
            java.time.MonthDay.of(12, 31)  // New Year's Eve
    );

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = pricingStrategy.calculatePrice(inventory);
        if (inventory.getDate() != null) {
            java.time.MonthDay dateToCheck = java.time.MonthDay.from(inventory.getDate());
            if (HOLIDAYS.contains(dateToCheck)) {
                price = price.multiply(BigDecimal.valueOf(1.25));
            }
        }
        return price;
    }
}