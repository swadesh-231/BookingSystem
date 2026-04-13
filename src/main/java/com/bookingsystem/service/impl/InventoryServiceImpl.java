package com.bookingsystem.service.impl;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.exception.APIException;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.UnAuthorisedException;
import com.bookingsystem.repository.HotelPriceRepository;
import com.bookingsystem.repository.InventoryRepository;
import com.bookingsystem.repository.RoomRepository;
import com.bookingsystem.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bookingsystem.config.CacheConfig.HOTEL_SEARCH;

import static com.bookingsystem.security.utils.AuthUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final HotelPriceRepository hotelPriceRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        // Single query instead of 365+ individual existsByRoomAndDate() calls
        Set<LocalDate> existingDates = inventoryRepository.findExistingDatesByRoomAndDateRange(room, today, endDate);

        List<Inventory> batch = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!existingDates.contains(date)) {
                Inventory inventory = Inventory.builder()
                        .hotel(room.getHotel())
                        .room(room)
                        .bookedCount(0)
                        .reservedCount(0)
                        .city(room.getHotel().getCity())
                        .date(date)
                        .price(room.getBasePrice())
                        .surgeFactor(BigDecimal.ONE)
                        .totalCount(room.getTotalCount())
                        .closed(false)
                        .build();
                batch.add(inventory);
            }

            if (batch.size() >= 100) {
                inventoryRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            inventoryRepository.saveAll(batch);
        }
        log.info("Initialized inventory for room {} in hotel {}", room.getId(), room.getHotel().getId());
    }

    @Override
    @Transactional
    public void deleteAllInventories(Room room) {
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventoriesByRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        User user = getCurrentUser();
        if (!user.equals(room.getHotel().getOwner())) {
            throw new UnAuthorisedException("You are not the owner of this room's hotel");
        }

        return inventoryRepository.findByRoomOrderByDate(room).stream()
                .map(element -> modelMapper.map(element, InventoryResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = HOTEL_SEARCH, key = "#hotelSearchRequest.city + '-' + #hotelSearchRequest.startDate + '-' + #hotelSearchRequest.endDate + '-' + #hotelSearchRequest.roomsCount + '-' + #hotelSearchRequest.page + '-' + #hotelSearchRequest.size")
    public Page<HotelPriceResponse> searchHotels(HotelSearchRequest hotelSearchRequest) {
        if (hotelSearchRequest.getStartDate() == null || hotelSearchRequest.getEndDate() == null) {
            throw new APIException("Start date and end date are required for search");
        }
        if (!hotelSearchRequest.getEndDate().isAfter(hotelSearchRequest.getStartDate())) {
            throw new APIException("End date must be after start date");
        }

        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate()) + 1;

        Page<HotelPriceDto> hotelPage = hotelPriceRepository.findHotelsWithAvailableInventory(
                hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate(),
                hotelSearchRequest.getRoomsCount(),
                dateCount,
                pageable);

        return hotelPage.map(hotelPriceDto -> {
            HotelPriceResponse hotelPriceResponse = modelMapper.map(hotelPriceDto.getHotel(), HotelPriceResponse.class);
            hotelPriceResponse.setPrice(hotelPriceDto.getPrice());
            return hotelPriceResponse;
        });
    }

    @Override
    @Transactional
    public void updateInventories(Long roomId, InventoryRequest inventoryRequest) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        User user = getCurrentUser();
        if (!user.equals(room.getHotel().getOwner())) {
            throw new UnAuthorisedException("You are not the owner of this room's hotel");
        }

        if (inventoryRequest.getStartDate() == null || inventoryRequest.getEndDate() == null) {
            throw new APIException("Start date and end date are required");
        }

        List<Inventory> inventories = inventoryRepository.findByRoomAndDateBetweenOrderByDate(
                room, inventoryRequest.getStartDate(), inventoryRequest.getEndDate());

        for (Inventory inventory : inventories) {
            if (inventoryRequest.getSurgeFactor() != null) {
                inventory.setSurgeFactor(inventoryRequest.getSurgeFactor());
            }
            if (inventoryRequest.getClosed() != null) {
                inventory.setClosed(inventoryRequest.getClosed());
            }
        }
        inventoryRepository.saveAll(inventories);
    }
}
