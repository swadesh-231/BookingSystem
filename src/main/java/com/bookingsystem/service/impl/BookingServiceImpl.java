package com.bookingsystem.service.impl;

import com.bookingsystem.dto.BookingRequest;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.GuestDto;
import com.bookingsystem.entity.*;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.exception.BookingExpiredException;
import com.bookingsystem.exception.InvalidBookingStateException;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.RoomNotAvailableException;
import com.bookingsystem.repository.*;
import com.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public BookingResponse initialiseBooking(BookingRequest bookingRequest) {
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", bookingRequest.getHotelId()));

        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", bookingRequest.getRoomId()));
        List<Inventory> inventoryLists = inventoryRepository.findAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate()) + 1;
        if (inventoryLists.size() != daysCount) {
            throw new RoomNotAvailableException("Requested room is not available for the full date range");
        }
        for (Inventory inventory : inventoryLists) {
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryLists);
        // todo calculate dynamic pricing
        Booking booking = Booking.builder()
                .status(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .user(getCurrentUser())
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .build();
        bookingRepository.save(booking);
        return modelMapper.map(booking, BookingResponse.class);
    }

    @Override
    @Transactional
    public BookingResponse addGuests(Long bookingId, List<GuestDto> guestList) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (hasBookingExpired(booking)) {
            throw new BookingExpiredException("Booking has expired");
        }
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new InvalidBookingStateException("Booking is not under reserved state, cannot add guests");
        }
        for (GuestDto guestDto : guestList) {
            Guest guest = Guest.builder()
                    .name(guestDto.getName())
                    .gender(guestDto.getGender())
                    .dateOfBirth(guestDto.getDateOfBirth())
                    .user(getCurrentUser())
                    .build();
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }
        booking.setStatus(BookingStatus.GUEST_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingResponse.class);
    }

    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser() {
        User user = new User();
        user.setId(1L);
        return user;
    }
}
