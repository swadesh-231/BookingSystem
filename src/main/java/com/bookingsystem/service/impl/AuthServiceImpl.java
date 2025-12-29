package com.bookingsystem.service.impl;

import com.bookingsystem.dto.AuthResponseDto;
import com.bookingsystem.dto.LoginRequestDto;
import com.bookingsystem.dto.LoginResponseDto;
import com.bookingsystem.dto.RegisterRequestDto;
import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.Roles;
import com.bookingsystem.exception.AuthenticationFailedException;
import com.bookingsystem.exception.DuplicateResourceException;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.security.jwt.JwtService;
import com.bookingsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public AuthResponseDto register(RegisterRequestDto dto) {
        validateUniqueUser(dto);
        User user = modelMapper.map(dto, User.class);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRoles(Set.of(Roles.ROLE_USER));
        User savedUser = userRepository.save(user);
        return mapToAuthResponse(savedUser);
    }

    @Override
    public AuthResponseDto registerAdmin(RegisterRequestDto dto) {
        validateUniqueUser(dto);
        User admin = modelMapper.map(dto, User.class);
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setRoles(Set.of(
                Roles.ROLE_USER,
                Roles.ROLE_ADMIN
        ));


        User savedAdmin = userRepository.save(admin);
        return mapToAuthResponse(savedAdmin);
    }

    @Override
    public LoginResponseDto login(LoginRequestDto dto) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    dto.getUsername(),
                                    dto.getPassword()
                            )
                    );

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateTokenFromUsername(user);
            return LoginResponseDto.builder()
                    .token(token)
                    .build();

        } catch (BadCredentialsException ex) {
            throw new AuthenticationFailedException("Invalid username or password");
        }
    }

    private void validateUniqueUser(RegisterRequestDto dto) {

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already taken");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }
    }

    private AuthResponseDto mapToAuthResponse(User user) {
        AuthResponseDto response =
                modelMapper.map(user, AuthResponseDto.class);
        response.setRoles(
                user.getRoles()
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        );
        return response;
    }
}
