package com.bookingsystem.strategy;

import com.bookingsystem.entity.Hotel;
import com.bookingsystem.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingUpdateService {
    private final HotelRepository hotelRepository;
    private final HotelPricingUpdateWorker hotelPricingUpdateWorker;

    @Scheduled(cron = "0 0 * * * *")
    public void updatePrice() {
        log.info("Starting scheduled price update");
        int page = 0;
        int batchSize = 100;
        AtomicInteger updated = new AtomicInteger(0);

        try {
            while (true) {
                Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batchSize));
                if (hotelPage.isEmpty()) {
                    break;
                }
                for (Hotel hotel : hotelPage.getContent()) {
                    try {
                        hotelPricingUpdateWorker.updateHotelPricesAsync(hotel);
                        updated.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Failed to submit price update for hotel {}: {}", hotel.getId(), e.getMessage());
                    }
                }
                page++;
            }
            log.info("Submitted scheduled price update for {} hotels", updated.get());
        } catch (Exception e) {
            log.error("Scheduled price update failed after processing {} hotels: {}", updated.get(), e.getMessage());
        }
    }
}
