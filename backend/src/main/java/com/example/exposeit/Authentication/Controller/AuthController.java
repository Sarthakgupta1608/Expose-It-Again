package com.example.exposeit.Authentication.Controller;

import com.example.exposeit.Authentication.DTO.AuthRegister;
import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Authentication.Service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthRegister request){
        try{
            authenticationService.register(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Registration Successful!");
        }catch (RuntimeException e){
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody AuthRequest request, HttpServletResponse response){
        try{
            authenticationService.login(request, response);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Login Successful!");
        } catch (AuthenticationException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Credentials!");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ){
        if(refreshToken == null){
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("No Refresh Token found!");
        }

        try {
            authenticationService.refreshToken(refreshToken, response);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Refreshed");
        } catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ){
        authenticationService.logout(refreshToken, response);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Success!");
    }
}
