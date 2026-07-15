package com.example.exposeit.integration;

import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.DTO.PostResponse;
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
 * PostSyncIntegrationTest
 *
 * Verifies the database and Elasticsearch synchronization flow of the application.
 * Inserts a Post through PostService and asserts that it's successfully written to Postgres
 * and replicated as a PostDocument in Elasticsearch.
 */
class PostSyncIntegrationTest extends BaseIntegrationTest {

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
    void testPostCreationAndElasticSearchSync() {
        User author = User.builder()
                .userName("sync_author")
                .email("sync@example.com")
                .password("encoded_pass")
                .build();
        userRepository.save(author);

        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Sync Test Title");
        request.setDescription("Sync Test Description");
        request.setLatitude(40.7128);
        request.setLongitude(-74.0060);
        request.setCategories(Set.of(PostCategory.EDUCATION, PostCategory.INFRASTRUCTURE));
        request.setMediaFiles(List.of("http://example.com/sync.png"));
        request.setLikes(42);

        PostResponse response = postService.createPost(request, "sync_author");
        assertNotNull(response);
        UUID postId = UUID.fromString(response.getId());

        assertTrue(postRepository.findById(postId).isPresent());

        Optional<PostDocument> esDocOpt = esRepository.findById(postId);
        assertTrue(esDocOpt.isPresent());

        PostDocument doc = esDocOpt.get();
        assertEquals("Sync Test Title", doc.getTitle());
        assertEquals("Sync Test Description", doc.getDescription());
        assertEquals("sync_author", doc.getAuthorName());
        assertEquals(40.7128, doc.getLatitude());
        assertEquals(-74.0060, doc.getLongitude());
        assertEquals(42, doc.getLikes());
        assertNotNull(doc.getGeohash());
        assertNotNull(doc.getGeohashPrefix());
        assertTrue(doc.getCategories().contains("EDUCATION"));
        assertTrue(doc.getCategories().contains("INFRASTRUCTURE"));
        assertEquals(1, doc.getMediaFiles().size());
        assertEquals("http://example.com/sync.png", doc.getMediaFiles().get(0));
    }
}
