package com.example.exposeit.testinfra.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.GenericContainer;

/**
 * ContainerRegistry
 *
 * Singleton-pattern static container registry for ExposeIT Testing Platform.
 * Spins up Postgres, Elasticsearch, and Redis containers exactly once per JVM session
 * to minimize startup overhead across different integration tests.
 */
public class ContainerRegistry {

    public static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("exposeit_database")
                    .withUsername("exposeit_user_hakim")
                    .withPassword("hakim_op_in_the_chat");

    public static final ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.4.2-amd64")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    public static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    static {
        postgres.start();
        elasticsearch.start();
        redis.start();
    }
}
