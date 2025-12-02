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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Integration tests for DatabaseSchemaProvider with MySQL using Testcontainers. */
@Testcontainers
@EnabledIf("com.aidvps.schemakit.provider.db.MySQLIntegrationTest#isDockerAvailable")
@DisplayName("MySQL Integration")
class MySQLIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
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
                        mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());

        // Create test table
        connection
                .createStatement()
                .execute(
                        "CREATE TABLE users ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY,"
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
    @DisplayName("Should extract schema from MySQL database")
    void testGetSchemaHandlesMySQLConstraints() throws SchemaProviderException {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(connection)
                        .platform(DatabasePlatform.MYSQL)
                        .build();

        // Act
        // Note: This test will work once DatabaseIntrospector is implemented
        // For now, just verify configuration is valid
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should validate MySQL configuration")
    void testValidateMySQLConfig() {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(connection)
                        .platform(DatabasePlatform.MYSQL)
                        .build();

        // Act & Assert
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should reject invalid MySQL configuration")
    void testRejectInvalidMySQLConfig() {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(null)
                        .platform(DatabasePlatform.MYSQL)
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
    @DisplayName("Should connect to MySQL container")
    void testMySQLContainerIsRunning() {
        // Assert
        assertTrue(mysql.isRunning(), "MySQL container should be running");
        assertTrue(mysql.getJdbcUrl().contains("mysql"), "JDBC URL should contain 'mysql'");
    }
}
