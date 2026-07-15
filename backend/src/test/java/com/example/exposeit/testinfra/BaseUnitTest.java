package com.example.exposeit.testinfra;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BaseUnitTest
 *
 * Base class for pure business logic unit tests.
 * Prevents booting the Spring Boot Context and relies entirely on Mockito
 * mock injection to keep business tests running in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
}
