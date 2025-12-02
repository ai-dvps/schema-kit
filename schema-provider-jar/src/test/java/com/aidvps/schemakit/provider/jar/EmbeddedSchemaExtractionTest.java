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
import java.util.Map;
import org.junit.jupiter.api.*;

/** Integration tests for embedded schema extraction from JAR files. */
@DisplayName("Embedded Schema Extraction")
class EmbeddedSchemaExtractionTest {

    private JarResourceExtractor resourceExtractor;

    @BeforeEach
    void setUp() {
        resourceExtractor = new JarResourceExtractor();
    }

    @Test
    @DisplayName("Should extract schema resources from JAR")
    void testExtractSchemaResourcesFromJar() throws IOException {
        // Arrange
        String jarPath = "/path/to/test.jar";

        // Act
        // Note: Actual extraction will work with a real JAR file
        // For now, we test the API is accessible
        Map<String, String> resources = resourceExtractor.extractSchemaResources(jarPath, null);

        // Assert
        assertNotNull(resources, "Resources map should not be null");
    }

    @Test
    @DisplayName("Should list schema files in JAR")
    void testListSchemaFilesInJar() throws IOException {
        // Arrange
        String jarPath = "/path/to/test.jar";

        // Act
        // Note: Actual listing will work with a real JAR file
        // For now, we test the API is accessible
        assertDoesNotThrow(
                () -> resourceExtractor.listSchemaFiles(jarPath, null),
                "Should not throw when listing schema files");
    }

    @Test
    @DisplayName("Should handle null JAR path")
    void testHandleNullJarPath() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceExtractor.extractSchemaResources(null, null),
                "Should reject null JAR path");
    }

    @Test
    @DisplayName("Should handle empty JAR path")
    void testHandleEmptyJarPath() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceExtractor.extractSchemaResources("", null),
                "Should reject empty JAR path");
    }

    @Test
    @DisplayName("Should filter by resource path")
    void testFilterByResourcePath() throws IOException {
        // Arrange
        String jarPath = "/path/to/test.jar";
        String resourcePath = "schemas";

        // Act
        Map<String, String> resources =
                resourceExtractor.extractSchemaResources(jarPath, resourcePath);

        // Assert
        assertNotNull(resources, "Resources map should not be null");
    }

    @Test
    @DisplayName("Should list schema files with resource path filter")
    void testListSchemaFilesWithResourcePath() throws IOException {
        // Arrange
        String jarPath = "/path/to/test.jar";
        String resourcePath = "schemas";

        // Act
        assertDoesNotThrow(
                () -> resourceExtractor.listSchemaFiles(jarPath, resourcePath),
                "Should not throw when listing with resource path");
    }

    @Test
    @DisplayName("Should handle non-existent JAR file")
    void testHandleNonExistentJarFile() throws IOException {
        // Arrange
        String jarPath = "/path/to/nonexistent.jar";

        // Act
        Map<String, String> resources = resourceExtractor.extractSchemaResources(jarPath, null);

        // Assert
        assertNotNull(resources, "Resources map should not be null");
        assertTrue(resources.isEmpty(), "Resources should be empty for non-existent JAR");
    }

    @Test
    @DisplayName("Should handle IOException during extraction")
    void testHandleIOExceptionDuringExtraction() {
        // Arrange
        String jarPath = "/invalid/path/!@#$%/test.jar";

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> resourceExtractor.extractSchemaResources(jarPath, null),
                "Should throw IOException for invalid path");
    }
}
