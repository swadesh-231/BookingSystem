package com.bookingsystem.service.impl;

import com.bookingsystem.dto.TheaterRequestDto;
import com.bookingsystem.dto.TheaterResponseDto;
import com.bookingsystem.entity.Theater;
import com.bookingsystem.exception.InvalidRequestException;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.repository.TheaterRepository;
import com.bookingsystem.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheaterServiceImpl implements TheaterService {
    private final TheaterRepository theaterRepository;
    private final ModelMapper modelMapper;
    @Override
    public TheaterResponseDto createTheater(TheaterRequestDto requestDto) {
        theaterRepository.findByNameAndLocationIgnoreCase(requestDto.getName(), requestDto.getLocation())
                .ifPresent(theater -> {throw new InvalidRequestException("Theater already exists at this location");
                });
        if (requestDto.getCapacity() <= 0) {
            throw new InvalidRequestException("Capacity must be greater than 0");
        }
        Theater theater = modelMapper.map(requestDto, Theater.class);
        Theater savedTheater = theaterRepository.save(theater);
        return modelMapper.map(savedTheater, TheaterResponseDto.class);
    }

    @Override
    public List<TheaterResponseDto> getTheatersByLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new InvalidRequestException("Location must not be empty");
        }
        String normalizedLocation = location.trim().toLowerCase();

        return theaterRepository.findByLocationNormalized(normalizedLocation)
                .stream()
                .map(theater -> modelMapper.map(theater, TheaterResponseDto.class))
                .toList();
    }

    @Override
    public TheaterResponseDto updateTheater(Long id, TheaterRequestDto requestDto) {
        Theater existingTheater = theaterRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Theater not found with id: " + id));
        if (requestDto.getCapacity() <= 0) {
            throw new InvalidRequestException("Capacity must be greater than 0");
        }
        existingTheater.setName(requestDto.getName());
        existingTheater.setLocation(requestDto.getLocation());
        existingTheater.setCapacity(requestDto.getCapacity());
        existingTheater.setScreenType(requestDto.getScreenType());
        Theater updatedTheater = theaterRepository.save(existingTheater);
        return modelMapper.map(updatedTheater, TheaterResponseDto.class);
    }

    @Override
    public void deleteTheater(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Theater not found with id: " + id)
                );

        theaterRepository.delete(theater);
    }
}
