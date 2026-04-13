package com.bookingsystem.service.impl;

import com.bookingsystem.dto.*;
import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.entity.Room;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.Role;
import com.bookingsystem.exception.APIException;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.UnAuthorisedException;
import com.bookingsystem.repository.HotelPriceRepository;
import com.bookingsystem.repository.InventoryRepository;
import com.bookingsystem.repository.RoomRepository;
import com.bookingsystem.security.utils.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private HotelPriceRepository hotelPriceRepository;
    @Mock private RoomRepository roomRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Captor
    private ArgumentCaptor<List<Inventory>> inventoryListCaptor;

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
                .basePrice(new BigDecimal("5000.00")).totalCount(10).capacity(2)
                .build();
    }

    @Nested
    class InitializeRoomForAYear {

        @Test
        void shouldCreateInventoryForAllDaysWhenNoneExist() {
            when(inventoryRepository.findExistingDatesByRoomAndDateRange(eq(room), any(), any()))
                    .thenReturn(Collections.emptySet());

            inventoryService.initializeRoomForAYear(room);

            verify(inventoryRepository, atLeastOnce()).saveAll(inventoryListCaptor.capture());

            int totalSaved = inventoryListCaptor.getAllValues().stream()
                    .mapToInt(List::size).sum();

            // 366 days (today + 1 year inclusive)
            assertThat(totalSaved).isEqualTo(366);
        }

        @Test
        void shouldSkipExistingDates() {
            LocalDate today = LocalDate.now();
            Set<LocalDate> existingDates = Set.of(today, today.plusDays(1), today.plusDays(2));

            when(inventoryRepository.findExistingDatesByRoomAndDateRange(eq(room), any(), any()))
                    .thenReturn(existingDates);

            inventoryService.initializeRoomForAYear(room);

            verify(inventoryRepository, atLeastOnce()).saveAll(inventoryListCaptor.capture());

            int totalSaved = inventoryListCaptor.getAllValues().stream()
                    .mapToInt(List::size).sum();

            assertThat(totalSaved).isEqualTo(366 - 3);
        }

        @Test
        void shouldSetCorrectFieldsOnNewInventory() {
            when(inventoryRepository.findExistingDatesByRoomAndDateRange(eq(room), any(), any()))
                    .thenReturn(Collections.emptySet());

            inventoryService.initializeRoomForAYear(room);

            verify(inventoryRepository, atLeastOnce()).saveAll(inventoryListCaptor.capture());

            Inventory first = inventoryListCaptor.getAllValues().get(0).get(0);
            assertThat(first.getHotel()).isEqualTo(hotel);
            assertThat(first.getRoom()).isEqualTo(room);
            assertThat(first.getBookedCount()).isEqualTo(0);
            assertThat(first.getReservedCount()).isEqualTo(0);
            assertThat(first.getTotalCount()).isEqualTo(10);
            assertThat(first.getCity()).isEqualTo("Mumbai");
            assertThat(first.getPrice()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(first.getSurgeFactor()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(first.getClosed()).isFalse();
        }

        @Test
        void shouldBatchSavesIn100() {
            when(inventoryRepository.findExistingDatesByRoomAndDateRange(eq(room), any(), any()))
                    .thenReturn(Collections.emptySet());

            inventoryService.initializeRoomForAYear(room);

            // 366 items = 3 batches of 100 + 1 batch of 66
            verify(inventoryRepository, times(4)).saveAll(inventoryListCaptor.capture());

            List<List<Inventory>> batches = inventoryListCaptor.getAllValues();
            assertThat(batches.get(0)).hasSize(100);
            assertThat(batches.get(1)).hasSize(100);
            assertThat(batches.get(2)).hasSize(100);
            assertThat(batches.get(3)).hasSize(66);
        }
    }

    @Nested
    class DeleteAllInventories {

        @Test
        void shouldDeleteByRoom() {
            inventoryService.deleteAllInventories(room);
            verify(inventoryRepository).deleteByRoom(room);
        }
    }

    @Nested
    class GetAllInventoriesByRoom {

        @Test
        void shouldReturnInventoriesForOwner() {
            Inventory inv = Inventory.builder().id(1L).room(room).date(LocalDate.now()).build();
            InventoryResponse response = InventoryResponse.builder().id(1L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(inventoryRepository.findByRoomOrderByDate(room)).thenReturn(List.of(inv));
                when(modelMapper.map(inv, InventoryResponse.class)).thenReturn(response);

                List<InventoryResponse> result = inventoryService.getAllInventoriesByRoom(1L);

                assertThat(result).hasSize(1);
            }
        }

        @Test
        void shouldThrowWhenNotOwner() {
            User otherUser = User.builder().id(2L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(otherUser);
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

                assertThatThrownBy(() -> inventoryService.getAllInventoriesByRoom(1L))
                        .isInstanceOf(UnAuthorisedException.class);
            }
        }

        @Test
        void shouldThrowWhenRoomNotFound() {
            when(roomRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getAllInventoriesByRoom(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class SearchHotels {

        @Test
        void shouldReturnSearchResults() {
            HotelSearchRequest request = HotelSearchRequest.builder()
                    .city("Mumbai")
                    .startDate(LocalDate.now().plusDays(5))
                    .endDate(LocalDate.now().plusDays(8))
                    .roomsCount(1).page(0).size(10).build();

            HotelPriceDto dto = new HotelPriceDto(hotel, 5000.0);
            Page<HotelPriceDto> dtoPage = new PageImpl<>(List.of(dto));
            HotelPriceResponse priceResponse = new HotelPriceResponse();
            priceResponse.setPrice(5000.0);

            when(hotelPriceRepository.findHotelsWithAvailableInventory(
                    anyString(), any(), any(), anyInt(), anyLong(), any(Pageable.class)))
                    .thenReturn(dtoPage);
            when(modelMapper.map(any(Hotel.class), eq(HotelPriceResponse.class))).thenReturn(priceResponse);

            Page<HotelPriceResponse> result = inventoryService.searchHotels(request);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getPrice()).isEqualTo(5000.0);
        }

        @Test
        void shouldThrowWhenDatesNull() {
            HotelSearchRequest request = HotelSearchRequest.builder()
                    .city("Mumbai").startDate(null).endDate(null)
                    .roomsCount(1).build();

            assertThatThrownBy(() -> inventoryService.searchHotels(request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("required");
        }

        @Test
        void shouldThrowWhenEndDateNotAfterStartDate() {
            HotelSearchRequest request = HotelSearchRequest.builder()
                    .city("Mumbai")
                    .startDate(LocalDate.now().plusDays(5))
                    .endDate(LocalDate.now().plusDays(3))
                    .roomsCount(1).build();

            assertThatThrownBy(() -> inventoryService.searchHotels(request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("End date must be after start date");
        }
    }

    @Nested
    class UpdateInventories {

        @Test
        void shouldUpdateSurgeFactorForDateRange() {
            LocalDate start = LocalDate.now().plusDays(1);
            LocalDate end = LocalDate.now().plusDays(3);

            Inventory inv1 = Inventory.builder().date(start).surgeFactor(BigDecimal.ONE).closed(false).build();
            Inventory inv2 = Inventory.builder().date(start.plusDays(1)).surgeFactor(BigDecimal.ONE).closed(false).build();
            Inventory inv3 = Inventory.builder().date(end).surgeFactor(BigDecimal.ONE).closed(false).build();

            InventoryRequest request = InventoryRequest.builder()
                    .startDate(start).endDate(end)
                    .surgeFactor(new BigDecimal("1.5")).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(inventoryRepository.findByRoomOrderByDate(room))
                        .thenReturn(List.of(inv1, inv2, inv3));

                inventoryService.updateInventories(1L, request);

                verify(inventoryRepository).saveAll(anyList());
                assertThat(inv1.getSurgeFactor()).isEqualByComparingTo(new BigDecimal("1.5"));
                assertThat(inv2.getSurgeFactor()).isEqualByComparingTo(new BigDecimal("1.5"));
                assertThat(inv3.getSurgeFactor()).isEqualByComparingTo(new BigDecimal("1.5"));
            }
        }

        @Test
        void shouldUpdateClosedStatusForDateRange() {
            LocalDate start = LocalDate.now().plusDays(1);
            Inventory inv = Inventory.builder().date(start).closed(false).build();

            InventoryRequest request = InventoryRequest.builder()
                    .startDate(start).endDate(start).closed(true).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(inventoryRepository.findByRoomOrderByDate(room)).thenReturn(List.of(inv));

                inventoryService.updateInventories(1L, request);

                assertThat(inv.getClosed()).isTrue();
            }
        }

        @Test
        void shouldThrowWhenDatesNotProvided() {
            InventoryRequest request = InventoryRequest.builder()
                    .startDate(null).endDate(null).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(owner);
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

                assertThatThrownBy(() -> inventoryService.updateInventories(1L, request))
                        .isInstanceOf(APIException.class);
            }
        }
    }
}
