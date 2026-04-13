package com.bookingsystem.strategy;

import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.HotelPrice;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.repository.HotelPriceRepository;
import com.bookingsystem.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelPricingUpdateWorker {

    private final InventoryRepository inventoryRepository;
    private final HotelPriceRepository hotelPriceRepository;
    private final PricingService pricingService;

    @Async("pricingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateHotelPricesAsync(Hotel hotel) {
        try {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusYears(1);
            List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
            updateInventoryPrices(inventoryList);
            updateHotelMinPrice(hotel, inventoryList);
        } catch (Exception e) {
            log.error("Failed to update prices for hotel {}: {}", hotel.getId(), e.getMessage());
        }
    }

    private void updateInventoryPrices(List<Inventory> inventoryList) {
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        inventoryRepository.saveAll(inventoryList);
    }

    private void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList) {
        Map<LocalDate, BigDecimal> dailyMinPrices = inventoryList.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(BigDecimal.ZERO)));

        List<HotelPrice> hotelPrices = new ArrayList<>();
        dailyMinPrices.forEach((date, price) -> {
            HotelPrice hotelPrice = hotelPriceRepository.findByHotelAndDate(hotel, date)
                    .orElse(new HotelPrice(hotel, date));
            hotelPrice.setPrice(price);
            hotelPrices.add(hotelPrice);
        });
        hotelPriceRepository.saveAll(hotelPrices);
    }
}
