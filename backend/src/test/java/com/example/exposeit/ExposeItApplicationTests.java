package com.example.exposeit;

import com.example.exposeit.testinfra.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * ExposeItApplicationTests
 *
 * Serving as our permanent integration and infrastructure health check smoke test.
 * Extends BaseIntegrationTest to spin up container dependencies (Postgres, Elasticsearch, Redis)
 * and dynamically wire database/search connection properties to verify the Spring context loads cleanly.
 */
class ExposeItApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }

}
