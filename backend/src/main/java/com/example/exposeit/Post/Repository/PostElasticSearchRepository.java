package com.example.exposeit.Post.Repository;

import com.example.exposeit.Post.Entity.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PostElasticSearchRepository extends ElasticsearchRepository<PostDocument, UUID> {
    List<PostDocument> findByGeohashPrefixIn(List<String> geohashes, PageRequest pageRequest);

    List<PostDocument> findByCategoriesIn(Set<String> categories, PageRequest pageRequest);

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"description\"], \"fuzziness\": \"AUTO\"}}")
    Page<PostDocument> searchByKeyword(String keyword, Pageable pageable);
}
