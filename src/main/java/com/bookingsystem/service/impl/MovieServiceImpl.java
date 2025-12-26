package com.bookingsystem.service.impl;

import com.bookingsystem.dto.MovieRequestDto;
import com.bookingsystem.dto.MovieResponseDto;
import com.bookingsystem.entity.Movie;
import com.bookingsystem.entity.enums.Genre;
import com.bookingsystem.exception.InvalidRequestException;
import com.bookingsystem.exception.ResourceNotFoundException;
import com.bookingsystem.repository.MovieRepository;
import com.bookingsystem.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {
    private final MovieRepository movieRepository;
    private final ModelMapper modelMapper;

    @Override
    public MovieResponseDto addMovie(MovieRequestDto movieRequestDto) {
        movieRepository.findByNameIgnoreCase(movieRequestDto.getName())
                .ifPresent(movie -> {
                    throw new InvalidRequestException(
                            "Movie already exists with name: " + movieRequestDto.getName()
                    );
                });
        Movie movie = modelMapper.map(movieRequestDto, Movie.class);
        Movie savedMovie = movieRepository.save(movie);
        return modelMapper.map(savedMovie, MovieResponseDto.class);

    }

    @Override
    public List<MovieResponseDto> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(movie -> modelMapper.map(movie, MovieResponseDto.class))
                .toList();
    }

    @Override
    public MovieResponseDto getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found with id: " + id)
                );
        return modelMapper.map(movie, MovieResponseDto.class);
    }

    @Override
    public MovieResponseDto getMovieByExactTitle(String title) {
        Movie movie = movieRepository.findByNameIgnoreCase(title)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found with title: " + title)
                );

        return modelMapper.map(movie, MovieResponseDto.class);
    }

    @Override
    public List<MovieResponseDto> getMoviesByLanguage(String language) {
        return movieRepository.findByLanguageIgnoreCase(language)
                .stream()
                .map(movie -> modelMapper.map(movie, MovieResponseDto.class))
                .toList();
    }

    @Override
    public List<MovieResponseDto> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenresContaining(genre)
                .stream()
                .map(movie -> modelMapper.map(movie, MovieResponseDto.class))
                .toList();
    }

    @Override
    public MovieResponseDto updateMovie(Long id, MovieRequestDto movieRequestDto) {

        Movie existingMovie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found with id: " + id)
                );
        if (movieRequestDto.getName() != null) {
            existingMovie.setName(movieRequestDto.getName());
        }
        if (movieRequestDto.getDescription() != null) {
            existingMovie.setDescription(movieRequestDto.getDescription());
        }
        if (movieRequestDto.getDuration() != null) {
            existingMovie.setDuration(movieRequestDto.getDuration());
        }
        if (movieRequestDto.getLanguage() != null) {
            existingMovie.setLanguage(movieRequestDto.getLanguage());
        }
        if (movieRequestDto.getReleaseDate() != null) {
            existingMovie.setReleaseDate(movieRequestDto.getReleaseDate());
        }
        if (movieRequestDto.getGenres() != null) {
            existingMovie.getGenres().clear();
            existingMovie.getGenres().addAll(movieRequestDto.getGenres());
        }
        Movie updatedMovie = movieRepository.save(existingMovie);
        return modelMapper.map(updatedMovie, MovieResponseDto.class);
    }


    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found with id: " + id)
                );

        movieRepository.delete(movie);
    }
}
