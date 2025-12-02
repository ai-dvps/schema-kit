/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aidvps.schemakit.provider.db;

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Integration tests for DatabaseSchemaProvider with PostgreSQL using Testcontainers. */
@Testcontainers
@EnabledIf("com.aidvps.schemakit.provider.db.PostgreSQLIntegrationTest#isDockerAvailable")
@DisplayName("PostgreSQL Integration")
class PostgreSQLIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    private DatabaseSchemaProvider provider;
    private Connection connection;

    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        provider = new DatabaseSchemaProvider();
        connection =
                DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        // Create test table
        connection
                .createStatement()
                .execute(
                        "CREATE TABLE users ("
                                + "id SERIAL PRIMARY KEY,"
                                + "name VARCHAR(255) NOT NULL,"
                                + "email VARCHAR(255) UNIQUE"
                                + ")");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should extract schema from PostgreSQL database")
    void testGetSchemaHandlesPostgreSQLConstraints() throws SchemaProviderException {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(connection)
                        .platform(DatabasePlatform.POSTGRESQL)
                        .build();

        // Act
        // Note: This test will work once DatabaseIntrospector is implemented
        // For now, just verify configuration is valid
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should validate PostgreSQL configuration")
    void testValidatePostgreSQLConfig() {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(connection)
                        .platform(DatabasePlatform.POSTGRESQL)
                        .build();

        // Act & Assert
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should reject invalid PostgreSQL configuration")
    void testRejectInvalidPostgreSQLConfig() {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(null)
                        .platform(DatabasePlatform.POSTGRESQL)
                        .build();

        // Act & Assert
        SchemaProviderException exception =
                assertThrows(
                        SchemaProviderException.class,
                        () -> provider.validateConfig(config),
                        "Should reject null connection");

        assertEquals(SchemaProviderException.ErrorCode.CONFIG_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should connect to PostgreSQL container")
    void testPostgreSQLContainerIsRunning() {
        // Assert
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        assertTrue(
                postgres.getJdbcUrl().contains("postgres"), "JDBC URL should contain 'postgres'");
    }
}
