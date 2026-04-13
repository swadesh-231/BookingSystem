package com.bookingsystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String message;
    private Boolean status;
    private String path;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    @Builder.Default
    private String correlationId = MDC.get("correlationId");
    private T data;
}