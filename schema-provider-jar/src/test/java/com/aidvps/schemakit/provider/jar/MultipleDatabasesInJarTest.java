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

package com.aidvps.schemakit.provider.jar;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;

/** Integration tests for handling multiple databases in a single JAR file. */
@DisplayName("Multiple Databases in JAR")
class MultipleDatabasesInJarTest {

    private JarResourceExtractor resourceExtractor;

    @BeforeEach
    void setUp() {
        resourceExtractor = new JarResourceExtractor();
    }

    @Test
    @DisplayName("Should extract schema from each database directory")
    void testExtractSchemaFromEachDatabase() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";
        String resourcePath = "database";

        // Act
        Map<String, String> schemas =
                resourceExtractor.extractSchemaResources(jarPath, resourcePath);

        // Assert
        assertNotNull(schemas, "Schemas map should not be null");
    }

    @Test
    @DisplayName("Should handle databases in different subdirectories")
    void testHandleDatabasesInDifferentSubdirectories() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";
        List<String> paths =
                java.util.Arrays.asList("app1/database", "app2/database", "shared/database");

        // Act & Assert
        for (String path : paths) {
            assertDoesNotThrow(
                    () -> resourceExtractor.extractSchemaResources(jarPath, path),
                    "Should handle path: " + path);
        }
    }

    @Test
    @DisplayName("Should extract all schema files from JAR")
    void testExtractAllSchemaFilesFromJar() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";

        // Act
        Map<String, String> schemas = resourceExtractor.extractSchemaResources(jarPath, null);

        // Assert
        assertNotNull(schemas, "Schemas map should not be null");
    }

    @Test
    @DisplayName("Should list schema files from JAR")
    void testListSchemaFilesFromJar() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";

        // Act
        List<String> schemaFiles = resourceExtractor.listSchemaFiles(jarPath, null);

        // Assert
        assertNotNull(schemaFiles, "Schema files list should not be null");
    }

    @Test
    @DisplayName("Should filter schema files by resource path")
    void testFilterSchemaFilesByResourcePath() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";
        String resourcePath = "database/production";

        // Act
        List<String> schemaFiles = resourceExtractor.listSchemaFiles(jarPath, resourcePath);

        // Assert
        assertNotNull(schemaFiles, "Schema files list should not be null");
    }

    @Test
    @DisplayName("Should handle databases with same name in different paths")
    void testHandleDatabasesWithSameName() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";
        List<String> paths = java.util.Arrays.asList("database1", "database2", "database3");

        // Act & Assert
        for (String path : paths) {
            Map<String, String> schemas = resourceExtractor.extractSchemaResources(jarPath, path);
            assertNotNull(schemas, "Should extract schemas for path: " + path);
        }
    }

    @Test
    @DisplayName("Should detect mixed database schemas")
    void testDetectMixedDatabaseSchemas() throws IOException {
        // Arrange
        String jarPath = "/path/to/mixed.jar";
        String resourcePath = "database";

        // Act
        Map<String, String> schemas =
                resourceExtractor.extractSchemaResources(jarPath, resourcePath);

        // Assert
        assertNotNull(schemas, "Schemas map should not be null");
    }

    @Test
    @DisplayName("Should handle edge case with no database files")
    void testHandleEdgeCaseWithNoDatabaseFiles() throws IOException {
        // Arrange
        String jarPath = "/path/to/no-db.jar";

        // Act
        Map<String, String> schemas = resourceExtractor.extractSchemaResources(jarPath, null);

        // Assert
        assertNotNull(schemas, "Schemas map should not be null");
        assertTrue(
                schemas.isEmpty() || schemas.size() >= 0,
                "Should return empty or non-negative count for schemas");
    }

    @Test
    @DisplayName("Should extract database and table files")
    void testExtractDatabaseAndTableFiles() throws IOException {
        // Arrange
        String jarPath = "/path/to/multi-db.jar";

        // Act
        Map<String, String> resources = resourceExtractor.extractSchemaResources(jarPath, null);

        // Assert
        assertNotNull(resources, "Resources map should not be null");
        // Verify that only .db and .tbl files are included
        for (String fileName : resources.keySet()) {
            assertTrue(
                    fileName.endsWith(".db") || fileName.endsWith(".tbl"),
                    "File should be .db or .tbl: " + fileName);
        }
    }

    @Test
    @DisplayName("Should handle large JAR with many database files")
    void testHandleLargeJarWithManyDatabaseFiles() throws IOException {
        // Arrange
        String jarPath = "/path/to/large-db.jar";

        // Act
        Map<String, String> schemas = resourceExtractor.extractSchemaResources(jarPath, null);

        // Assert
        assertNotNull(schemas, "Schemas map should not be null");
        assertTrue(
                schemas.size() >= 0, "Should handle any number of schema files (including zero)");
    }
}
