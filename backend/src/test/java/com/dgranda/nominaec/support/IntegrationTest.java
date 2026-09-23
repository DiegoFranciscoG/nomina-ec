package com.dgranda.nominaec.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Base class for integration tests: a real PostgreSQL 16 (same engine as Neon) started once by
 * Testcontainers and shared by all test classes, Flyway applying every migration, and "today"
 * pinned to 2026-09-23 for deterministic results.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTest.FixedClock.class)
public abstract class IntegrationTest {

    public static final Instant TODAY = Instant.parse("2026-09-23T15:00:00Z");

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.10-alpine");

    static {
        POSTGRES.start();
    }

    /** Test-only values; they never leave the test JVM. */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-tests-only-signing-key-0123456789abcdef");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:4200");
        registry.add("app.bootstrap.admin-username", () -> "admin");
        registry.add("app.bootstrap.admin-password", () -> "integration-admin-pass");
        registry.add("app.bootstrap.demo-username", () -> "demo");
        registry.add("app.bootstrap.demo-password", () -> "integration-demo-pass");
        registry.add("app.demo-data", () -> "false");
        registry.add("app.login-rate-limit.max-attempts", () -> "5");
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class FixedClock {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY, ZoneId.of("America/Guayaquil"));
        }
    }
}
