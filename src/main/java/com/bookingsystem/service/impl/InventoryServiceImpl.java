package com.bookingsystem.service.impl;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.entity.Room;
import com.bookingsystem.repository.HotelPriceRepository;
import com.bookingsystem.repository.InventoryRepository;
import com.bookingsystem.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final HotelPriceRepository hotelPriceRepository;

    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for (; !today.isAfter(endDate); today = today.plusDays(1)) {
            if (!inventoryRepository.existsByRoomAndDate(room, today)) {
                Inventory inventory = Inventory.builder()
                        .hotel(room.getHotel())
                        .room(room)
                        .bookedCount(0)
                        .reservedCount(0)
                        .city(room.getHotel().getCity())
                        .date(today)
                        .price(room.getBasePrice())
                        .surgeFactor(BigDecimal.ONE)
                        .totalCount(room.getTotalCount())
                        .closed(false)
                        .build();
                inventoryRepository.save(inventory);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAllInventories(Room room) {
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceResponse> searchHotels(HotelSearchRequest hotelSearchRequest) {
        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount =
                ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate()) + 1;

        // business logic - 90 days
        Page<HotelPriceDto> hotelPage =
                hotelPriceRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                        hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate(), hotelSearchRequest.getRoomsCount(),
                        dateCount, pageable);

        return hotelPage.map(hotelPriceDto -> {
            HotelPriceResponse hotelPriceResponse = modelMapper.map(hotelPriceDto.getHotel(), HotelPriceResponse.class);
            hotelPriceResponse.setPrice(hotelPriceDto.getPrice());
            return hotelPriceResponse;
        });
    }
}
