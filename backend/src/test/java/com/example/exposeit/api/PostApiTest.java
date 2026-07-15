package com.example.exposeit.api;

import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Authentication.Repository.RefreshTokenRepository;
import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.Post.Entity.PostDocument;
import com.example.exposeit.Post.Repository.PostElasticSearchRepository;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import com.example.exposeit.Post.Utils.SpatialIndexingUtils;
import com.example.exposeit.testinfra.BaseApiTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PostApiTest
 *
 * Black-box REST API contract validation tests for Post endpoints (/api/posts/**) using MockMvc.
 * Validates post creation payloads, access authorization constraints, and query/search endpoints.
 */
class PostApiTest extends BaseApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PostElasticSearchRepository esRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Cookie accessTokenCookie;

    @BeforeEach
    void cleanDbAndLogin() throws Exception {
        esRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .userName("post_api_user")
                .email("postapi@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequest("post_api_user", "password"))))
                .andReturn();

        accessTokenCookie = loginResult.getResponse().getCookie("access_token");
        assertNotNull(accessTokenCookie);
    }

    @Test
    void testCreatePost_Success() throws Exception {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("API Post Title");
        request.setDescription("API Post Description");
        request.setLatitude(34.0522);
        request.setLongitude(-118.2437);
        request.setCategories(Set.of(PostCategory.EDUCATION));
        request.setMediaFiles(List.of("http://example.com/api.png"));
        request.setLikes(15);

        mockMvc.perform(post("/api/posts")
                .cookie(accessTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("API Post Title")))
                .andExpect(jsonPath("$.description", equalTo("API Post Description")))
                .andExpect(jsonPath("$.author", equalTo("post_api_user")))
                .andExpect(jsonPath("$.likes", equalTo(15)));
    }

    @Test
    void testCreatePost_ValidationFailure() throws Exception {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle(""); // Invalid empty title
        request.setDescription("API Post Description");

        mockMvc.perform(post("/api/posts")
                .cookie(accessTokenCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreatePost_AuthFailure() throws Exception {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("API Post Title");
        request.setDescription("API Post Description");

        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetGlobalTrending_Success() throws Exception {
        PostDocument doc = PostDocument.builder()
                .id(UUID.randomUUID())
                .authorName("post_api_user")
                .title("API Trending Title")
                .description("API Description")
                .latitude(34.0522)
                .longitude(-118.2437)
                .geohash("9q5ctfm")
                .geohashPrefix("9q5c")
                .categories(Set.of("EDUCATION"))
                .mediaFiles(Collections.emptyList())
                .likes(50)
                .createdAtEpoch(System.currentTimeMillis())
                .build();
        esRepository.save(doc);

        mockMvc.perform(get("/api/posts/trending")
                .cookie(accessTokenCookie)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("API Trending Title")));
    }

    @Test
    void testGetNearbyTrending_Success() throws Exception {
        double lat = 37.7749;
        double lon = -122.4194;
        String geohash = SpatialIndexingUtils.getExactGeoHash(lat, lon);
        String geohashPrefix = SpatialIndexingUtils.getBoundingBoxPrefix(lat, lon);

        PostDocument doc = PostDocument.builder()
                .id(UUID.randomUUID())
                .authorName("post_api_user")
                .title("API Nearby Title")
                .description("API Description")
                .latitude(lat)
                .longitude(lon)
                .geohash(geohash)
                .geohashPrefix(geohashPrefix)
                .categories(Set.of("EDUCATION"))
                .mediaFiles(Collections.emptyList())
                .likes(5)
                .createdAtEpoch(System.currentTimeMillis())
                .build();
        esRepository.save(doc);

        mockMvc.perform(get("/api/posts/nearby/trending")
                .cookie(accessTokenCookie)
                .param("lat", String.valueOf(lat))
                .param("lon", String.valueOf(lon))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("API Nearby Title")));
    }

    @Test
    void testGetByCategory_Success() throws Exception {
        double lat = 37.7749;
        double lon = -122.4194;
        String geohash = SpatialIndexingUtils.getExactGeoHash(lat, lon);
        String geohashPrefix = SpatialIndexingUtils.getBoundingBoxPrefix(lat, lon);

        PostDocument doc = PostDocument.builder()
                .id(UUID.randomUUID())
                .authorName("post_api_user")
                .title("API Category Title")
                .description("API Description")
                .latitude(lat)
                .longitude(lon)
                .geohash(geohash)
                .geohashPrefix(geohashPrefix)
                .categories(Set.of("GARBAGE"))
                .mediaFiles(Collections.emptyList())
                .likes(5)
                .createdAtEpoch(System.currentTimeMillis())
                .build();
        esRepository.save(doc);

        mockMvc.perform(get("/api/posts/filter")
                .cookie(accessTokenCookie)
                .param("categories", "GARBAGE")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("API Category Title")));
    }

    @Test
    void testGetByKeyword_Success() throws Exception {
        double lat = 37.7749;
        double lon = -122.4194;
        String geohash = SpatialIndexingUtils.getExactGeoHash(lat, lon);
        String geohashPrefix = SpatialIndexingUtils.getBoundingBoxPrefix(lat, lon);

        PostDocument doc = PostDocument.builder()
                .id(UUID.randomUUID())
                .authorName("post_api_user")
                .title("UniqueKeywordPost")
                .description("API Description")
                .latitude(lat)
                .longitude(lon)
                .geohash(geohash)
                .geohashPrefix(geohashPrefix)
                .categories(Set.of("GARBAGE"))
                .mediaFiles(Collections.emptyList())
                .likes(5)
                .createdAtEpoch(System.currentTimeMillis())
                .build();
        esRepository.save(doc);

        mockMvc.perform(get("/api/posts/search")
                .cookie(accessTokenCookie)
                .param("keyword", "UniqueKeywordPost")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("UniqueKeywordPost")));
    }
}
