package com.example.exposeit.fixtures;

import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.User.Entity.User;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TestDataFactoryTest
 *
 * A context-free verification test verifying that all data generation helpers,
 * Lombok builder templates, factories, and JWT generators compile and execute cleanly
 * without any runtime failures or null outputs.
 */
class TestDataFactoryTest {

    @Test
    void testFactoriesProduceValidObjects() {
        User user = TestDataFactory.createRandomUser();
        assertNotNull(user);
        assertNotNull(user.getUsername());
        assertNotNull(user.getEmail());
        assertNotNull(user.getPassword());

        Post post = TestDataFactory.createRandomPost(user);
        assertNotNull(post);
        assertEquals(user, post.getAuthor());
        assertNotNull(post.getTitle());

        Post postWithCat = TestDataFactory.createRandomPost(user, PostCategory.GARBAGE);
        assertNotNull(postWithCat);
        assertTrue(postWithCat.getCategories().contains(PostCategory.GARBAGE));

        PostCreateRequest request = TestDataFactory.createPostCreateRequest();
        assertNotNull(request);
        assertNotNull(request.getTitle());

        List<Post> bulkPosts = TestDataFactory.createBulkPosts(user, 5);
        assertNotNull(bulkPosts);
        assertEquals(5, bulkPosts.size());

        String token = JwtFixture.generateTokenOffline(user);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
}
