package com.bookingsystem.service;

import com.bookingsystem.dto.ShowRequestDto;
import com.bookingsystem.dto.ShowResponseDto;

import java.util.List;

public interface ShowService {
    ShowResponseDto createShow(ShowRequestDto requestDto);
    List<ShowResponseDto> getAllShows();
    List<ShowResponseDto> getShowsByTheater(String theaterName);
    List<ShowResponseDto> getShowsByMovie(String movieName);
    ShowResponseDto updateShow(Long showId, ShowRequestDto requestDto);
    void deleteShow(Long showId);
}
