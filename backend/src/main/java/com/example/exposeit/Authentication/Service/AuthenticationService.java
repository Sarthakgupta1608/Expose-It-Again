package com.example.exposeit.Authentication.Service;

import com.example.exposeit.Authentication.DTO.AuthRegister;
import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.jwt-expiration}")
    private Integer JwtExpiration;
    @Value("${application.security.refresh-expiration}")
    private Integer RefreshExpiration;

    private Cookie createTokenCookie(String name, String value, int maxAge){
        Cookie cookie= new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    private void attachCookies(HttpServletResponse response, String accessToken, String refreshToken){
        Cookie jwtCookie = createTokenCookie("access_token", accessToken, JwtExpiration/1000);
        response.addCookie(jwtCookie);

        Cookie refreshCookie = createTokenCookie("refresh_token", refreshToken, RefreshExpiration/1000);
        response.addCookie(refreshCookie);
    }

    public void register(AuthRegister request){
        System.out.println(request.getUserName() + " " + request.getPassword());
        Optional<User> userByUsername = userRepository.findByUserName(request.getUserName());
        Optional<User> userByEmail = userRepository.findByEmail(request.getEmail());

        if(userByUsername.isPresent())
            throw new RuntimeException("Username is already taken!");

        if(userByEmail.isPresent())
            throw new RuntimeException("Email is already taken!");

        User user = User
                .builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
    }

    public void login(AuthRequest request, HttpServletResponse response){
        System.out.println(request.getUserName() + " " + request.getPassword());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUserName(request.getUserName()).orElseThrow(() -> new UsernameNotFoundException("Username not exists!"));

        String jwtToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername()).getToken();

        attachCookies(response, jwtToken, refreshToken);
    }

    public void refreshToken(String refreshToken, HttpServletResponse response){
        RefreshToken validToken = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token not found!"));

        refreshTokenService.verifyExpiration(validToken);

        User user = validToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        Cookie jwtCookie = createTokenCookie("access_token", newAccessToken, JwtExpiration);
        response.addCookie(jwtCookie);
    }

    public void logout(String refreshToken, HttpServletResponse response){
        if(refreshToken != null)
            refreshTokenService.deleteByToken(refreshToken);

        Cookie jwtCookie = createTokenCookie("access_token", null, 0);
        response.addCookie(jwtCookie);

        Cookie refreshCookie = createTokenCookie("refresh_token", null, 0);
        response.addCookie(refreshCookie);
    }
}