package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuestDto {
    private Long id;
    private String name;
    private Gender gender;
    private LocalDate dateOfBirth;
}
