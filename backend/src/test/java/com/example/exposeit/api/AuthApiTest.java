package com.example.exposeit.api;

import com.example.exposeit.Authentication.DTO.AuthRegister;
import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthApiTest
 *
 * Black-box REST API contract validation tests for authentication endpoints (/api/auth/**)
 * using MockMvc. Validates registration success, validation failure, login credentials check,
 * cookie issuance, token refresh, and logout cookie expiration.
 */
class AuthApiTest extends BaseApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDb() {
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testRegister_Success() throws Exception {
        AuthRegister registerDto = new AuthRegister("api_register_user", "api_reg@example.com", "my_password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registration Successful!"));

        assertTrue(userRepository.findByUserName("api_register_user").isPresent());
    }

    @Test
    void testRegister_ValidationFailure() throws Exception {
        AuthRegister registerDto = new AuthRegister("", "invalid-email", "pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_ConflictFailure() throws Exception {
        User user = User.builder()
                .userName("dup_api_user")
                .email("dup_api@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        AuthRegister registerDto = new AuthRegister("dup_api_user", "other@example.com", "password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username is already taken!"));
    }

    @Test
    void testLogin_Success() throws Exception {
        User user = User.builder()
                .userName("api_login_user")
                .email("login@example.com")
                .password(passwordEncoder.encode("correct_password"))
                .build();
        userRepository.save(user);

        AuthRequest request = new AuthRequest("api_login_user", "correct_password");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Login Successful!"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        Cookie accessToken = result.getResponse().getCookie("access_token");
        Cookie refreshToken = result.getResponse().getCookie("refresh_token");

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertTrue(accessToken.isHttpOnly());
        assertTrue(refreshToken.isHttpOnly());
    }

    @Test
    void testLogin_AuthFailure() throws Exception {
        User user = User.builder()
                .userName("api_login_user")
                .email("login@example.com")
                .password(passwordEncoder.encode("correct_password"))
                .build();
        userRepository.save(user);

        AuthRequest request = new AuthRequest("api_login_user", "wrong_password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid Credentials!"));
    }

    @Test
    void testRefresh_Success() throws Exception {
        User user = User.builder()
                .userName("api_refresh_user")
                .email("refresh@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("api_refresh_user", "password"))))
                .andReturn();

        Cookie refreshToken = loginResult.getResponse().getCookie("refresh_token");
        assertNotNull(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Refreshed"))
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    void testRefresh_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("No Refresh Token found!"));
    }

    @Test
    void testLogout() throws Exception {
        User user = User.builder()
                .userName("api_logout_user")
                .email("logout@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("api_logout_user", "password"))))
                .andReturn();

        Cookie refreshToken = loginResult.getResponse().getCookie("refresh_token");
        assertNotNull(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                .cookie(refreshToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Success!"))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andReturn();
    }
}
