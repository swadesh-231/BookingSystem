package com.bookingsystem.service;

import com.bookingsystem.dto.MovieRequestDto;
import com.bookingsystem.dto.MovieResponseDto;
import com.bookingsystem.entity.Movie;
import com.bookingsystem.entity.enums.Genre;
import com.bookingsystem.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService{
    private final MovieRepository movieRepository;
    private final ModelMapper modelMapper;

    @Override
    public MovieResponseDto addMovie(MovieRequestDto movieRequestDto) {
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
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + id));
        return modelMapper.map(movie, MovieResponseDto.class);
    }

    @Override
    public MovieResponseDto getMovieByExactTitle(String title) {
        Movie movie = movieRepository.findByNameIgnoreCase(title)
                .orElseThrow(() -> new RuntimeException("Movie not found with title: " + title));
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
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + id));
        existingMovie.setName(movieRequestDto.getName());
        existingMovie.setDescription(movieRequestDto.getDescription());
        existingMovie.setDuration(movieRequestDto.getDuration());
        existingMovie.setLanguage(movieRequestDto.getLanguage());
        existingMovie.setReleaseDate(movieRequestDto.getReleaseDate());
        existingMovie.setGenres(movieRequestDto.getGenres());

        Movie updatedMovie = movieRepository.save(existingMovie);
        return modelMapper.map(updatedMovie, MovieResponseDto.class);
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + id));
        movieRepository.delete(movie);
    }
}
