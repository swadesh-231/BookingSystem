package com.bookingsystem.strategy.impl;

import com.bookingsystem.entity.Inventory;
import com.bookingsystem.strategy.PricingStrategy;

import java.math.BigDecimal;

public class BasePricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}
