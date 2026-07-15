package com.example.exposeit.integration;

import com.example.exposeit.Authentication.DTO.AuthRegister;
import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.Authentication.Service.JWTService;
import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseApiTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.hasLength;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EdgeCaseIntegrationTest
 *
 * Verifies application failure modes, boundary limits, and input validations for Module 7.
 * Validates security boundary limits (expired JWT, bad signature, refresh token rotation/replay),
 * validation boundary constraints (oversized post description, invalid lat/lon, empty categories),
 * and concurrent user registrations.
 */
class EdgeCaseIntegrationTest extends BaseApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDb() {
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testExpiredJwt_ReturnsUnauthorized() throws Exception {
        User user = User.builder()
                .userName("expired_jwt_user")
                .email("expired@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        Long originalExpiration = (Long) ReflectionTestUtils.getField(jwtService, "JwtExpiration");
        try {
            // Set expiration to negative value to force generating an expired token
            ReflectionTestUtils.setField(jwtService, "JwtExpiration", -1000L);
            String expiredToken = jwtService.generateToken(user);
            
            Cookie expiredCookie = new Cookie("access_token", expiredToken);

            mockMvc.perform(get("/api/posts/trending")
                    .cookie(expiredCookie))
                    .andExpect(status().isUnauthorized());
        } finally {
            ReflectionTestUtils.setField(jwtService, "JwtExpiration", originalExpiration);
        }
    }

    @Test
    void testMalformedJwt_ReturnsUnauthorized() throws Exception {
        Cookie malformedCookie = new Cookie("access_token", "malformed.jwt.token");
        mockMvc.perform(get("/api/posts/trending")
                .cookie(malformedCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidSignatureJwt_ReturnsUnauthorized() throws Exception {
        User user = User.builder()
                .userName("invalid_sig_user")
                .email("sig@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        // Generate a token signed with a different key
        JWTService customJwtService = new JWTService();
        ReflectionTestUtils.setField(customJwtService, "SECRET_KEY", "differentSecretKeyThatMustBeAtLeast32BytesLongForHMAC256Verification");
        ReflectionTestUtils.setField(customJwtService, "JwtExpiration", 3600000L);
        String wrongSignedToken = customJwtService.generateToken(user);

        Cookie wrongCookie = new Cookie("access_token", wrongSignedToken);
        mockMvc.perform(get("/api/posts/trending")
                .cookie(wrongCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDuplicateRefreshToken_DeletesOldAndReplayFails() throws Exception {
        User user = User.builder()
                .userName("rotation_user")
                .email("rotate@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        AuthRequest request = new AuthRequest("rotation_user", "password");

        // First login
        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie1 = login1.getResponse().getCookie("refresh_token");
        assertNotNull(refreshCookie1);

        // Assert exactly 1 token exists
        assertEquals(1, refreshTokenRepository.count());

        // Second login (generates a new token and deletes the old one)
        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie2 = login2.getResponse().getCookie("refresh_token");
        assertNotNull(refreshCookie2);
        assertNotEquals(refreshCookie1.getValue(), refreshCookie2.getValue());

        // Assert only 1 token exists and the first token is deleted
        assertEquals(1, refreshTokenRepository.count());
        assertFalse(refreshTokenRepository.findByToken(refreshCookie1.getValue()).isPresent());
        assertTrue(refreshTokenRepository.findByToken(refreshCookie2.getValue()).isPresent());

        // Replay old refresh token (returns 401 Unauthorized because the token is deleted from DB)
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOversizedPostDescription_Succeeds() throws Exception {
        User user = User.builder()
                .userName("oversized_user")
                .email("oversized@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("oversized_user", "password"))))
                .andReturn();
        Cookie accessCookie = login.getResponse().getCookie("access_token");
        assertNotNull(accessCookie);

        String largeDesc = "A".repeat(25000);
        PostCreateRequest postReq = new PostCreateRequest();
        postReq.setTitle("Oversized Post Title");
        postReq.setDescription(largeDesc);
        postReq.setLatitude(34.0522);
        postReq.setLongitude(-118.2437);
        postReq.setCategories(Set.of(PostCategory.EDUCATION));

        mockMvc.perform(post("/api/posts")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", hasLength(25000)));
    }

    @Test
    void testInvalidCoordinates_ReturnsBadRequest() throws Exception {
        User user = User.builder()
                .userName("coordinates_user")
                .email("coordinates@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("coordinates_user", "password"))))
                .andReturn();
        Cookie accessCookie = login.getResponse().getCookie("access_token");
        assertNotNull(accessCookie);

        PostCreateRequest postReq = new PostCreateRequest();
        postReq.setTitle("Invalid Lat Lon");
        postReq.setDescription("Valid Description");
        postReq.setLatitude(150.0);
        postReq.setLongitude(200.0);
        postReq.setCategories(Set.of(PostCategory.EDUCATION));

        // Returns 400 Bad Request due to coordinate range check failure
        mockMvc.perform(post("/api/posts")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmptyCategories_ReturnsBadRequest() throws Exception {
        User user = User.builder()
                .userName("categories_user")
                .email("categories@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("categories_user", "password"))))
                .andReturn();
        Cookie accessCookie = login.getResponse().getCookie("access_token");
        assertNotNull(accessCookie);

        PostCreateRequest postReq = new PostCreateRequest();
        postReq.setTitle("Empty Categories");
        postReq.setDescription("Valid Description");
        postReq.setLatitude(34.0522);
        postReq.setLongitude(-118.2437);
        postReq.setCategories(Collections.emptySet());

        mockMvc.perform(post("/api/posts")
                .cookie(accessCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConcurrentRegistrations_HandlesSafely() throws Exception {
        AuthRegister registerDto = new AuthRegister("concurrent_user", "concurrent@example.com", "password");
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<CompletableFuture<MvcResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await();
                    return mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDto)))
                            .andReturn();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor));
        }

        latch.countDown();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int createdCount = 0;
        int conflictCount = 0;
        for (CompletableFuture<MvcResult> future : futures) {
            int status = future.get().getResponse().getStatus();
            if (status == 201) {
                createdCount++;
            } else if (status == 409) {
                conflictCount++;
            }
        }

        executor.shutdown();
        assertEquals(1, createdCount, "Exactly one registration should succeed");
        assertEquals(threadCount - 1, conflictCount, "Remaining concurrent registrations should fail with 409 Conflict");
    }
}
