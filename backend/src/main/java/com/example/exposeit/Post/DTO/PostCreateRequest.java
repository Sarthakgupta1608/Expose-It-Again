package com.example.exposeit.Post.DTO;

import com.example.exposeit.Post.Entity.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class PostCreateRequest {
    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Description cannot be empty")
    private String description;

    @NotNull(message = "latitude is required")
    private Double latitude;

    @NotNull(message = "longitude is required")
    private Double longitude;

    @NotEmpty(message = "At least one category is required")
    private Set<PostCategory> categories;

    private List<String> mediaFiles;

    private Integer likes;
}
