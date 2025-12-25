package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieResponseDto {
    private Long id;
    private String name;
    private String description;
    private Integer duration;
    private String language;
    private LocalDate releaseDate;
    private List<Genre> genres;
//    private List<ShowDto> shows;
}
