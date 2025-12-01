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
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Integration test for directory file parsing. */
class DirectoryFileParsingIntegrationTest {

    @TempDir Path tempDir;

    private DirectorySchemaProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DirectorySchemaProvider();
    }

    @Test
    void testParseDatabaseFile() throws IOException, SchemaProviderException {
        // Arrange
        String dbContent = "CREATE DATABASE myapp;";
        Path dbFile = tempDir.resolve("myapp.db");
        Files.write(dbFile, dbContent.getBytes());

        DatabaseFileParser parser = new DatabaseFileParser();

        // Act
        String databaseName = parser.parseDatabaseName(dbFile);

        // Assert
        assertNotNull(databaseName);
        assertEquals("myapp", databaseName);
    }

    @Test
    void testParseTableFile() throws IOException, SchemaProviderException {
        // Arrange
        String tblContent =
                "CREATE TABLE users ("
                        + "id INT PRIMARY KEY AUTO_INCREMENT, "
                        + "email VARCHAR(255) NOT NULL, "
                        + "name VARCHAR(100)"
                        + ");";
        Path tblFile = tempDir.resolve("users.tbl");
        Files.write(tblFile, tblContent.getBytes());

        TableFileParser parser = new TableFileParser();

        // Act
        com.aidvps.druid.differ.internal.model.Table table = parser.parseTableFile(tblFile);

        // Assert
        assertNotNull(table);
        assertEquals("users", table.getName());
        assertTrue(table.getColumns().size() >= 1);
    }

    @Test
    void testParseMultipleFiles() throws IOException, SchemaProviderException {
        // Arrange
        Files.write(tempDir.resolve("app.db"), "CREATE DATABASE app;".getBytes());
        Files.write(
                tempDir.resolve("users.tbl"),
                "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100));".getBytes());
        Files.write(
                tempDir.resolve("products.tbl"),
                "CREATE TABLE products (id INT PRIMARY KEY, title VARCHAR(255));".getBytes());

        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder().directoryPath(tempDir.toString()).build();

        // Act
        Schema schema = provider.getSchema(config);

        // Assert
        assertNotNull(schema);
        assertEquals(2, schema.getTableCount());
        assertTrue(schema.hasTable("users"));
        assertTrue(schema.hasTable("products"));
    }

    @Test
    void testDirectoryWithSubdirectory() throws IOException, SchemaProviderException {
        // Arrange - create a subdirectory (should be ignored)
        Path subdir = tempDir.resolve("ignored");
        Files.createDirectory(subdir);
        Files.write(subdir.resolve("test.tbl"), "CREATE TABLE test (id INT);".getBytes());

        // Create valid files in root
        Files.write(tempDir.resolve("app.db"), "CREATE DATABASE app;".getBytes());
        Files.write(tempDir.resolve("users.tbl"), "CREATE TABLE users (id INT);".getBytes());

        DirectorySchemaProviderConfig config =
                DirectorySchemaProviderConfig.builder().directoryPath(tempDir.toString()).build();

        // Act
        Schema schema = provider.getSchema(config);

        // Assert
        assertNotNull(schema);
        assertEquals(1, schema.getTableCount());
        assertTrue(schema.hasTable("users"));
    }

    @Test
    void testParseDatabaseNameWithQuotes() throws IOException, SchemaProviderException {
        // Arrange
        String dbContent = "CREATE DATABASE `my_database`;";
        Path dbFile = tempDir.resolve("my_database.db");
        Files.write(dbFile, dbContent.getBytes());

        DatabaseFileParser parser = new DatabaseFileParser();

        // Act
        String databaseName = parser.parseDatabaseName(dbFile);

        // Assert
        assertNotNull(databaseName);
        assertEquals("my_database", databaseName);
    }
}
