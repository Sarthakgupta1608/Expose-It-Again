package com.example.exposeit.Post.Service;

import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.DTO.PostResponse;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.Post.Entity.PostDocument;
import com.example.exposeit.Post.Repository.PostElasticSearchRepository;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.testinfra.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PostServiceTest
 *
 * Tests post creation logic, Elasticsearch sync, and trending/nearby/category/keyword searches.
 * Verifies that spatial geohash indices are generated, entities are saved to database and ES,
 * pagination is respected, and responses are correctly mapped from database/document entities.
 */
class PostServiceTest extends BaseUnitTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostElasticSearchRepository esRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private PostCreateRequest createRequest;
    private Post savedPost;
    private PostDocument postDoc;
    private UUID postId;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();

        testUser = User.builder()
                .userName("testuser")
                .email("test@example.com")
                .build();

        createRequest = new PostCreateRequest();
        createRequest.setTitle("Test Post");
        createRequest.setDescription("This is a description");
        createRequest.setLatitude(37.7749);
        createRequest.setLongitude(-122.4194);
        createRequest.setMediaFiles(List.of("http://example.com/image.jpg"));
        createRequest.setCategories(Set.of(PostCategory.EDUCATION));
        createRequest.setLikes(10);

        savedPost = Post.builder()
                .id(postId)
                .title("Test Post")
                .description("This is a description")
                .latitude(37.7749)
                .longitude(-122.4194)
                .mediaFiles(List.of("http://example.com/image.jpg"))
                .categories(Set.of(PostCategory.EDUCATION))
                .author(testUser)
                .createdAt(Instant.now())
                .likes(10)
                .build();

        postDoc = PostDocument.builder()
                .id(postId)
                .authorName("testuser")
                .title("Test Post")
                .description("This is a description")
                .latitude(37.7749)
                .longitude(-122.4194)
                .geohash("9q9h1fn")
                .geohashPrefix("9q9h")
                .categories(Set.of("EDUCATION"))
                .mediaFiles(List.of("http://example.com/image.jpg"))
                .likes(10)
                .createdAtEpoch(savedPost.getCreatedAt().toEpochMilli())
                .build();
    }

    @Test
    void testCreatePost_Success() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(testUser));
        when(postRepository.saveAndFlush(any(Post.class))).thenReturn(savedPost);

        PostResponse response = postService.createPost(createRequest, "testuser");

        assertNotNull(response);
        assertEquals(postId.toString(), response.getId());
        assertEquals("Test Post", response.getTitle());
        assertEquals("testuser", response.getAuthor());

        verify(postRepository).saveAndFlush(any(Post.class));
        verify(esRepository).save(any(PostDocument.class));
    }

    @Test
    void testCreatePost_UserNotFound() {
        when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> postService.createPost(createRequest, "unknown"));

        verify(postRepository, never()).saveAndFlush(any());
        verify(esRepository, never()).save(any());
    }

    @Test
    void testGetGlobalTrending() {
        Page<PostDocument> pageResult = new PageImpl<>(List.of(postDoc));
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "likes"));
        when(esRepository.findAll(pageRequest)).thenReturn(pageResult);

        List<PostResponse> trending = postService.getGlobalTrending(0, 10);

        assertNotNull(trending);
        assertEquals(1, trending.size());
        assertEquals(postId.toString(), trending.get(0).getId());
        assertEquals("Test Post", trending.get(0).getTitle());
    }

    @Test
    void testGetNearbyRecent() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAtEpoch"));
        when(esRepository.findByGeohashPrefixIn(anyList(), any(PageRequest.class))).thenReturn(List.of(postDoc));

        List<PostResponse> nearby = postService.getNearbyRecent(37.7749, -122.4194, 0, 10);

        assertNotNull(nearby);
        assertEquals(1, nearby.size());
        verify(esRepository).findByGeohashPrefixIn(anyList(), any(PageRequest.class));
    }

    @Test
    void testGetPostByCategory() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAtEpoch"));
        Set<String> categories = Set.of("EDUCATION");
        when(esRepository.findByCategoriesIn(eq(categories), any(PageRequest.class))).thenReturn(List.of(postDoc));

        List<PostResponse> byCategory = postService.getPostByCategory(categories, 0, 10);

        assertNotNull(byCategory);
        assertEquals(1, byCategory.size());
    }

    @Test
    void testGetPostByKeyword() {
        Page<PostDocument> pageResult = new PageImpl<>(List.of(postDoc));
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(esRepository.searchByKeyword(eq("test"), any(PageRequest.class))).thenReturn(pageResult);

        List<PostResponse> byKeyword = postService.getPostByKeyword("test", 0, 10);

        assertNotNull(byKeyword);
        assertEquals(1, byKeyword.size());
    }
}
