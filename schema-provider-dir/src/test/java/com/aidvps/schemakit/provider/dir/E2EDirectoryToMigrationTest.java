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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// End-to-end test for directory → schema workflow.
class E2EDirectoryToMigrationTest {

    @TempDir Path tempDir;

    private DirectorySchemaProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DirectorySchemaProvider();
    }

    @Test
    void testDirectoryToSchema() throws Exception {
        // Arrange - Create source schema directory
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        createTestDatabaseFile(sourceDir, "mydb.db", "CREATE DATABASE mydb;");
        createTestTableFile(
                sourceDir,
                "users.tbl",
                "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255));");

        // Arrange - Create target schema directory
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        createTestDatabaseFile(targetDir, "mydb.db", "CREATE DATABASE mydb;");
        createTestTableFile(
                targetDir,
                "users.tbl",
                "CREATE TABLE users (id INT PRIMARY KEY, email VARCHAR(255));");
        createTestTableFile(
                targetDir,
                "products.tbl",
                "CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(255));");

        // Act - Load schemas from directories
        DirectorySchemaProviderConfig sourceConfig =
                DirectorySchemaProviderConfig.builder().directoryPath(sourceDir.toString()).build();
        Schema sourceSchema = provider.getSchema(sourceConfig);

        DirectorySchemaProviderConfig targetConfig =
                DirectorySchemaProviderConfig.builder().directoryPath(targetDir.toString()).build();
        Schema targetSchema = provider.getSchema(targetConfig);

        // Assert - Verify schemas were loaded
        assertNotNull(sourceSchema);
        assertNotNull(targetSchema);

        // Assert - Verify source has users table
        assertTrue(sourceSchema.hasTable("users"), "Source schema should have users table");
        assertEquals(1, sourceSchema.getTableCount(), "Source schema should have 1 table");

        // Assert - Verify target has users and products tables
        assertTrue(targetSchema.hasTable("users"), "Target schema should have users table");
        assertTrue(targetSchema.hasTable("products"), "Target schema should have products table");
        assertEquals(2, targetSchema.getTableCount(), "Target schema should have 2 tables");
    }

    @Test
    void testSchemaComparison() throws Exception {
        // Arrange - Create source schema with one table
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        createTestDatabaseFile(sourceDir, "test.db", "CREATE DATABASE test;");
        createTestTableFile(sourceDir, "users.tbl", "CREATE TABLE users (id INT PRIMARY KEY);");

        // Arrange - Create target schema with additional table
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        createTestDatabaseFile(targetDir, "test.db", "CREATE DATABASE test;");
        createTestTableFile(targetDir, "users.tbl", "CREATE TABLE users (id INT PRIMARY KEY);");
        createTestTableFile(
                targetDir,
                "products.tbl",
                "CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(255));");

        // Act - Load both schemas
        DirectorySchemaProviderConfig sourceConfig =
                DirectorySchemaProviderConfig.builder().directoryPath(sourceDir.toString()).build();
        Schema sourceSchema = provider.getSchema(sourceConfig);

        DirectorySchemaProviderConfig targetConfig =
                DirectorySchemaProviderConfig.builder().directoryPath(targetDir.toString()).build();
        Schema targetSchema = provider.getSchema(targetConfig);

        // Assert - Verify schemas differ
        assertNotEquals(sourceSchema, targetSchema, "Schemas should be different");
        assertEquals(1, sourceSchema.getTableCount(), "Source should have 1 table");
        assertEquals(2, targetSchema.getTableCount(), "Target should have 2 tables");
        assertFalse(
                targetSchema.hasTable("products") && sourceSchema.hasTable("products"),
                "Products table should only exist in target");
    }

    private void createTestDatabaseFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.write(file, content.getBytes());
    }

    private void createTestDatabaseFile(Path dir, String fileName, String content)
            throws IOException {
        Path file = dir.resolve(fileName);
        Files.write(file, content.getBytes());
    }

    private void createTestTableFile(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.write(file, content.getBytes());
    }

    private void createTestTableFile(Path dir, String fileName, String content) throws IOException {
        Path file = dir.resolve(fileName);
        Files.write(file, content.getBytes());
    }
}
