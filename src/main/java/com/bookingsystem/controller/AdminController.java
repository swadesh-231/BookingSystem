package com.bookingsystem.controller;

import com.bookingsystem.dto.AuthResponseDto;
import com.bookingsystem.dto.RegisterRequestDto;
import com.bookingsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AuthService authService;

    @PostMapping("/create-admin")
    public ResponseEntity<AuthResponseDto> registerAdmin(@Valid @RequestBody RegisterRequestDto dto) {
        return ResponseEntity.ok(authService.registerAdmin(dto));
    }
}
