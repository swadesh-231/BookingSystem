package com.bookingsystem.service;

import com.bookingsystem.dto.TheaterRequestDto;
import com.bookingsystem.dto.TheaterResponseDto;

import java.util.List;

public interface TheaterService {
    TheaterResponseDto createTheater(TheaterRequestDto requestDto);
    List<TheaterResponseDto> getTheatersByLocation(String location);
    TheaterResponseDto updateTheater(Long id, TheaterRequestDto requestDto);
    void deleteTheater(Long id);
}
