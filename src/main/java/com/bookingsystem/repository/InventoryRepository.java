package com.bookingsystem.repository;

import com.bookingsystem.entity.Inventory;
import com.bookingsystem.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    void deleteByRoom(Room room);
}
