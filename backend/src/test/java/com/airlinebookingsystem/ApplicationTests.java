package com.airlinebookingsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the application context starts against a real, empty Postgres.
 *
 * <p>This is the only test that touches a database, and it checks more than
 * bean wiring: the container starts empty, so Flyway replays every migration
 * from V1, and {@code ddl-auto=validate} then checks the JPA entities against
 * the schema those migrations actually produced. Drift between an entity and
 * a migration fails here rather than on deployment.
 *
 * <p>Requires a running Docker daemon. The image is pinned to the same major
 * version as production — testing against a different one would defeat the
 * point of using a real database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18");

    @Test
    void contextLoads() {
    }
}
