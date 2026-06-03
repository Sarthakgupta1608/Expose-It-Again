package com.example.exposeit.Authentication.Repository;

import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.User.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByToken(String token);
    Optional<RefreshToken> findByUser(User user);
}
