package com.example.exposeit.integration;

import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.Authentication.Service.RefreshTokenService;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RefreshTokenLifecycleIntegrationTest
 *
 * Verifies the database lifecycle of refresh tokens (creation, rotation/deletion of old tokens,
 * verification of expiration state, and explicit deletion) using real PostgreSQL repository logic.
 */
class RefreshTokenLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .userName("token_lifecycle_user")
                .email("token@example.com")
                .password("encoded_pass")
                .build();
        userRepository.save(testUser);
    }

    @Test
    void testCreateAndFetchRefreshToken() {
        RefreshToken token = refreshTokenService.createRefreshToken("token_lifecycle_user");
        assertNotNull(token);
        assertNotNull(token.getToken());

        Optional<RefreshToken> foundOpt = refreshTokenRepository.findByToken(token.getToken());
        assertTrue(foundOpt.isPresent());
        assertEquals("token_lifecycle_user", foundOpt.get().getUser().getUsername());
    }

    @Test
    void testCreateRefreshTokenReplacesOldToken() {
        RefreshToken firstToken = refreshTokenService.createRefreshToken("token_lifecycle_user");
        String firstTokenVal = firstToken.getToken();

        RefreshToken secondToken = refreshTokenService.createRefreshToken("token_lifecycle_user");
        String secondTokenVal = secondToken.getToken();

        assertNotEquals(firstTokenVal, secondTokenVal);
        assertFalse(refreshTokenRepository.findByToken(firstTokenVal).isPresent());
        assertTrue(refreshTokenRepository.findByToken(secondTokenVal).isPresent());
    }

    @Test
    void testVerifyExpiration_Valid() {
        RefreshToken token = refreshTokenService.createRefreshToken("token_lifecycle_user");
        RefreshToken verified = refreshTokenService.verifyExpiration(token);
        assertEquals(token.getToken(), verified.getToken());
    }

    @Test
    void testVerifyExpiration_Expired() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token-val")
                .expiration(Instant.now().minusSeconds(10))
                .user(testUser)
                .build();
        refreshTokenRepository.saveAndFlush(expired);

        assertThrows(IllegalStateException.class, () -> {
            refreshTokenService.verifyExpiration(expired);
        });

        assertFalse(refreshTokenRepository.findByToken("expired-token-val").isPresent());
    }

    @Test
    void testDeleteByToken() {
        RefreshToken token = refreshTokenService.createRefreshToken("token_lifecycle_user");
        String tokenStr = token.getToken();

        refreshTokenService.deleteByToken(tokenStr);
        assertFalse(refreshTokenRepository.findByToken(tokenStr).isPresent());
    }
}
