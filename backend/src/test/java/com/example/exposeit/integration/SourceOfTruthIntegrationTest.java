package com.example.exposeit.integration;

import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.DTO.PostResponse;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.Post.Entity.PostDocument;
import com.example.exposeit.Post.Repository.PostElasticSearchRepository;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.Post.Service.PostService;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SourceOfTruthIntegrationTest
 *
 * Verifies that PostgreSQL acts as the single source of truth for post data, even when
 * a mismatch exists between PostgreSQL and Elasticsearch (e.g. simulated data out-of-sync).
 */
class SourceOfTruthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostElasticSearchRepository esRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        esRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testPostgresIsSourceOfTruthWhenMismatchOccurs() {
        User author = User.builder()
                .userName("truth_author")
                .email("truth@example.com")
                .password("password")
                .build();
        userRepository.save(author);

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Trusted Database Title");
        request.setDescription("Authoritative Description");
        request.setLatitude(34.0522);
        request.setLongitude(-118.2437);
        request.setCategories(Set.of(PostCategory.CORRUPTION));
        request.setMediaFiles(List.of("http://example.com/truth.png"));
        request.setLikes(100);

        PostResponse response = postService.createPost(request, "truth_author");
        UUID postId = UUID.fromString(response.getId());

        Optional<Post> dbPostBefore = postRepository.findById(postId);
        Optional<PostDocument> esDocBefore = esRepository.findById(postId);
        assertTrue(dbPostBefore.isPresent());
        assertTrue(esDocBefore.isPresent());
        assertEquals("Trusted Database Title", dbPostBefore.get().getTitle());
        assertEquals("Trusted Database Title", esDocBefore.get().getTitle());

        PostDocument corruptedDoc = esDocBefore.get();
        corruptedDoc.setTitle("Corrupted Elasticsearch Title");
        esRepository.save(corruptedDoc);

        Optional<PostDocument> esDocAfter = esRepository.findById(postId);
        assertTrue(esDocAfter.isPresent());
        assertEquals("Corrupted Elasticsearch Title", esDocAfter.get().getTitle());

        Optional<Post> dbPostAfter = postRepository.findById(postId);
        assertTrue(dbPostAfter.isPresent());
        assertEquals("Trusted Database Title", dbPostAfter.get().getTitle());
    }
}
