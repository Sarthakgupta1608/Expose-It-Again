package com.example.exposeit.Post.Repository;

import com.example.exposeit.Post.Entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
}
