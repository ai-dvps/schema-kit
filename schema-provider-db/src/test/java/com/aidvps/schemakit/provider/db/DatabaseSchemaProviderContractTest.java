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
import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

/**
 * Contract tests for DatabaseSchemaProvider.
 *
 * <p>Validates that DatabaseSchemaProvider implements the SchemaProvider contract correctly.
 */
@DisplayName("Database Schema Provider Contract")
class DatabaseSchemaProviderContractTest {

    private DatabaseSchemaProvider provider;
    private DatabaseSchemaProviderConfig config;

    @BeforeEach
    void setUp() throws SQLException {
        provider = new DatabaseSchemaProvider();
        config = Mockito.mock(DatabaseSchemaProviderConfig.class);
        Connection conn = Mockito.mock(Connection.class);
        Mockito.when(config.getConnection()).thenReturn(conn);
        Mockito.when(config.getPlatform()).thenReturn(DatabasePlatform.MYSQL);
    }

    @Test
    @DisplayName("Should return correct provider type")
    void testGetType() {
        // Act
        ProviderType type = provider.getType();

        // Assert
        assertEquals(ProviderType.DATABASE, type);
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
    @DisplayName("Should reject non-database configuration")
    void testGetSchemaWithInvalidConfig() {
        // Arrange
        SchemaProviderConfig invalidConfig = Mockito.mock(SchemaProviderConfig.class);

        // Act & Assert
        SchemaProviderException exception =
                assertThrows(
                        SchemaProviderException.class,
                        () -> provider.getSchema(invalidConfig),
                        "Should reject non-database configuration");

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
    @DisplayName("Should reject config without connection")
    void testValidateConfigWithoutConnection() throws SQLException {
        // Arrange
        DatabaseSchemaProviderConfig invalidConfig =
                Mockito.mock(DatabaseSchemaProviderConfig.class);
        Connection conn = Mockito.mock(Connection.class);
        Mockito.when(invalidConfig.getConnection()).thenReturn(null);
        Mockito.when(invalidConfig.getPlatform()).thenReturn(DatabasePlatform.MYSQL);

        // Act & Assert
        SchemaProviderException exception =
                assertThrows(
                        SchemaProviderException.class,
                        () -> provider.validateConfig(invalidConfig),
                        "Should reject config without connection");

        assertEquals(
                SchemaProviderException.ErrorCode.CONFIG_INVALID,
                exception.getErrorCode(),
                "Error code should be CONFIG_INVALID");
    }

    @Test
    @DisplayName("Should reject config without platform")
    void testValidateConfigWithoutPlatform() throws SQLException {
        // Arrange
        DatabaseSchemaProviderConfig invalidConfig =
                Mockito.mock(DatabaseSchemaProviderConfig.class);
        Connection conn = Mockito.mock(Connection.class);
        Mockito.when(invalidConfig.getConnection()).thenReturn(conn);
        Mockito.when(invalidConfig.getPlatform()).thenReturn(null);

        // Act & Assert
        SchemaProviderException exception =
                assertThrows(
                        SchemaProviderException.class,
                        () -> provider.validateConfig(invalidConfig),
                        "Should reject config without platform");

        assertEquals(
                SchemaProviderException.ErrorCode.CONFIG_INVALID,
                exception.getErrorCode(),
                "Error code should be CONFIG_INVALID");
    }
}
