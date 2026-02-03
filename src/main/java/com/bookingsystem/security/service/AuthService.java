package com.bookingsystem.security.service;

import com.bookingsystem.dto.*;

public interface AuthService {
    AuthResponse registerUser(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

    String refreshToken(String refreshToken);
}
