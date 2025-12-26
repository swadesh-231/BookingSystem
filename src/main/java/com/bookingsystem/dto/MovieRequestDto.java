package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.Genre;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRequestDto {
    @NotBlank(message = "Movie name is required")
    @Size(min = 2, max = 100, message = "Movie name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 30, message = "Movie duration must be at least 30 minutes")
    @Max(value = 300, message = "Movie duration cannot exceed 300 minutes")
    private Integer duration;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull(message = "Release date is required")
    @PastOrPresent(message = "Release date cannot be in the future")
    private LocalDate releaseDate;

    @NotEmpty(message = "At least one genre must be selected")
    private Set<Genre> genres;
}