package com.example.exposeit.Authentication.Service;

import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    @Value("${application.security.refresh-expiration}")
    private Long RefreshExpiration;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshToken createRefreshToken(String username){
        User user = userRepository.findByUserName(username).orElseThrow(() -> new UsernameNotFoundException("User not Found!"));

        refreshTokenRepository.findByUser(user).ifPresent(oldToken -> {
            refreshTokenRepository.delete(oldToken);
            refreshTokenRepository.flush();
        });

        RefreshToken refreshToken = RefreshToken
                .builder()
                .token(UUID.randomUUID().toString())
                .expiration(Instant.now().plusMillis(RefreshExpiration))
                .user(user)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken refreshToken){
        if(refreshToken.getExpiration().isBefore(Instant.now())){
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalStateException("Refresh Token was Expired!");
        }

        return refreshToken;
    }

    @Transactional
    public void deleteByToken(String token){
        refreshTokenRepository.deleteByToken(token);
    }

    Optional<RefreshToken> findByToken(String token){
        return refreshTokenRepository.findByToken(token);
    }
}