package com.bookingsystem.strategy;

import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.HotelPrice;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.repository.HotelPriceRepository;
import com.bookingsystem.repository.HotelRepository;
import com.bookingsystem.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PricingUpdateService {
    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelPriceRepository hotelPriceRepository;
    private final PricingService pricingService;

    @Scheduled(cron = "0 0 * * * *")
    public void updatePrice(){
        log.info("Starting scheduled price update");
        int page = 0;
        int batchSize = 100;
        int updated = 0;

        try {
            while(true) {
                Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batchSize));
                if(hotelPage.isEmpty()) {
                    break;
                }
                for (Hotel hotel : hotelPage.getContent()) {
                    try {
                        updateHotelPrices(hotel);
                        updated++;
                    } catch (Exception e) {
                        log.error("Failed to update prices for hotel {}: {}", hotel.getId(), e.getMessage());
                    }
                }
                page++;
            }
            log.info("Completed scheduled price update for {} hotels", updated);
        } catch (Exception e) {
            log.error("Scheduled price update failed after processing {} hotels: {}", updated, e.getMessage());
        }
    }
    private void updateHotelPrices(Hotel hotel) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);
        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
        updateInventoryPrices(inventoryList);
        updateHotelMinPrice(hotel, inventoryList, startDate, endDate);
    }
    private void updateInventoryPrices(List<Inventory> inventoryList) {
        inventoryList.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        inventoryRepository.saveAll(inventoryList);
    }
    private void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList, LocalDate startDate, LocalDate endDate) {
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
