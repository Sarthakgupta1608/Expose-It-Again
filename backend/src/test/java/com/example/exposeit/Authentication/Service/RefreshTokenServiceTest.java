package com.example.exposeit.Authentication.Service;

import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RefreshTokenServiceTest
 *
 * Tests the creation, validation, expiration checking, and deletion of Refresh Tokens.
 * Verifies that existing tokens are replaced upon a new creation request and that expired
 * tokens are correctly deleted from the repository.
 */
class RefreshTokenServiceTest extends BaseUnitTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private static final long REFRESH_EXPIRATION_MS = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "RefreshExpiration", REFRESH_EXPIRATION_MS);

        testUser = User.builder()
                .userName("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Test
    void testCreateRefreshToken_Success_NoOldToken() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = refreshTokenService.createRefreshToken("testuser");

        assertNotNull(created);
        assertNotNull(created.getToken());
        assertEquals(testUser, created.getUser());
        assertTrue(created.getExpiration().isAfter(Instant.now()));

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testCreateRefreshToken_Success_WithOldToken() {
        RefreshToken oldToken = RefreshToken.builder()
                .token("old-token-uuid")
                .user(testUser)
                .expiration(Instant.now())
                .build();

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUser(testUser)).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = refreshTokenService.createRefreshToken("testuser");

        assertNotNull(created);
        assertNotEquals("old-token-uuid", created.getToken());
        verify(refreshTokenRepository).delete(oldToken);
        verify(refreshTokenRepository).flush();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testCreateRefreshToken_UserNotFound() {
        when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> refreshTokenService.createRefreshToken("unknown"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testVerifyExpiration_Valid() {
        RefreshToken token = RefreshToken.builder()
                .token("token")
                .expiration(Instant.now().plusSeconds(100))
                .build();

        RefreshToken result = refreshTokenService.verifyExpiration(token);
        assertEquals(token, result);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void testVerifyExpiration_Expired() {
        RefreshToken token = RefreshToken.builder()
                .token("token")
                .expiration(Instant.now().minusSeconds(10))
                .build();

        assertThrows(IllegalStateException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void testDeleteByToken() {
        refreshTokenService.deleteByToken("my-token");
        verify(refreshTokenRepository).deleteByToken("my-token");
    }

    @Test
    void testFindByToken() {
        RefreshToken token = RefreshToken.builder().token("my-token").build();
        when(refreshTokenRepository.findByToken("my-token")).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = refreshTokenService.findByToken("my-token");
        assertTrue(result.isPresent());
        assertEquals("my-token", result.get().getToken());
    }
}
