package com.bookingsystem.service.impl;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.HotelContact;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.Role;
import com.bookingsystem.exception.APIException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock private HotelRepository hotelRepository;
    @Mock private InventoryService inventoryService;
    @Mock private RoomRepository roomRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private User owner;
    private Hotel hotel;
    private HotelContact contact;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).name("Hotel Owner").email("owner@test.com")
                .roles(Set.of(Role.HOTEL_MANAGER)).build();

        contact = new HotelContact();
        contact.setAddress("123 Main St");
        contact.setPhoneNumber("+911234567890");
        contact.setEmail("hotel@test.com");
        contact.setLocation("Downtown");

        hotel = Hotel.builder()
                .id(1L).name("Grand Hotel").city("Mumbai")
                .active(false).owner(owner).contact(contact)
                .rooms(new ArrayList<>()).build();
    }

    @Nested
    class CreateNewHotel {

        @Test
        void shouldCreateHotelSuccessfully() {
            HotelRequest request = HotelRequest.builder()
                    .name("Grand Hotel").city("Mumbai").contact(contact).build();
            HotelResponse response = HotelResponse.builder()
                    .id(1L).name("Grand Hotel").city("Mumbai").active(false).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.existsByNameAndCity("Grand Hotel", "Mumbai")).thenReturn(false);
                when(modelMapper.map(request, Hotel.class)).thenReturn(hotel);
                when(modelMapper.map(any(Hotel.class), eq(HotelResponse.class))).thenReturn(response);

                HotelResponse result = hotelService.createNewHotel(request);

                assertThat(result.getName()).isEqualTo("Grand Hotel");
                assertThat(result.getActive()).isFalse();
                verify(hotelRepository).save(any(Hotel.class));
            }
        }

        @Test
        void shouldThrowWhenHotelAlreadyExists() {
            HotelRequest request = HotelRequest.builder()
                    .name("Grand Hotel").city("Mumbai").build();

            when(hotelRepository.existsByNameAndCity("Grand Hotel", "Mumbai")).thenReturn(true);

            assertThatThrownBy(() -> hotelService.createNewHotel(request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    class GetHotelById {

        @Test
        void shouldReturnHotelForOwner() {
            HotelResponse response = HotelResponse.builder().id(1L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(modelMapper.map(hotel, HotelResponse.class)).thenReturn(response);

                HotelResponse result = hotelService.getHotelById(1L);

                assertThat(result.getId()).isEqualTo(1L);
            }
        }

        @Test
        void shouldThrowWhenNotOwner() {
            User otherUser = User.builder().id(2L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(otherUser);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

                assertThatThrownBy(() -> hotelService.getHotelById(1L))
                        .isInstanceOf(UnAuthorisedException.class);
            }
        }

        @Test
        void shouldThrowWhenHotelNotFound() {
            when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.getHotelById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class FindAllHotelsPaginated {

        @Test
        void shouldReturnPaginatedOwnerHotels() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Hotel> hotelPage = new PageImpl<>(List.of(hotel), pageable, 1);
            HotelResponse response = HotelResponse.builder().id(1L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findByOwner(owner, pageable)).thenReturn(hotelPage);
                when(modelMapper.map(any(Hotel.class), eq(HotelResponse.class))).thenReturn(response);

                Page<HotelResponse> result = hotelService.findAllHotels(pageable);

                assertThat(result.getTotalElements()).isEqualTo(1);
            }
        }
    }

    @Nested
    class UpdateHotelById {

        @Test
        void shouldUpdateHotelSuccessfully() {
            HotelRequest request = HotelRequest.builder()
                    .name("Updated Hotel").city("Delhi").contact(contact).build();
            HotelResponse response = HotelResponse.builder()
                    .id(1L).name("Updated Hotel").city("Delhi").build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
                when(modelMapper.map(any(Hotel.class), eq(HotelResponse.class))).thenReturn(response);

                HotelResponse result = hotelService.updateHotelById(1L, request);

                assertThat(result.getName()).isEqualTo("Updated Hotel");
            }
        }
    }

    @Nested
    class DeleteHotelById {

        @Test
        void shouldDeleteHotelAndCleanup() {
            Room room1 = Room.builder().id(1L).hotel(hotel).build();
            Room room2 = Room.builder().id(2L).hotel(hotel).build();
            hotel.setRooms(List.of(room1, room2));

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

                hotelService.deleteHotelById(1L);

                verify(inventoryService, times(2)).deleteAllInventories(any(Room.class));
                verify(roomRepository, times(2)).deleteById(anyLong());
                verify(hotelRepository).deleteById(1L);
            }
        }
    }

    @Nested
    class UpdateHotelStatus {

        @Test
        void shouldActivateHotelAndInitializeInventory() {
            Room room = Room.builder().id(1L).hotel(hotel)
                    .basePrice(new BigDecimal("5000")).totalCount(10).build();
            hotel.setRooms(List.of(room));
            HotelResponse response = HotelResponse.builder().id(1L).active(true).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
                when(modelMapper.map(any(Hotel.class), eq(HotelResponse.class))).thenReturn(response);

                HotelResponse result = hotelService.updateHotelStatus(1L, true);

                verify(inventoryService).initializeRoomForAYear(room);
                assertThat(result.getActive()).isTrue();
            }
        }

        @Test
        void shouldDeactivateHotelAndDeleteInventory() {
            hotel.setActive(true);
            Room room = Room.builder().id(1L).hotel(hotel).build();
            hotel.setRooms(List.of(room));
            HotelResponse response = HotelResponse.builder().id(1L).active(false).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
                when(modelMapper.map(any(Hotel.class), eq(HotelResponse.class))).thenReturn(response);

                hotelService.updateHotelStatus(1L, false);

                verify(inventoryService).deleteAllInventories(room);
            }
        }
    }

    @Nested
    class GetHotelInfoById {

        @Test
        void shouldReturnHotelInfoWithRooms() {
            Room room = Room.builder().id(1L).hotel(hotel).type("DELUXE")
                    .basePrice(new BigDecimal("5000")).build();
            hotel.setRooms(List.of(room));

            HotelSearchResponse searchResponse = HotelSearchResponse.builder()
                    .name("Grand Hotel").city("Mumbai").build();
            RoomResponse roomResponse = RoomResponse.builder()
                    .id(1L).type("DELUXE").build();

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(modelMapper.map(hotel, HotelSearchResponse.class)).thenReturn(searchResponse);
            when(modelMapper.map(room, RoomResponse.class)).thenReturn(roomResponse);

            HotelInfoDto result = hotelService.getHotelInfoById(1L);

            assertThat(result.getSearchResponse().getName()).isEqualTo("Grand Hotel");
            assertThat(result.getRoomResponses()).hasSize(1);
        }
    }
}
