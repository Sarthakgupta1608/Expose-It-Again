package com.example.exposeit.Post.Utils;

import com.example.exposeit.Post.Entity.Post;
import com.example.exposeit.Post.Entity.PostDocument;
import com.example.exposeit.Post.Repository.PostElasticSearchRepository;
import com.example.exposeit.Post.Repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer implements CommandLineRunner {

    private final ElasticsearchOperations elasticsearchOperations;
    private final PostRepository postRepository;
    private final PostElasticSearchRepository esRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing Elasticsearch Index for Posts...");
        
        IndexOperations indexOps = elasticsearchOperations.indexOps(PostDocument.class);
        
        try {
            if (indexOps.exists()) {
                log.info("Deleting existing 'posts' index...");
                indexOps.delete();
            }
            
            log.info("Creating 'posts' index with custom settings...");
            indexOps.create();
            
            log.info("Applying custom mappings for 'posts' index...");
            indexOps.putMapping();
            
            log.info("Index created and mapped successfully.");
        } catch (Exception e) {
            log.error("Failed to recreate index settings and mappings", e);
            throw e;
        }

        // Fetch all posts from relational DB and index them
        log.info("Reindexing all posts from PostgreSQL database into Elasticsearch...");
        List<Post> posts = postRepository.findAll();
        log.info("Found {} posts in DB to index.", posts.size());
        
        List<PostDocument> docs = posts.stream().map(post -> {
            String exactGeoHash = SpatialIndexingUtils.getExactGeoHash(post.getLatitude(), post.getLongitude());
            String prefixGeoHash = SpatialIndexingUtils.getBoundingBoxPrefix(post.getLatitude(), post.getLongitude());
            
            Set<String> stringCategories = post.getCategories().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            
            return PostDocument.builder()
                    .id(post.getId())
                    .authorName(post.getAuthor().getUsername())
                    .title(post.getTitle())
                    .description(post.getDescription())
                    .latitude(post.getLatitude())
                    .longitude(post.getLongitude())
                    .geohash(exactGeoHash)
                    .geohashPrefix(prefixGeoHash)
                    .categories(stringCategories)
                    .mediaFiles(post.getMediaFiles())
                    .likes(post.getLikes())
                    .createdAtEpoch(post.getCreatedAt().toEpochMilli())
                    .build();
        }).collect(Collectors.toList());
        
        if (!docs.isEmpty()) {
            esRepository.saveAll(docs);
            log.info("Successfully reindexed {} posts into Elasticsearch.", docs.size());
        } else {
            log.info("No posts to reindex.");
        }
    }
}
