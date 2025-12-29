package com.bookingsystem.service.impl;

import com.bookingsystem.dto.ShowRequestDto;
import com.bookingsystem.dto.ShowResponseDto;
import com.bookingsystem.entity.Booking;
import com.bookingsystem.entity.Movie;
import com.bookingsystem.entity.Show;
import com.bookingsystem.entity.Theater;
import com.bookingsystem.exception.*;
import com.bookingsystem.repository.MovieRepository;
import com.bookingsystem.repository.ShowRepository;
import com.bookingsystem.repository.TheaterRepository;
import com.bookingsystem.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {
    private final ShowRepository showRepository;
    private final ModelMapper modelMapper;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    @Override
    public ShowResponseDto createShow(ShowRequestDto requestDto) {
        Movie movie = movieRepository.findById(requestDto.getMovieId())
                .orElseThrow(() -> new MovieNotFoundException(requestDto.getMovieId()));

        Theater theater = theaterRepository.findById(requestDto.getTheaterId())
                .orElseThrow(() -> new TheaterNotFoundException(requestDto.getTheaterId()));

        Show show = Show.builder()
                .showTime(requestDto.getShowTime())
                .price(requestDto.getPrice())
                .movie(movie)
                .theater(theater)
                .build();

        Show savedShow = showRepository.save(show);

        return modelMapper.map(savedShow, ShowResponseDto.class);
    }

    @Override
    public List<ShowResponseDto> getAllShows() {
        List<Show> shows = showRepository.findAll();

        if (shows.isEmpty()) {
            throw new NoShowsFoundException("No shows available");
        }

        return shows.stream()
                .map(show -> modelMapper.map(show, ShowResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowResponseDto> getShowsByTheater(String theaterName) {
        List<Show> shows = showRepository.findByTheater_NameIgnoreCase(theaterName);

        if (shows.isEmpty()) {
            throw new NoShowsFoundException(
                    "No shows found for theater: " + theaterName
            );
        }

        return shows.stream()
                .map(show -> modelMapper.map(show, ShowResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowResponseDto> getShowsByMovie(String movieName) {
        List<Show> shows = showRepository.findByMovie_NameIgnoreCase(movieName);

        if (shows.isEmpty()) {
            throw new NoShowsFoundException(
                    "No shows found for movie: " + movieName
            );
        }

        return shows.stream()
                .map(show -> modelMapper.map(show, ShowResponseDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public ShowResponseDto updateShow(Long showId, ShowRequestDto requestDto) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ShowNotFoundException(showId));

        Movie movie = movieRepository.findById(requestDto.getMovieId())
                .orElseThrow(() -> new MovieNotFoundException(requestDto.getMovieId()));

        Theater theater = theaterRepository.findById(requestDto.getTheaterId())
                .orElseThrow(() -> new TheaterNotFoundException(requestDto.getTheaterId()));

        show.setShowTime(requestDto.getShowTime());
        show.setPrice(requestDto.getPrice());
        show.setMovie(movie);
        show.setTheater(theater);

        return modelMapper.map(showRepository.save(show), ShowResponseDto.class);
    }

    @Override
    public void deleteShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ShowNotFoundException(showId));

        if (!show.getBookings().isEmpty()) {
            throw new ShowDeletionNotAllowedException(
                    "Cannot delete show with existing bookings. Show id: " + showId
            );
        }

        showRepository.delete(show);
    }
}
