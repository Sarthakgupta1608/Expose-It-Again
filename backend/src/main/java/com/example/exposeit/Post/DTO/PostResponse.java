package com.example.exposeit.Post.DTO;

import com.example.exposeit.Post.Entity.PostCategory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class PostResponse {
    private String id;
    private String title;
    private String description;
    private Double latitude;
    private Double longitude;
    private List<String> mediaFiles;
    private Set<PostCategory> categories;
    private String author;
    private Instant createdAt;
    private Integer likes;
}
