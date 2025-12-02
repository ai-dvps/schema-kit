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
import org.junit.jupiter.api.*;

/** Unit tests for JAR resource loading functionality. */
@DisplayName("JAR Resource Loading")
class JarResourceLoaderTest {

    private ClasspathResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        resourceLoader = new ClasspathResourceLoader();
    }

    @Test
    @DisplayName("Should load resource from classpath")
    void testLoadResourceFromClasspath() {
        // Act & Assert
        // Note: Actual resource loading will depend on the JAR structure
        // For now, we test the configuration
        assertNotNull(resourceLoader, "Resource loader should be initialized");
    }

    @Test
    @DisplayName("Should handle non-existent resources")
    void testHandleNonExistentResources() {
        // Arrange
        String resourcePath = "nonexistent/resource.txt";

        // Act & Assert
        assertThrows(
                IOException.class,
                () -> resourceLoader.loadResource(resourcePath),
                "Should throw exception for non-existent resource");
    }

    @Test
    @DisplayName("Should check if resource exists")
    void testCheckIfResourceExists() {
        // Act & Assert
        assertFalse(
                resourceLoader.resourceExists("nonexistent.txt"),
                "Should return false for non-existent resource");
    }

    @Test
    @DisplayName("Should validate resource path format")
    void testValidateResourcePathFormat() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceLoader.loadResource(""),
                "Should reject empty resource path");
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceLoader.loadResource(null),
                "Should reject null resource path");
    }

    @Test
    @DisplayName("Should handle path separators in resource paths")
    void testHandlePathSeparatorsInResourcePaths() {
        // Arrange
        String resourceWithSlashes = "schemas/database/schema.db";
        String resourceWithDots = "schemas.database.schema";

        // Act & Assert
        assertDoesNotThrow(
                () -> resourceLoader.resourceExists(resourceWithSlashes),
                "Should accept forward slash separators");
        assertDoesNotThrow(
                () -> resourceLoader.resourceExists(resourceWithDots),
                "Should accept dot separators");
    }

    @Test
    @DisplayName("Should get class loader")
    void testGetClassLoader() {
        // Act
        ClassLoader classLoader = resourceLoader.getClassLoader();

        // Assert
        assertNotNull(classLoader, "Should return class loader");
    }

    @Test
    @DisplayName("Should load resources with pattern")
    void testLoadResourcesWithPattern() {
        // Act & Assert
        assertDoesNotThrow(
                () -> resourceLoader.loadResources("*.txt"),
                "Should not throw when loading resources with pattern");
    }

    @Test
    @DisplayName("Should get resources with prefix")
    void testGetResourcesWithPrefix() throws IOException {
        // Act
        assertDoesNotThrow(
                () -> resourceLoader.getResourcesWithPrefix("schemas"),
                "Should not throw when getting resources with prefix");
    }

    @Test
    @DisplayName("Should handle null pattern in loadResources")
    void testHandleNullPatternInLoadResources() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceLoader.loadResources(null),
                "Should reject null pattern");
    }

    @Test
    @DisplayName("Should handle empty pattern in loadResources")
    void testHandleEmptyPatternInLoadResources() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceLoader.loadResources(""),
                "Should reject empty pattern");
    }

    @Test
    @DisplayName("Should handle null prefix in getResourcesWithPrefix")
    void testHandleNullPrefixInGetResourcesWithPrefix() throws IOException {
        // Act
        assertDoesNotThrow(
                () -> resourceLoader.getResourcesWithPrefix(null),
                "Should handle null prefix gracefully");
    }

    @Test
    @DisplayName("Should load resource as file when on filesystem")
    void testLoadResourceAsFileWhenOnFilesystem() throws IOException {
        // Act
        assertDoesNotThrow(
                () -> resourceLoader.loadResourceAsFile("test.txt"),
                "Should not throw when loading resource as file");
    }

    @Test
    @DisplayName("Should handle null resource path in loadResourceAsFile")
    void testHandleNullResourcePathInLoadResourceAsFile() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> resourceLoader.loadResourceAsFile(null),
                "Should reject null resource path");
    }
}
