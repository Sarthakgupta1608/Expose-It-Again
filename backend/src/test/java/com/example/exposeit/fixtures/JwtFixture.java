package com.example.exposeit.fixtures;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Date;
import java.util.HashMap;

/**
 * JwtFixture
 *
 * Provides utilities to generate cryptographically signed JWT authentication tokens
 * offline in-memory, mimicking the backend's signature configuration.
 * Avoids Spring initialization overhead during lightweight authentication checks.
 */
public class JwtFixture {

    public static final String DEFAULT_TEST_SECRET = "test-jwt-secret-key-that-is-long-enough-to-be-secure-32-bytes-exposeit";

    public static String generateTokenOffline(String username) {
        return generateTokenOffline(username, 3600000L); // 1 hour expiration
    }

    public static String generateTokenOffline(String username, long expirationMs) {
        return Jwts.builder()
                .claims(new HashMap<>())
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(DEFAULT_TEST_SECRET.getBytes()))
                .compact();
    }

    public static String generateTokenOffline(UserDetails userDetails) {
        return generateTokenOffline(userDetails.getUsername());
    }
}
