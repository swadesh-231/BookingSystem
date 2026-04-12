package com.bookingsystem.strategy;

import com.bookingsystem.entity.Inventory;
import com.bookingsystem.strategy.impl.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {

    private final PricingStrategy pricingChain;

    public PricingService() {
        PricingStrategy base = new BasePricingStrategy();
        base = new SurgePricingStrategy(base);
        base = new OccupancyPricingStrategy(base);
        base = new UrgencyPricingStrategy(base);
        base = new HolidayPricingStrategy(base);
        this.pricingChain = base;
    }

    public BigDecimal calculateDynamicPricing(Inventory inventory) {
        return pricingChain.calculatePrice(inventory);
    }

    public BigDecimal calculateTotalPrice(List<Inventory> inventoryList) {
        return inventoryList.stream()
                .map(this::calculateDynamicPricing)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
