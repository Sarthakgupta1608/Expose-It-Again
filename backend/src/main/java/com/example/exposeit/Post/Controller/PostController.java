package com.example.exposeit.Post.Controller;

import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.DTO.PostResponse;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostDocument;
import com.example.exposeit.Post.Service.PostService;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody PostCreateRequest postRequest, Principal principal){ //add principal
        try {
            String username = principal.getName();
            PostResponse createdPost = postService.createPost(postRequest, username);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(createdPost);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not found or session expired!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/trending")
    public ResponseEntity<List<PostResponse>> getGlobalTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(postService.getGlobalTrending(page, size));
    }

    @GetMapping("/nearby/trending")
    public ResponseEntity<List<PostResponse>> getNearbyTrending(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(postService.getNearbyRecent(lat, lon, page, size));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<PostResponse>> getByCategory(
            @RequestParam Set<String> categories,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(postService.getPostByCategory(categories, page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostResponse>> getPostByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok(postService.getPostByKeyword(keyword, page, size));
    }
}