package com.example.exposeit.fixtures;

import com.example.exposeit.Authentication.DTO.AuthRegister;
import com.example.exposeit.Authentication.DTO.AuthRequest;
import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.User.Entity.Role;
import com.example.exposeit.User.Entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TestObjectBuilder
 *
 * Exposes template builders pre-populated with realistic random values
 * for entities (User, Post) and DTOs (PostCreateRequest, AuthRegister, AuthRequest).
 * Allows test suites to customize only the properties relevant to the specific test scenario.
 */
public class TestObjectBuilder {

    public static User.UserBuilder defaultUserBuilder() {
        return User.builder()
                .id(RandomDataGenerator.randomUuid())
                .userName(RandomDataGenerator.randomUsername())
                .email(RandomDataGenerator.randomEmail())
                .password(RandomDataGenerator.randomPassword())
                .role(Role.USER)
                .bio("Bio for " + RandomDataGenerator.randomString(5));
    }

    public static Post.PostBuilder defaultPostBuilder(User author) {
        return Post.builder()
                .id(RandomDataGenerator.randomUuid())
                .title("Post Title " + RandomDataGenerator.randomString(8))
                .description("Post description detailing " + RandomDataGenerator.randomString(20))
                .latitude(RandomDataGenerator.randomLatitude())
                .longitude(RandomDataGenerator.randomLongitude())
                .author(author)
                .categories(Set.of(RandomDataGenerator.randomCategory()))
                .mediaFiles(List.of("http://example.com/media1.jpg"))
                .likes(0)
                .createdAt(Instant.now());
    }

    public static PostCreateRequest defaultPostCreateRequest() {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("Post Title " + RandomDataGenerator.randomString(8));
        request.setDescription("Post description detailing " + RandomDataGenerator.randomString(20));
        request.setLatitude(RandomDataGenerator.randomLatitude());
        request.setLongitude(RandomDataGenerator.randomLongitude());
        request.setCategories(Set.of(RandomDataGenerator.randomCategory()));
        request.setMediaFiles(List.of("http://example.com/media1.jpg"));
        request.setLikes(0);
        return request;
    }

    public static AuthRegister defaultAuthRegister() {
        return AuthRegister.builder()
                .userName(RandomDataGenerator.randomUsername())
                .email(RandomDataGenerator.randomEmail())
                .password(RandomDataGenerator.randomPassword())
                .build();
    }

    public static AuthRequest defaultAuthRequest() {
        return AuthRequest.builder()
                .userName(RandomDataGenerator.randomUsername())
                .password(RandomDataGenerator.randomPassword())
                .build();
    }
}
