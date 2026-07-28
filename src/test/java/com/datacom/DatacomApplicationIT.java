package com.datacom;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Boots the whole application against a real, ephemeral PostgreSQL 17 container and verifies that
 * the Flyway migration (V1__init_schema.sql) actually created the target schema — the L0 proof that
 * the socle works end to end, not just that the classes compile.
 *
 * <p>WebEnvironment.MOCK (not NONE): Spring Security's SecurityFilterChain bean needs a servlet
 * context to obtain its HttpSecurity bean, even though this test never issues an HTTP request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class DatacomApplicationIT {

    @Container @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private DataSource dataSource;

    @Test
    void contextLoadsAndFlywayMigrationCreatesTargetSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            assertThat(tableExists(metadata, "users")).isTrue();
            assertThat(tableExists(metadata, "products")).isTrue();
            assertThat(tableExists(metadata, "audit_entry")).isTrue();
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws Exception {
        try (ResultSet rs = metadata.getTables(null, "public", tableName, new String[] {"TABLE"})) {
            return rs.next();
        }
    }
}
