package com.bookingsystem.service;


import com.bookingsystem.dto.*;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.entity.Room;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {
    void initializeRoomForAYear(Room room);
    void deleteAllInventories(Room room);
    List<InventoryResponse> getAllInventoriesByRoom(Long roomId);
    Page<HotelPriceResponse> searchHotels(HotelSearchRequest hotelSearchRequest);
    void updateInventories(Long roomId, InventoryRequest inventoryRequest);
}
