package com.bookingsystem.service;

import com.bookingsystem.dto.AuthResponseDto;
import com.bookingsystem.dto.LoginRequestDto;
import com.bookingsystem.dto.LoginResponseDto;
import com.bookingsystem.dto.RegisterRequestDto;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto dto);
    AuthResponseDto registerAdmin(RegisterRequestDto dto);
    LoginResponseDto login(LoginRequestDto dto);
}
