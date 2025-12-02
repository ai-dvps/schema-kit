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

import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import org.junit.jupiter.api.*;

/**
 * Contract tests for JarSchemaProvider.
 *
 * <p>Validates that JarSchemaProvider implements the SchemaProvider contract correctly.
 */
@DisplayName("JAR Schema Provider Contract")
class JarSchemaProviderContractTest {

    private JarSchemaProvider provider;
    private JarSchemaProviderConfig config;

    @BeforeEach
    void setUp() {
        provider = new JarSchemaProvider();
        config = JarSchemaProviderConfig.builder().jarPath("test.jar").build();
    }

    @Test
    @DisplayName("Should return correct provider type")
    void testGetType() {
        // Act
        ProviderType type = provider.getType();

        // Assert
        assertEquals(ProviderType.JAR, type);
    }

    @Test
    @DisplayName("Should reject null configuration")
    void testGetSchemaWithNullConfig() {
        // Act & Assert
        assertThrows(
                SchemaProviderException.class,
                () -> provider.getSchema(null),
                "Should reject null configuration");
    }

    @Test
    @DisplayName("Should reject non-JAR configuration")
    void testGetSchemaWithInvalidConfig() {
        // Arrange
        SchemaProviderConfig invalidConfig =
                new SchemaProviderConfig() {
                    @Override
                    public java.util.Map<String, Object> toMap() {
                        return new java.util.HashMap<>();
                    }

                    @Override
                    public void validate()
                            throws com.aidvps.schemakit.provider.ConfigValidationException {
                        // Invalid config
                    }
                };

        // Act & Assert
        SchemaProviderException exception =
                assertThrows(
                        SchemaProviderException.class,
                        () -> provider.getSchema(invalidConfig),
                        "Should reject non-JAR configuration");

        assertEquals(
                SchemaProviderException.ErrorCode.CONFIG_INVALID,
                exception.getErrorCode(),
                "Error code should be CONFIG_INVALID");
    }

    @Test
    @DisplayName("Should validate configuration correctly")
    void testValidateConfig() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    @DisplayName("Should reject null configuration in validateConfig")
    void testValidateConfigWithNull() {
        // Act & Assert
        SchemaProviderException exception =
                assertThrows(
                        SchemaProviderException.class,
                        () -> provider.validateConfig(null),
                        "Should reject null configuration");

        assertEquals(
                SchemaProviderException.ErrorCode.CONFIG_INVALID,
                exception.getErrorCode(),
                "Error code should be CONFIG_INVALID");
    }

    @Test
    @DisplayName("Should reject config without JAR path")
    void testValidateConfigWithoutJarPath() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> JarSchemaProviderConfig.builder().jarPath(null).build(),
                "Should reject config without JAR path");
    }

    @Test
    @DisplayName("Should reject config with empty JAR path")
    void testValidateConfigWithEmptyJarPath() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> JarSchemaProviderConfig.builder().jarPath("").build(),
                "Should reject config with empty JAR path");
    }

    @Test
    @DisplayName("Should accept valid JAR path")
    void testValidateConfigWithValidJarPath() {
        // Arrange
        JarSchemaProviderConfig validConfig =
                JarSchemaProviderConfig.builder().jarPath("/path/to/test.jar").build();

        // Act & Assert
        assertDoesNotThrow(() -> provider.validateConfig(validConfig));
    }
}
