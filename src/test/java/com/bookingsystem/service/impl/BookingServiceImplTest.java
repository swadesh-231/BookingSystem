package com.bookingsystem.service.impl;

import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.GuestDto;
import com.bookingsystem.dto.BookingRequest;
import com.bookingsystem.dto.HotelReport;
import com.bookingsystem.entity.*;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.entity.enums.Gender;
import com.bookingsystem.entity.enums.Role;
import com.bookingsystem.exception.*;
import com.bookingsystem.repository.*;
import com.bookingsystem.security.utils.AuthUtils;
import com.bookingsystem.service.TransactionalService;
import com.bookingsystem.strategy.PricingService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private GuestRepository guestRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private TransactionalService transactionalService;
    @Mock private PricingService pricingService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Hotel hotel;
    private Room room;
    private Booking booking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "serverFrontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(bookingService, "reservationTtlMinutes", 10);

        user = User.builder()
                .id(1L).name("Test User").email("test@example.com")
                .password("encoded").roles(Set.of(Role.GUEST))
                .build();

        hotel = Hotel.builder()
                .id(1L).name("Test Hotel").city("Mumbai")
                .active(true).owner(user).rooms(new ArrayList<>())
                .build();

        room = Room.builder()
                .id(1L).hotel(hotel).type("DELUXE")
                .basePrice(new BigDecimal("5000.00")).totalCount(10).capacity(2)
                .build();

        booking = Booking.builder()
                .id(1L).hotel(hotel).room(room).user(user)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .roomsCount(2).amount(new BigDecimal("30000.00"))
                .status(BookingStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .guests(new ArrayList<>())
                .build();
    }

    @Nested
    class InitialiseBooking {

        @Test
        void shouldInitialiseBookingSuccessfully() {
            BookingRequest request = BookingRequest.builder()
                    .hotelId(1L).roomId(1L)
                    .checkInDate(LocalDate.now().plusDays(5))
                    .checkOutDate(LocalDate.now().plusDays(8))
                    .roomsCount(2).build();

            List<Inventory> inventories = List.of(
                    createInventory(LocalDate.now().plusDays(5)),
                    createInventory(LocalDate.now().plusDays(6)),
                    createInventory(LocalDate.now().plusDays(7)));

            BookingResponse expectedResponse = BookingResponse.builder()
                    .id(1L).status(BookingStatus.RESERVED).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);

                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
                when(inventoryRepository.findAndLockAvailableInventory(eq(1L), any(), any(), eq(2)))
                        .thenReturn(inventories);
                when(pricingService.calculateTotalPrice(inventories)).thenReturn(new BigDecimal("15000.00"));
                when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
                when(modelMapper.map(any(Booking.class), eq(BookingResponse.class))).thenReturn(expectedResponse);

                BookingResponse result = bookingService.initialiseBooking(request);

                assertThat(result.getStatus()).isEqualTo(BookingStatus.RESERVED);
                verify(inventoryRepository).initBooking(eq(1L), any(), any(), eq(2));
            }
        }

        @Test
        void shouldThrowWhenCheckOutBeforeCheckIn() {
            BookingRequest request = BookingRequest.builder()
                    .hotelId(1L).roomId(1L)
                    .checkInDate(LocalDate.now().plusDays(8))
                    .checkOutDate(LocalDate.now().plusDays(5))
                    .roomsCount(1).build();

            assertThatThrownBy(() -> bookingService.initialiseBooking(request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("Check-out date must be after check-in date");
        }

        @Test
        void shouldThrowWhenHotelNotFound() {
            BookingRequest request = BookingRequest.builder()
                    .hotelId(999L).roomId(1L)
                    .checkInDate(LocalDate.now().plusDays(1))
                    .checkOutDate(LocalDate.now().plusDays(3))
                    .roomsCount(1).build();

            when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.initialiseBooking(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowWhenHotelNotActive() {
            hotel.setActive(false);
            BookingRequest request = BookingRequest.builder()
                    .hotelId(1L).roomId(1L)
                    .checkInDate(LocalDate.now().plusDays(1))
                    .checkOutDate(LocalDate.now().plusDays(3))
                    .roomsCount(1).build();

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> bookingService.initialiseBooking(request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        void shouldThrowWhenRoomDoesNotBelongToHotel() {
            Hotel otherHotel = Hotel.builder().id(2L).active(true).build();
            Room otherRoom = Room.builder().id(2L).hotel(otherHotel).build();

            BookingRequest request = BookingRequest.builder()
                    .hotelId(1L).roomId(2L)
                    .checkInDate(LocalDate.now().plusDays(1))
                    .checkOutDate(LocalDate.now().plusDays(3))
                    .roomsCount(1).build();

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(roomRepository.findById(2L)).thenReturn(Optional.of(otherRoom));

            assertThatThrownBy(() -> bookingService.initialiseBooking(request))
                    .isInstanceOf(APIException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        void shouldThrowWhenRoomNotAvailableForFullRange() {
            BookingRequest request = BookingRequest.builder()
                    .hotelId(1L).roomId(1L)
                    .checkInDate(LocalDate.now().plusDays(5))
                    .checkOutDate(LocalDate.now().plusDays(8))
                    .roomsCount(2).build();

            // Only 2 days available out of 3 required
            List<Inventory> partialInventory = List.of(
                    createInventory(LocalDate.now().plusDays(5)),
                    createInventory(LocalDate.now().plusDays(6)));

            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(inventoryRepository.findAndLockAvailableInventory(eq(1L), any(), any(), eq(2)))
                    .thenReturn(partialInventory);

            assertThatThrownBy(() -> bookingService.initialiseBooking(request))
                    .isInstanceOf(RoomNotAvailableException.class);
        }
    }

    @Nested
    class AddGuests {

        @Test
        void shouldAddGuestsSuccessfully() {
            List<GuestDto> guests = List.of(
                    GuestDto.builder().name("Guest 1").gender(Gender.MALE)
                            .dateOfBirth(LocalDate.of(1990, 1, 1)).build());

            Guest savedGuest = Guest.builder().id(1L).name("Guest 1")
                    .gender(Gender.MALE).user(user).build();

            BookingResponse expectedResponse = BookingResponse.builder()
                    .id(1L).status(BookingStatus.GUEST_ADDED).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);

                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
                when(guestRepository.save(any(Guest.class))).thenReturn(savedGuest);
                when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
                when(modelMapper.map(any(Booking.class), eq(BookingResponse.class))).thenReturn(expectedResponse);

                BookingResponse result = bookingService.addGuests(1L, guests);

                assertThat(result.getStatus()).isEqualTo(BookingStatus.GUEST_ADDED);
                verify(guestRepository).save(any(Guest.class));
            }
        }

        @Test
        void shouldThrowWhenBookingExpired() {
            booking.setCreatedAt(LocalDateTime.now().minusMinutes(15));

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                        .isInstanceOf(BookingExpiredException.class);
            }
        }

        @Test
        void shouldThrowWhenUnauthorizedUser() {
            User otherUser = User.builder().id(2L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(otherUser);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                        .isInstanceOf(UnAuthorisedException.class);
            }
        }

        @Test
        void shouldThrowWhenBookingNotInReservedState() {
            booking.setStatus(BookingStatus.CONFIRMED);

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                        .isInstanceOf(InvalidBookingStateException.class);
            }
        }

        @Test
        void shouldThrowWhenGuestListEmpty() {
            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.addGuests(1L, List.of()))
                        .isInstanceOf(APIException.class)
                        .hasMessageContaining("At least one guest");
            }
        }
    }

    @Nested
    class InitiatePayments {

        @Test
        void shouldInitiatePaymentSuccessfully() {
            booking.setStatus(BookingStatus.GUEST_ADDED);

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
                when(transactionalService.getCheckoutSession(any(), anyString(), anyString()))
                        .thenReturn("https://checkout.stripe.com/session123");

                String url = bookingService.initiatePayments(1L);

                assertThat(url).isEqualTo("https://checkout.stripe.com/session123");
                verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.PAYMENT_PENDING));
            }
        }

        @Test
        void shouldThrowWhenNotInGuestAddedState() {
            booking.setStatus(BookingStatus.RESERVED);

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.initiatePayments(1L))
                        .isInstanceOf(InvalidBookingStateException.class);
            }
        }
    }

    @Nested
    class CancelBooking {

        @Test
        void shouldCancelReservedBookingAndReleaseInventory() {
            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                bookingService.cancelBooking(1L);

                verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.CANCELLED));
                verify(inventoryRepository).releaseReservedInventory(
                        eq(room.getId()), any(), any(), eq(booking.getRoomsCount()));
            }
        }

        @Test
        void shouldThrowWhenAlreadyCancelled() {
            booking.setStatus(BookingStatus.CANCELLED);

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                        .isInstanceOf(InvalidBookingStateException.class)
                        .hasMessageContaining("already cancelled");
            }
        }

        @Test
        void shouldThrowWhenUnauthorized() {
            User otherUser = User.builder().id(2L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(otherUser);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> bookingService.cancelBooking(1L))
                        .isInstanceOf(UnAuthorisedException.class);
            }
        }
    }

    @Nested
    class GetBookingStatus {

        @Test
        void shouldReturnBookingStatus() {
            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

                BookingStatus status = bookingService.getBookingStatus(1L);

                assertThat(status).isEqualTo(BookingStatus.RESERVED);
            }
        }

        @Test
        void shouldThrowWhenBookingNotFound() {
            when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBookingStatus(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetAllBookingsByHotelIdPaginated {

        @Test
        void shouldReturnPaginatedBookings() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Booking> bookingPage = new PageImpl<>(List.of(booking), pageable, 1);
            BookingResponse response = BookingResponse.builder().id(1L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(bookingRepository.findByHotel(hotel, pageable)).thenReturn(bookingPage);
                when(modelMapper.map(any(Booking.class), eq(BookingResponse.class))).thenReturn(response);

                Page<BookingResponse> result = bookingService.getAllBookingsByHotelId(1L, pageable);

                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent()).hasSize(1);
            }
        }

        @Test
        void shouldThrowWhenNotOwner() {
            User otherUser = User.builder().id(2L).build();
            Pageable pageable = PageRequest.of(0, 10);

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(otherUser);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

                assertThatThrownBy(() -> bookingService.getAllBookingsByHotelId(1L, pageable))
                        .isInstanceOf(UnAuthorisedException.class);
            }
        }
    }

    @Nested
    class GetHotelReport {

        @Test
        void shouldCalculateReportCorrectly() {
            Booking confirmedBooking1 = Booking.builder()
                    .status(BookingStatus.CONFIRMED).amount(new BigDecimal("10000")).build();
            Booking confirmedBooking2 = Booking.builder()
                    .status(BookingStatus.CONFIRMED).amount(new BigDecimal("20000")).build();
            Booking cancelledBooking = Booking.builder()
                    .status(BookingStatus.CANCELLED).amount(new BigDecimal("5000")).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(bookingRepository.findByHotelAndCreatedAtBetween(eq(hotel), any(), any()))
                        .thenReturn(List.of(confirmedBooking1, confirmedBooking2, cancelledBooking));

                HotelReport report = bookingService.getHotelReport(1L,
                        LocalDate.now().minusMonths(1), LocalDate.now());

                assertThat(report.getBookingCount()).isEqualTo(2);
                assertThat(report.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("30000"));
                assertThat(report.getAvgRevenue()).isEqualByComparingTo(new BigDecimal("15000.00"));
            }
        }

        @Test
        void shouldReturnZerosWhenNoConfirmedBookings() {
            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
                when(bookingRepository.findByHotelAndCreatedAtBetween(eq(hotel), any(), any()))
                        .thenReturn(List.of());

                HotelReport report = bookingService.getHotelReport(1L,
                        LocalDate.now().minusMonths(1), LocalDate.now());

                assertThat(report.getBookingCount()).isEqualTo(0);
                assertThat(report.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(report.getAvgRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }

    @Nested
    class GetMyBookingsPaginated {

        @Test
        void shouldReturnPaginatedUserBookings() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Booking> bookingPage = new PageImpl<>(List.of(booking), pageable, 1);
            BookingResponse response = BookingResponse.builder().id(1L).build();

            try (MockedStatic<AuthUtils> authUtils = mockStatic(AuthUtils.class)) {
                authUtils.when(AuthUtils::getCurrentUser).thenReturn(user);
                when(bookingRepository.findByUserOrderByCreatedAtDesc(user, pageable)).thenReturn(bookingPage);
                when(modelMapper.map(any(Booking.class), eq(BookingResponse.class))).thenReturn(response);

                Page<BookingResponse> result = bookingService.getMyBookings(pageable);

                assertThat(result.getTotalElements()).isEqualTo(1);
            }
        }
    }

    @Nested
    class HasBookingExpired {

        @Test
        void shouldReturnTrueWhenExpired() {
            booking.setCreatedAt(LocalDateTime.now().minusMinutes(15));
            assertThat(bookingService.hasBookingExpired(booking)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNotExpired() {
            booking.setCreatedAt(LocalDateTime.now().minusMinutes(5));
            assertThat(bookingService.hasBookingExpired(booking)).isFalse();
        }
    }

    private Inventory createInventory(LocalDate date) {
        return Inventory.builder()
                .id(1L).hotel(hotel).room(room).date(date)
                .bookedCount(0).reservedCount(0).totalCount(10)
                .surgeFactor(BigDecimal.ONE).price(new BigDecimal("5000"))
                .city("Mumbai").closed(false).build();
    }
}
