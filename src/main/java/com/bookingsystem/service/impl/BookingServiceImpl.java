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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.bookingsystem.security.utils.AuthUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Value("${app.booking.reservation-ttl-minutes}")
    private int reservationTtlMinutes;

    @Override
    @Transactional
    public BookingResponse initialiseBooking(BookingRequest bookingRequest) {
        if (!bookingRequest.getCheckOutDate().isAfter(bookingRequest.getCheckInDate())) {
            throw new APIException("Check-out date must be after check-in date");
        }

        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", bookingRequest.getHotelId()));

        if (!hotel.getActive()) {
            throw new APIException("Hotel is not active for bookings");
        }

        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", bookingRequest.getRoomId()));

        if (!room.getHotel().getId().equals(hotel.getId())) {
            throw new APIException("Room does not belong to the specified hotel");
        }

        // checkOut date is exclusive (guest leaves on checkout day)
        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

        List<Inventory> inventoryLists = inventoryRepository.findAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate().minusDays(1), bookingRequest.getRoomsCount());

        if (inventoryLists.size() != daysCount) {
            throw new RoomNotAvailableException("Requested room is not available for the full date range");
        }

        inventoryRepository.initBooking(room.getId(), bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate().minusDays(1), bookingRequest.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryLists);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

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

        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("User is unauthorized to modify this booking");
        }

        if (hasBookingExpired(booking)) {
            releaseExpiredBooking(booking);
            throw new BookingExpiredException("Booking has expired. Reserved inventory has been released.");
        }

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new InvalidBookingStateException("Booking is not in RESERVED state, cannot add guests");
        }

        if (guestList == null || guestList.isEmpty()) {
            throw new APIException("At least one guest is required");
        }

        for (GuestDto guestDto : guestList) {
            Guest guest = Guest.builder()
                    .name(guestDto.getName())
                    .gender(guestDto.getGender())
                    .dateOfBirth(guestDto.getDateOfBirth())
                    .user(user)
                    .build();
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }
        booking.setStatus(BookingStatus.GUEST_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingResponse.class);
    }

    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("User is unauthorized to pay for this booking");
        }

        if (hasBookingExpired(booking)) {
            releaseExpiredBooking(booking);
            throw new BookingExpiredException("Booking has expired. Reserved inventory has been released.");
        }

        if (booking.getStatus() != BookingStatus.GUEST_ADDED) {
            throw new InvalidBookingStateException("Guests must be added before initiating payment");
        }

        String sessionUrl = transactionalService.getCheckoutSession(booking,
                serverFrontendUrl + "/payment/success",
                serverFrontendUrl + "/payment/failed");
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);
        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) return;

        String sessionId = session.getId();
        Booking booking = bookingRepository.findByPaymentSessionId(sessionId).orElse(null);

        if (booking == null) {
            log.warn("Webhook received for unknown session: {}", sessionId);
            return;
        }

        // Idempotency: skip if already confirmed
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Duplicate webhook for already confirmed booking: {}", booking.getId());
            return;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate().minusDays(1), booking.getRoomsCount());

        inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate().minusDays(1), booking.getRoomsCount());

        log.info("Booking {} confirmed via webhook", booking.getId());
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking", "id", bookingId));
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("You don't have permission to cancel this booking");
        }

        BookingStatus currentStatus = booking.getStatus();

        if (currentStatus == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (currentStatus == BookingStatus.CONFIRMED) {
            // Release booked inventory and refund
            inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate().minusDays(1), booking.getRoomsCount());

            try {
                Session stripeSession = Session.retrieve(booking.getPaymentSessionId());
                RefundCreateParams refundParams = RefundCreateParams.builder()
                        .setPaymentIntent(stripeSession.getPaymentIntent())
                        .build();
                Refund.create(refundParams);
            } catch (StripeException e) {
                log.error("Stripe refund failed for booking {}: {}", bookingId, e.getMessage());
                throw new APIException("Payment refund failed. Please contact support.");
            }
        } else if (currentStatus == BookingStatus.RESERVED || currentStatus == BookingStatus.GUEST_ADDED
                || currentStatus == BookingStatus.PAYMENT_PENDING) {
            // Release reserved inventory (no payment to refund)
            inventoryRepository.releaseReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate().minusDays(1), booking.getRoomsCount());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatus getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking", "id", bookingId));
        User user = getCurrentUser();
        if (!user.equals(booking.getUser())) {
            throw new UnAuthorisedException("You don't have permission to view this booking");
        }
        return booking.getStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookingsByHotelId(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() ->
                new ResourceNotFoundException("Hotel", "id", hotelId));
        User user = getCurrentUser();
        if (!user.equals(hotel.getOwner()))
            throw new UnAuthorisedException("You don't have permission to view bookings for this hotel");
        List<Booking> bookings = bookingRepository.findByHotel(hotel);
        return bookings.stream()
                .map(element -> modelMapper.map(element, BookingResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelReport getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(
                () -> new ResourceNotFoundException("Hotel", "id", hotelId));
        User user = getCurrentUser();
        if (!user.equals(hotel.getOwner()))
            throw new UnAuthorisedException("You don't have permission to view reports for this hotel");

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);

        long totalConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = totalConfirmedBookings == 0 ? BigDecimal.ZERO :
                totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), 2, RoundingMode.HALF_UP);

        return new HotelReport(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User user = getCurrentUser();
        return bookingRepository.findByUser(user).stream()
                .map(element -> modelMapper.map(element, BookingResponse.class))
                .collect(Collectors.toList());
    }

    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(reservationTtlMinutes).isBefore(LocalDateTime.now());
    }

    private void releaseExpiredBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.RESERVED || booking.getStatus() == BookingStatus.GUEST_ADDED) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            inventoryRepository.releaseReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate().minusDays(1), booking.getRoomsCount());
            log.info("Released expired booking {} inventory", booking.getId());
        }
    }
}
