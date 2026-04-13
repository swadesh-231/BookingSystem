package com.bookingsystem.service.impl;

import com.bookingsystem.dto.RoomRequest;
import com.bookingsystem.dto.RoomResponse;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.Role;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.UnAuthorisedException;
import com.bookingsystem.repository.HotelRepository;
import com.bookingsystem.repository.RoomRepository;
import com.bookingsystem.security.utils.AuthUtils;
import com.bookingsystem.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private InventoryService inventoryService;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private RoomServiceImpl roomService;

    private User owner;
    private Hotel hotel;
    private Room room;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).name("Owner").email("owner@test.com")
                .roles(Set.of(Role.HOTEL_MANAGER)).build();

        hotel = Hotel.builder()
                .id(1L).name("Test Hotel").city("Mumbai")
                .active(true).owner(owner).build();

        room = Room.builder()
                .id(1L).hotel(hotel).type("DELUXE")
                .basePrice(new BigDecimal("5000.00"))
                .totalCount(10).capacity(2).build();
    }

    @Nested
    class CreateNewRoom {

        @Test
        void shouldCreateRoomAndInitInventoryWhenHotelActive() {
            RoomRequest request = RoomRequest.builder()
                    .type("DELUXE").basePrice(new BigDecimal("5000"))
                    .totalCount(10).capacity(2).build();
            RoomResponse response = RoomResponse.builder().id(1L).type("DELUXE").build();

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(modelMapper.map(request, Room.class)).thenReturn(room);
            when(modelMapper.map(room, RoomResponse.class)).thenReturn(response);

            RoomResponse result = roomService.createNewRoom(1L, request);

            assertThat(result.getType()).isEqualTo("DELUXE");
            verify(roomRepository).save(room);
            verify(inventoryService).initializeRoomForAYear(room);
        }

        @Test
        void shouldCreateRoomWithoutInventoryWhenHotelInactive() {
            hotel.setActive(false);
            RoomRequest request = RoomRequest.builder()
                    .type("STANDARD").basePrice(new BigDecimal("3000"))
                    .totalCount(5).capacity(2).build();
            RoomResponse response = RoomResponse.builder().id(1L).type("STANDARD").build();

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(modelMapper.map(request, Room.class)).thenReturn(room);
            when(modelMapper.map(room, RoomResponse.class)).thenReturn(response);

            roomService.createNewRoom(1L, request);

            verify(roomRepository).save(room);
            verify(inventoryService, never()).initializeRoomForAYear(any());
        }

        @Test
        void shouldThrowWhenHotelNotFound() {
            RoomRequest request = RoomRequest.builder().type("DELUXE").build();
            when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.createNewRoom(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetAllRoomsInHotel {

        @Test
        void shouldReturnAllRooms() {
            hotel.setRooms(List.of(room));
            RoomResponse response = RoomResponse.builder().id(1L).build();

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(modelMapper.map(room, RoomResponse.class)).thenReturn(response);

            List<RoomResponse> result = roomService.getAllRoomsInHotel(1L);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    class GetRoomById {

        @Test
        void shouldReturnRoom() {
            RoomResponse response = RoomResponse.builder().id(1L).build();

            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(modelMapper.map(room, RoomResponse.class)).thenReturn(response);

            RoomResponse result = roomService.getRoomById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomService.getRoomById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DeleteRoomById {

        @Test
        void shouldDeleteRoomAndCleanupInventory() {
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

            roomService.deleteRoomById(1L);

            verify(inventoryService).deleteAllInventories(room);
            verify(roomRepository).delete(room);
        }
    }

    @Nested
    class UpdateRoom {

        @Test
        void shouldUpdateRoomSuccessfully() {
            RoomRequest request = RoomRequest.builder()
                    .type("SUITE").basePrice(new BigDecimal("8000"))
                    .totalCount(5).capacity(4).build();
            RoomResponse response = RoomResponse.builder().id(1L).type("SUITE").build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(modelMapper.map(room, RoomResponse.class)).thenReturn(response);

                RoomResponse result = roomService.updateRoom(1L, 1L, request);

                assertThat(result.getType()).isEqualTo("SUITE");
                verify(roomRepository).save(room);
            }
        }

        @Test
        void shouldThrowWhenNotOwner() {
            User otherUser = User.builder().id(2L).build();
            RoomRequest request = RoomRequest.builder().type("SUITE").build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(otherUser);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

                assertThatThrownBy(() -> roomService.updateRoom(1L, 1L, request))
                        .isInstanceOf(UnAuthorisedException.class);
            }
        }

        @Test
        void shouldThrowWhenRoomDoesNotBelongToHotel() {
            Hotel otherHotel = Hotel.builder().id(2L).build();
            Room otherRoom = Room.builder().id(2L).hotel(otherHotel).build();
            RoomRequest request = RoomRequest.builder().type("SUITE").build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(roomRepository.findById(2L)).thenReturn(Optional.of(otherRoom));

                assertThatThrownBy(() -> roomService.updateRoom(1L, 2L, request))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
        }
    }
}
