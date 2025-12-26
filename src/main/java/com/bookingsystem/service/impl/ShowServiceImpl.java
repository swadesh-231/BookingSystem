package com.bookingsystem.service.impl;

import com.bookingsystem.dto.ShowRequestDto;
import com.bookingsystem.dto.ShowResponseDto;
import com.bookingsystem.repository.ShowRepository;
import com.bookingsystem.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {
    private final ShowRepository showRepository;
    @Override
    public ShowResponseDto createShow(ShowRequestDto requestDto) {
        return null;
    }

    @Override
    public List<ShowResponseDto> getAllShows() {
        return List.of();
    }

    @Override
    public List<ShowResponseDto> getShowsByTheater(String theaterName) {
        return List.of();
    }

    @Override
    public List<ShowResponseDto> getShowsByMovie(String movieName) {
        return List.of();
    }


    @Override
    public ShowResponseDto updateShow(Long showId, ShowRequestDto requestDto) {
        return null;
    }

    @Override
    public void deleteShow(Long showId) {

    }
}
