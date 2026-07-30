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
