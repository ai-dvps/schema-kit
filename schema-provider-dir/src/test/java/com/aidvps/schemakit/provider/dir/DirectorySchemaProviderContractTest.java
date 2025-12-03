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

package com.aidvps.schemakit.provider.dir;

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.schemakit.provider.BuiltInProviders;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Contract tests for DirectorySchemaProvider. */
class DirectorySchemaProviderContractTest {

    @TempDir Path tempDir;

    private DirectorySchemaProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DirectorySchemaProvider();
    }

    @Test
    void testImplementsSchemaProviderInterface() {
        // Assert
        assertTrue(provider instanceof SchemaProvider);
    }

    @Test
    void testGetSchemaWithValidDirectoryReturnsSchema()
            throws SchemaProviderException, IOException {
        // Arrange
        createTestDatabaseFile("test.db", "CREATE DATABASE test_db;");
        createTestTableFile(
                "users.tbl", "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255));");
        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder().directoryPath(tempDir.toString()).build();

        // Act
        Schema schema = provider.getSchema(config);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.hasTable("users"));
    }

    @Test
    void testGetSchemaWithNonExistentDirectoryThrowsException() {
        // Arrange
        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder()
                        .directoryPath("/non/existent/directory")
                        .build();

        // Act & Assert
        assertThrows(SchemaProviderException.class, () -> provider.getSchema(config));
    }

    @Test
    void testGetSchemaWithNullConfigThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> provider.getSchema(null));
    }

    @Test
    void testValidateConfigWithValidConfigDoesNotThrow() throws SchemaProviderException {
        // Arrange
        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder().directoryPath(tempDir.toString()).build();

        // Act & Assert
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    void testValidateConfigWithNullConfigThrowsException() {
        // Act & Assert
        assertThrows(SchemaProviderException.class, () -> provider.validateConfig(null));
    }

    @Test
    void testGetSchemaHandlesEmptyDirectory() throws SchemaProviderException {
        // Arrange
        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder().directoryPath(tempDir.toString()).build();

        // Act
        Schema schema = provider.getSchema(config);

        // Assert
        assertNotNull(schema);
        assertEquals(0, schema.getTableCount());
    }

    @Test
    void testGetSchemaParsesMultipleDatabaseFiles() throws SchemaProviderException, IOException {
        // Arrange
        createTestDatabaseFile("db1.db", "CREATE DATABASE db1;");
        createTestDatabaseFile("db2.db", "CREATE DATABASE db2;");
        createTestTableFile(
                "users.tbl", "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255));");
        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder().directoryPath(tempDir.toString()).build();

        // Act
        Schema schema = provider.getSchema(config);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.getTableCount() >= 1);
    }

    private void createTestDatabaseFile(String filename, String content) throws IOException {
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, content.getBytes());
    }

    private void createTestTableFile(String filename, String content) throws IOException {
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, content.getBytes());
    }
}
