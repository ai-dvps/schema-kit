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

package com.aidvps.schemakit.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aidvps.druid.differ.internal.model.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for SchemaProvider interface. */
class SchemaProviderTest {

    private SchemaProvider provider;
    private SchemaProviderConfig config;
    private Schema schema;

    @BeforeEach
    void setUp() {
        provider = Mockito.mock(SchemaProvider.class);
        config = Mockito.mock(SchemaProviderConfig.class);
        schema = Mockito.mock(Schema.class);
    }

    @Test
    void testGetSchemaReturnsSchema() throws SchemaProviderException {
        // Arrange
        when(provider.getSchema(config)).thenReturn(schema);

        // Act
        Schema result = provider.getSchema(config);

        // Assert
        assertNotNull(result);
        assertEquals(schema, result);
        verify(provider).getSchema(config);
    }

    @Test
    void testGetSchemaThrowsExceptionOnError() throws SchemaProviderException {
        // Arrange
        when(provider.getSchema(config)).thenThrow(new SchemaProviderException(
                SchemaProviderException.ErrorCode.SOURCE_NOT_FOUND, "Test error"));

        // Act & Assert
        SchemaProviderException exception = assertThrows(
                SchemaProviderException.class,
                () -> provider.getSchema(config));

        assertEquals(SchemaProviderException.ErrorCode.SOURCE_NOT_FOUND,
                exception.getErrorCode());
        assertEquals("Test error", exception.getMessage());
    }

    @Test
    void testGetTypeReturnsProviderType() {
        // Arrange
        when(provider.getType()).thenReturn(ProviderType.DIRECTORY);

        // Act
        ProviderType result = provider.getType();

        // Assert
        assertNotNull(result);
        assertEquals(ProviderType.DIRECTORY, result);
        verify(provider).getType();
    }

    @Test
    void testValidateConfigThrowsExceptionForNullConfig() {
        // Act & Assert
        assertThrows(SchemaProviderException.class,
                () -> provider.validateConfig(null));
    }

    @Test
    void testValidateConfigDoesNotThrowForValidConfig() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    void testDefaultValidateConfigDoesNotThrow() throws SchemaProviderException {
        // Use the default implementation
        SchemaProvider providerWithDefault = new SchemaProvider() {
            @Override
            public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
                return null;
            }

            @Override
            public ProviderType getType() {
                return null;
            }
        };

        // Act & Assert
        assertDoesNotThrow(() -> providerWithDefault.validateConfig(config));
    }

    @Test
    void testDefaultValidateConfigThrowsForNullConfig() {
        // Use the default implementation
        SchemaProvider providerWithDefault = new SchemaProvider() {
            @Override
            public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
                return null;
            }

            @Override
            public ProviderType getType() {
                return null;
            }
        };

        // Act & Assert
        assertThrows(SchemaProviderException.class,
                () -> providerWithDefault.validateConfig(null));
    }
}
