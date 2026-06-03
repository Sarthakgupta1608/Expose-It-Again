package com.example.exposeit.Post.Service;

import com.example.exposeit.Post.DTO.PostCreateRequest;
import com.example.exposeit.Post.DTO.PostResponse;
import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostCategory;
import com.example.exposeit.Post.Entity.PostDocument;
import com.example.exposeit.Post.Repository.PostElasticSearchRepository;
import com.example.exposeit.Post.Repository.PostRepository;
import com.example.exposeit.Post.Utils.SpatialIndexingUtils;
import com.example.exposeit.User.Entity.User;
import com.example.exposeit.User.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostElasticSearchRepository esRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResponse createPost(PostCreateRequest request, String username)
    {
        User author = userRepository.findByUserName(username)
                .orElseThrow(()->new UsernameNotFoundException("User not found!"));

        Post post = Post.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .mediaFiles(request.getMediaFiles())
                .categories(request.getCategories())
                .author(author)
                .build();

        Post savedPost = postRepository.saveAndFlush(post);

        String exactGeoHash = SpatialIndexingUtils.getExactGeoHash(savedPost.getLatitude(), savedPost.getLongitude());
        String prefixGeoHash = SpatialIndexingUtils.getBoundingBoxPrefix(savedPost.getLatitude(), savedPost.getLongitude());

        Set<String> stringCategories = savedPost.getCategories().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        PostDocument esDoc = PostDocument.builder()
                .id(savedPost.getId())
                .authorName(author.getUsername())
                .title(savedPost.getTitle())
                .description(savedPost.getDescription())
                .latitude(savedPost.getLatitude())
                .longitude(savedPost.getLongitude())
                .geohash(exactGeoHash)
                .geohashPrefix(prefixGeoHash)
                .categories(stringCategories)
                .mediaFiles(savedPost.getMediaFiles())
                .likes(savedPost.getLikes())
                .createdAtEpoch(savedPost.getCreatedAt().toEpochMilli())
                .build();

        esRepository.save(esDoc);
        return mapToResponse(savedPost);
    }


    public List<PostResponse> getGlobalTrending(int page, int size){
        PageRequest pageRequest= PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likes"));
        List<PostDocument> docs = esRepository.findAll(pageRequest).getContent();

        return docs.stream().map(this::mapDocToResponse).collect(Collectors.toList());
    }

    public List<PostResponse> getNearbyRecent(double lat, double lon, int page, int size){
        List<String> searchPrefixes = SpatialIndexingUtils.getNineBoxGeohashes(lat, lon);
        Sort recentSort = Sort.by(Sort.Direction.DESC, "createdAtEpoch");

        PageRequest pageRequest = PageRequest.of(page, size, recentSort);
        List<PostDocument> docs = esRepository.findByGeohashPrefixIn(searchPrefixes, pageRequest);

        return docs.stream().map(this::mapDocToResponse).collect(Collectors.toList());
    }

    public List<PostResponse> getPostByCategory(Set<String> categories, int page, int size){
        Sort recentSort = Sort.by(Sort.Direction.DESC, "createdAtEpoch");

        PageRequest pageRequest = PageRequest.of(page, size, recentSort);
        List<PostDocument> docs = esRepository.findByCategoriesIn(categories, pageRequest);

        return docs.stream().map(this::mapDocToResponse).collect(Collectors.toList());
    }

    public List<PostResponse> getPostByKeyword(String keyword, int page, int size)
    {
        PageRequest pageRequest = PageRequest.of(page, size);

        List<PostDocument> docs = esRepository.searchByKeyword(keyword, pageRequest).getContent();
        return docs.stream().map(this::mapDocToResponse).collect(Collectors.toList());
    }

    private PostResponse mapToResponse(Post post){
        return PostResponse.builder()
                .id(post.getId().toString())
                .title(post.getTitle())
                .description(post.getDescription())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .mediaFiles(post.getMediaFiles())
                .categories(post.getCategories())
                .author(post.getAuthor().getUsername())
                .createdAt(post.getCreatedAt())
                .likes(post.getLikes())
                .build();
    }

    private PostResponse mapDocToResponse(PostDocument doc){
        return PostResponse.builder()
                .id(doc.getId().toString())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .mediaFiles(doc.getMediaFiles())
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .categories(doc.getCategories().stream()
                        .map(PostCategory::valueOf)
                        .collect(Collectors.toSet()))
                .author(doc.getAuthorName())
                .createdAt(Instant.ofEpochMilli(doc.getCreatedAtEpoch()))
                .likes(doc.getLikes())
                .build();
    }
}
