package com.bookingsystem.dto;

import com.bookingsystem.entity.enums.Gender;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProfileUpdateRequest{
    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
}