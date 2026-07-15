package com.example.exposeit.Authentication.Service;

import com.example.exposeit.testinfra.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWTServiceTest
 *
 * Tests the JWT generation, parsing, expiration verification, and username extraction logic.
 * Validates that signatures are verified against the configured signing key and expired tokens
 * are correctly identified.
 */
class JWTServiceTest extends BaseUnitTest {

    private JWTService jwtService;
    private UserDetails userDetails;
    private static final String TEST_SECRET = "verySecretSigningKeyThatMustBeAtLeast32BytesLongForHMAC256Verification";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "JwtExpiration", EXPIRATION_MS);

        userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void testGenerateAndExtractUsername() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void testGenerateWithExtraClaims() {
        Map<String, Object> extra = new HashMap<>();
        extra.put("role", "ADMIN");
        extra.put("customId", 12345);

        String token = jwtService.generateToken(extra, userDetails);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        Integer customId = jwtService.extractClaim(token, claims -> claims.get("customId", Integer.class));
        assertEquals("ADMIN", role);
        assertEquals(12345, customId);
    }

    @Test
    void testExtractExpiration() {
        long beforeGen = System.currentTimeMillis();
        String token = jwtService.generateToken(userDetails);
        long afterGen = System.currentTimeMillis();

        Date expiration = jwtService.extractExpiration(token);
        assertNotNull(expiration);

        long expTime = expiration.getTime();
        long expectedExpTime = beforeGen + EXPIRATION_MS;
        long diff = Math.abs(expTime - expectedExpTime);
        assertTrue(diff < 5000, "Difference should be less than 5 seconds, but was " + diff + " ms");
    }

    @Test
    void testIsTokenValid_Success() {
        String token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValid_WrongUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = User.withUsername("wronguser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    void testIsTokenExpired() {
        ReflectionTestUtils.setField(jwtService, "JwtExpiration", -1000L); // already expired
        String token = jwtService.generateToken(userDetails);
        assertThrows(Exception.class, () -> jwtService.isTokenValid(token, userDetails));
    }
}
