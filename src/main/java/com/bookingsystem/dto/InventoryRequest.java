package com.bookingsystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRequest {
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or later")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date must be today or later")
    private LocalDate endDate;

    @DecimalMin(value = "0.1", message = "Surge factor must be at least 0.1")
    @DecimalMax(value = "10.0", message = "Surge factor must not exceed 10.0")
    private BigDecimal surgeFactor;

    private Boolean closed;
}
