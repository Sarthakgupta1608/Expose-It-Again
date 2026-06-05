package com.example.exposeit.Post.Entity;

import com.example.exposeit.User.Entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="posts", indexes = {
        @Index(name = "idx_post_author", columnList = "author_id")
})
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private Double latitude;
    private Double longitude;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "media_url")
    private List<String> mediaFiles;

    @ElementCollection(targetClass = PostCategory.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "post_categories", joinColumns = @JoinColumn(name = "post_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Set<PostCategory> categories;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_author"))
    private User author;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer likes=0;
}
