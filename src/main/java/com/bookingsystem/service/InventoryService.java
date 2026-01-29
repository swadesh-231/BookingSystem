package com.bookingsystem.service;

import com.bookingsystem.entity.Room;

public interface InventoryService {
    void initializeRoomForAYear(Room room);
    void deleteAllInventories(Room room);
}
