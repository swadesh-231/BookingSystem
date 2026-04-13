package com.bookingsystem.security.jwt;

import com.bookingsystem.entity.User;
import com.bookingsystem.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "thisIsAVeryLongSecretKeyUsedForTestingPurposesOnlyToSatisfyJwtRequirements";
    private static final Long EXPIRATION = 300000L; // 5 mins

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    void generateToken_WithValidUserId_GeneratesValidToken() {
        User user = new User();
        user.setId(123L);
        user.setRoles(java.util.Set.of(Role.valueOf("GUEST")));

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(123L, userId);
    }

    @Test
    void getUserIdFromToken_WithInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtService.getUserIdFromToken("invalid.token.here"));
    }

    @Test
    void getUserIdFromToken_WithExpiredToken_ThrowsExpiredJwtException() {
        // Manually create expired token
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .setSubject("123")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.getUserIdFromToken(expiredToken));
    }
}
