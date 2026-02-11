package com.bookingsystem.service.impl;

import com.bookingsystem.dto.BookingRequest;
import com.bookingsystem.dto.BookingResponse;
import com.bookingsystem.dto.GuestDto;
import com.bookingsystem.dto.HotelReport;
import com.bookingsystem.entity.*;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.exception.*;
import com.bookingsystem.repository.*;
import com.bookingsystem.service.BookingService;
import com.bookingsystem.service.TransactionalService;
import com.bookingsystem.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final InventoryRepository inventoryRepository;
    private final TransactionalService transactionalService;
    private final PricingService pricingService;

    @Value("${server.frontend.url}")
    private String serverFrontendUrl;

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
        inventoryRepository.initBooking(room.getId(), bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryLists);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));
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
                .amount(totalPrice)
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
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("User is unauthorized to add guests");
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

    @Override
    public String initiatePayments(Long bookingId) {
        Booking booking  = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("User is unauthorized to add guests");
        }
        if (hasBookingExpired(booking)) {
            throw new BookingExpiredException("Booking has expired");
        }
        String sessionUrl = transactionalService.getCheckoutSession(booking,serverFrontendUrl+"/payment/success",serverFrontendUrl+"/payment/failed");
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);
        return sessionUrl;

    }

    @Override
    public void capturePayment(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return;

        String sessionId = session.getId();
        Booking booking =
                bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
                        new ResourceNotFoundException("Session","sessionId",sessionId));

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

    }

    @Override
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking", "id", bookingId));
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        if(booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();

            Refund.create(refundParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public BookingStatus getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking", "id", bookingId));
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("Booking does not belong to this user with id: "+user.getId());
        }

        return booking.getStatus();
    }

    @Override
    public List<BookingResponse> getAllBookingsByHotelId(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        User user = getCurrentUser();
        if(!user.equals(hotel.getOwner())) throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);

        List<Booking> bookings = bookingRepository.findByHotel(hotel);

        return bookings.stream()
                .map((element) -> modelMapper.map(element, BookingResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public HotelReport getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Override
    public List<BookingResponse> getMyBookings() {
        return List.of();
    }


    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now());
    }
    public User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}
