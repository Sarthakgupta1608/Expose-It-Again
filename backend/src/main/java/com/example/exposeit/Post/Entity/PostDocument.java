package com.example.exposeit.Post.Entity;

import org.springframework.data.annotation.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(indexName = "posts")
public class PostDocument {
    @Id
    private UUID id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    //update to authorId Denormalize later
    @Field(type = FieldType.Keyword)
    private String authorName;

    private Double latitude;
    private Double longitude;
//    @Field(type = FieldType.Keyword)
    private String geohash;

    @Field(type = FieldType.Keyword)
    private String geohashPrefix;

    @Field(type = FieldType.Keyword)
    private Set<String> categories;

    @Field(type = FieldType.Keyword)
    private List<String> mediaFiles;

    private Integer likes;
    private Long createdAtEpoch;
}
