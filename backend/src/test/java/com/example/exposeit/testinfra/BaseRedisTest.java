package com.example.exposeit.testinfra;

/**
 * BaseRedisTest
 *
 * Base class for integration tests verifying Redis caching, ZSETs, and transient states.
 * Extends BaseIntegrationTest to inherit the containerized JVM registry and dynamic property binds.
 */
public abstract class BaseRedisTest extends BaseIntegrationTest {
}
