package com.example.exposeit.integration;

import com.example.exposeit.Authentication.Entity.RefreshToken;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RepositoryIntegrationTest
 *
 * Verifies relational database persistence and database constraints in PostgreSQL.
 * Tests unique constraints on users, foreign key constraints on refresh tokens,
 * and foreign key references for posts.
 */
class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testUserRepository_Success() {
        User user = User.builder()
                .userName("john_doe")
                .email("john@example.com")
                .password("securePassword")
                .build();

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<User> foundByUsername = userRepository.findByUserName("john_doe");
        assertTrue(foundByUsername.isPresent());
        assertEquals("john@example.com", foundByUsername.get().getEmail());

        Optional<User> foundByEmail = userRepository.findByEmail("john@example.com");
        assertTrue(foundByEmail.isPresent());
        assertEquals("john_doe", foundByEmail.get().getUsername());
    }

    @Test
    void testUserRepository_DuplicateUsernameThrowsException() {
        User user1 = User.builder()
                .userName("dup_user")
                .email("user1@example.com")
                .password("pass")
                .build();
        userRepository.save(user1);

        User user2 = User.builder()
                .userName("dup_user")
                .email("user2@example.com")
                .password("pass")
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    void testUserRepository_DuplicateEmailThrowsException() {
        User user1 = User.builder()
                .userName("user1")
                .email("dup@example.com")
                .password("pass")
                .build();
        userRepository.save(user1);

        User user2 = User.builder()
                .userName("user2")
                .email("dup@example.com")
                .password("pass")
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    void testRefreshTokenRepository_ForeignKeyConstraint() {
        User nonPersistedUser = User.builder()
                .id(UUID.randomUUID())
                .userName("non_persisted")
                .email("non@example.com")
                .password("password")
                .build();

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiration(Instant.now().plusSeconds(100))
                .user(nonPersistedUser)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            refreshTokenRepository.saveAndFlush(token);
        });
    }

    @Test
    void testPostRepository_ForeignKeyConstraint() {
        Post post = Post.builder()
                .title("No Author Post")
                .description("Desc")
                .categories(Set.of(PostCategory.EDUCATION))
                .author(null) // null author will fail JPA check
                .build();

        assertThrows(Exception.class, () -> {
            postRepository.saveAndFlush(post);
        });
    }

    @Test
    void testPostRepository_Success() {
        User author = User.builder()
                .userName("author_user")
                .email("author@example.com")
                .password("pass")
                .build();
        userRepository.save(author);

        Post post = Post.builder()
                .title("My Title")
                .description("My Description")
                .categories(Set.of(PostCategory.EDUCATION))
                .author(author)
                .likes(5)
                .build();

        Post savedPost = postRepository.save(post);
        assertNotNull(savedPost.getId());
        assertEquals("My Title", savedPost.getTitle());
        assertEquals(author.getId(), savedPost.getAuthor().getId());
    }
}
