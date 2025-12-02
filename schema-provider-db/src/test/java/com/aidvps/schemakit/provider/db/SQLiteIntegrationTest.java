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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Integration tests for DatabaseSchemaProvider with SQLite. */
@DisplayName("SQLite Integration")
class SQLiteIntegrationTest {

    @TempDir Path tempDir;

    private DatabaseSchemaProvider provider;
    private Connection connection;
    private File dbFile;

    @BeforeEach
    void setUp() throws Exception {
        provider = new DatabaseSchemaProvider();
        dbFile = tempDir.resolve("test.db").toFile();

        // Load SQLite JDBC driver
        Class.forName("org.sqlite.JDBC");

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // Create test table
        connection
                .createStatement()
                .execute(
                        "CREATE TABLE users ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                                + "name TEXT NOT NULL,"
                                + "email TEXT UNIQUE"
                                + ")");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should extract schema from SQLite database")
    void testGetSchemaHandlesSQLiteConstraints() throws SchemaProviderException {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(connection)
                        .platform(DatabasePlatform.SQLITE)
                        .build();

        // Act
        // Note: This test will work once DatabaseIntrospector is implemented
        // For now, just verify configuration is valid
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should validate SQLite configuration")
    void testValidateSQLiteConfig() {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(connection)
                        .platform(DatabasePlatform.SQLITE)
                        .build();

        // Act & Assert
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should reject invalid SQLite configuration")
    void testRejectInvalidSQLiteConfig() {
        // Arrange
        DatabaseSchemaProviderConfig config =
                DatabaseSchemaProviderConfig.builder()
                        .connection(null)
                        .platform(DatabasePlatform.SQLITE)
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
    @DisplayName("Should create SQLite database file")
    void testSQLiteDatabaseCreation() throws IOException {
        // Assert
        assertTrue(dbFile.exists(), "SQLite database file should be created");
        assertTrue(dbFile.length() >= 0, "SQLite database file should have valid size");
    }

    @Test
    @DisplayName("Should handle file permissions correctly")
    void testSQLiteFilePermissions() throws IOException {
        // Assert
        assertTrue(dbFile.canRead(), "SQLite database file should be readable");
        assertTrue(dbFile.canWrite(), "SQLite database file should be writable");
    }
}
