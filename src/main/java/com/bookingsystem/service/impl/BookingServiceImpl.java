package com.bookingsystem.service.impl;

import com.bookingsystem.dto.BookingRequestDto;
import com.bookingsystem.dto.BookingResponseDto;
import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.Show;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.BookingStatus;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.exception.SeatNotAvailableException;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.ShowRepository;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


    @Override
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto) {
        Show show = showRepository.findById(bookingRequestDto.getShowId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Show not found with id: " + bookingRequestDto.getShowId()
                        )
                );
        User user = userRepository.findById(bookingRequestDto.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + bookingRequestDto.getUserId()
                        )
                );
        if (bookingRequestDto.getSeatNumbers() == null || bookingRequestDto.getSeatNumbers().isEmpty()) {
            throw new IllegalArgumentException("Seat numbers are required");
        }
        List<String> bookedSeats =
                bookingRepository.findConfirmedSeatNumbers(show.getShowId());

        for (String seat : bookingRequestDto.getSeatNumbers()) {
            if (bookedSeats.contains(seat)) {
                throw new SeatNotAvailableException(
                        "Seat already booked: " + seat
                );
            }
        }
        Booking booking = Booking.builder()
                .show(show)
                .user(user)
                .seatNumbers(bookingRequestDto.getSeatNumbers())
                .numberOfSeats(bookingRequestDto.getSeatNumbers().size())
                .price(show.getPrice() * bookingRequestDto.getSeatNumbers().size())
                .bookingStatus(BookingStatus.PENDING)
                .bookingTime(LocalDateTime.now())
                .build();
        return mapToResponse(bookingRepository.save(booking));
    }

    @Override
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId
                        )
                );

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING bookings can be confirmed"
            );
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    @Override
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId
                        )
                );

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Confirmed booking cannot be cancelled directly"
            );
        }

        booking.setBookingStatus(BookingStatus.CANCELED);
        bookingRepository.save(booking);
    }

    @Override
    public BookingResponseDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId
                        )
                );

        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponseDto> getBookingsByUser(Long userId) {
        List<Booking> bookings =
                bookingRepository.findByUser_UserId(userId);

        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No bookings found for user id: " + userId
            );
        }

        return bookings.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getBookingsByShow(Long showId) {
        List<Booking> bookings =
                bookingRepository.findByShow_ShowId(showId);

        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No bookings found for show id: " + showId
            );
        }

        return bookings.stream()
                .map(this::mapToResponse)
                .toList();
    }
    private BookingResponseDto mapToResponse(Booking booking) {
        BookingResponseDto dto =
                modelMapper.map(booking, BookingResponseDto.class);
        dto.setUserId(booking.getUser().getUserId());
        dto.setShowId(booking.getShow().getShowId());
        return dto;
    }
}
