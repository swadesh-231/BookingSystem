package com.bookingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlatformStatsDto {
    private Long totalUsers;
    private Long totalHotels;
    private Long activeHotels;
    private Long totalBookings;
    private BigDecimal totalRevenue;
}
