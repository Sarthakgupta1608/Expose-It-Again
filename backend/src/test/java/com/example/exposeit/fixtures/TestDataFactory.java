package com.example.exposeit.fixtures;

import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.User.Entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TestDataFactory
 *
 * Exposes high-level static utility methods to construct fully configured,
 * random domain entity and request/response DTO structures.
 * Simplifies test data setup to single-line declarations.
 */
public class TestDataFactory {

    public static User createRandomUser() {
        return TestObjectBuilder.defaultUserBuilder().build();
    }

    public static Post createRandomPost(User author) {
        return TestObjectBuilder.defaultPostBuilder(author).build();
    }

    public static Post createRandomPost(User author, PostCategory category) {
        return TestObjectBuilder.defaultPostBuilder(author)
                .categories(Set.of(category))
                .build();
    }

    public static PostCreateRequest createPostCreateRequest() {
        return TestObjectBuilder.defaultPostCreateRequest();
    }

    public static List<Post> createBulkPosts(User author, int count) {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            posts.add(createRandomPost(author));
        }
        return posts;
    }
}
