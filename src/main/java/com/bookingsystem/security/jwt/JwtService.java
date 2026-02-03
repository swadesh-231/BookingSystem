package com.bookingsystem.security.jwt;

import com.bookingsystem.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @Value("${spring.app.secret}")
    private String secret;
    @Value("${spring.app.expiration}")
    private Long expiration;
    @Value("${spring.app.refreshtoken}")
    private Long refreshTime;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("roles", user.getRoles().toString())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + expiration))
                .signWith(getSecretKey())
                .compact();
    }
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + refreshTime))
                .signWith(getSecretKey())
                .compact();
    }
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(refreshToken);
            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
