package com.example.exposeit.testinfra;

import com.example.exposeit.testinfra.containers.ContainerRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * BaseIntegrationTest
 *
 * Base class for integration tests requiring the full Spring Boot application context.
 * Utilizes Spring's DynamicPropertySource to dynamically override property configurations
 * with the connection coordinates of the started test containers (Postgres, Elasticsearch, Redis).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ContainerRegistry.postgres::getJdbcUrl);
        registry.add("spring.datasource.username", ContainerRegistry.postgres::getUsername);
        registry.add("spring.datasource.password", ContainerRegistry.postgres::getPassword);
        registry.add("spring.elasticsearch.uris", () -> "http://" + ContainerRegistry.elasticsearch.getHttpHostAddress());
        
        // Future-proofing Redis properties
        registry.add("spring.data.redis.host", ContainerRegistry.redis::getHost);
        registry.add("spring.data.redis.port", () -> ContainerRegistry.redis.getMappedPort(6379));
    }
}
