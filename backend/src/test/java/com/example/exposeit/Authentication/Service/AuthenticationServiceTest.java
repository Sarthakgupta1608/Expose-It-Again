package com.example.exposeit.Authentication.Service;

import com.example.exposeit.Authentication.DTO.AuthRegister;
import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseUnitTest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthenticationServiceTest
 *
 * Tests user registration, login, logout, and token refresh workflows.
 * Verifies password encoding on registration, correct cookie attachment on login/refresh,
 * exception propagation on failures, and database calls for user verification/token deletion.
 */
class AuthenticationServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "JwtExpiration", 3600000); // 1 hour
        ReflectionTestUtils.setField(authenticationService, "RefreshExpiration", 604800000); // 7 days

        testUser = User.builder()
                .userName("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Test
    void testRegister_Success() {
        AuthRegister registerDto = new AuthRegister();
        registerDto.setUserName("testuser");
        registerDto.setEmail("test@example.com");
        registerDto.setPassword("plainPassword");

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");

        authenticationService.register(registerDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
    }

    @Test
    void testRegister_UsernameTaken() {
        AuthRegister registerDto = new AuthRegister();
        registerDto.setUserName("testuser");
        registerDto.setEmail("test@example.com");

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.register(registerDto));
        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_EmailTaken() {
        AuthRegister registerDto = new AuthRegister();
        registerDto.setUserName("testuser");
        registerDto.setEmail("test@example.com");

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.register(registerDto));
        assertEquals("Email is already taken!", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_Success() {
        AuthRequest loginDto = new AuthRequest();
        loginDto.setUserName("testuser");
        loginDto.setPassword("plainPassword");

        RefreshToken mockRefreshToken = RefreshToken.builder()
                .token("mock-refresh-token")
                .build();

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("mock-jwt-token");
        when(refreshTokenService.createRefreshToken("testuser")).thenReturn(mockRefreshToken);

        authenticationService.login(loginDto, response);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());
        List<Cookie> cookies = cookieCaptor.getAllValues();

        Cookie accessCookie = cookies.stream().filter(c -> "access_token".equals(c.getName())).findFirst().orElse(null);
        Cookie refreshCookie = cookies.stream().filter(c -> "refresh_token".equals(c.getName())).findFirst().orElse(null);

        assertNotNull(accessCookie);
        assertEquals("mock-jwt-token", accessCookie.getValue());
        assertTrue(accessCookie.isHttpOnly());
        assertEquals("/", accessCookie.getPath());
        assertEquals(3600, accessCookie.getMaxAge());

        assertNotNull(refreshCookie);
        assertEquals("mock-refresh-token", refreshCookie.getValue());
        assertTrue(refreshCookie.isHttpOnly());
        assertEquals("/", refreshCookie.getPath());
        assertEquals(604800, refreshCookie.getMaxAge());
    }

    @Test
    void testLogin_AuthenticationFailed() {
        AuthRequest loginDto = new AuthRequest();
        loginDto.setUserName("testuser");
        loginDto.setPassword("wrongPassword");

        doThrow(new AuthenticationException("Bad credentials") {}).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(AuthenticationException.class, () -> authenticationService.login(loginDto, response));
        verify(userRepository, never()).findByUserName(any());
        verify(response, never()).addCookie(any());
    }

    @Test
    void testLogin_UserNotFound() {
        AuthRequest loginDto = new AuthRequest();
        loginDto.setUserName("unknown");
        loginDto.setPassword("password");

        when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authenticationService.login(loginDto, response));
        verify(response, never()).addCookie(any());
    }

    @Test
    void testRefreshToken_Success() {
        RefreshToken validToken = RefreshToken.builder()
                .token("mock-refresh-token")
                .user(testUser)
                .expiration(Instant.now().plusSeconds(100))
                .build();

        when(refreshTokenService.findByToken("mock-refresh-token")).thenReturn(Optional.of(validToken));
        when(jwtService.generateToken(testUser)).thenReturn("new-jwt-token");

        authenticationService.refreshToken("mock-refresh-token", response);

        verify(refreshTokenService).verifyExpiration(validToken);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();

        assertEquals("access_token", cookie.getName());
        assertEquals("new-jwt-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(3600, cookie.getMaxAge());
    }

    @Test
    void testRefreshToken_TokenNotFound() {
        when(refreshTokenService.findByToken("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authenticationService.refreshToken("unknown", response));
        assertEquals("Refresh Token not found!", exception.getMessage());
        verify(response, never()).addCookie(any());
    }

    @Test
    void testLogout() {
        authenticationService.logout("mock-refresh-token", response);

        verify(refreshTokenService).deleteByToken("mock-refresh-token");

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(2)).addCookie(cookieCaptor.capture());
        List<Cookie> cookies = cookieCaptor.getAllValues();

        Cookie accessCookie = cookies.stream().filter(c -> "access_token".equals(c.getName())).findFirst().orElse(null);
        Cookie refreshCookie = cookies.stream().filter(c -> "refresh_token".equals(c.getName())).findFirst().orElse(null);

        assertNotNull(accessCookie);
        assertNull(accessCookie.getValue());
        assertEquals(0, accessCookie.getMaxAge());

        assertNotNull(refreshCookie);
        assertNull(refreshCookie.getValue());
        assertEquals(0, refreshCookie.getMaxAge());
    }

    @Test
    void testLogout_NullToken() {
        authenticationService.logout(null, response);

        verify(refreshTokenService, never()).deleteByToken(any());
        verify(response, times(2)).addCookie(any(Cookie.class));
    }
}
