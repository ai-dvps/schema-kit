/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with License.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SchemaProviderFactory. */
class SchemaProviderFactoryTest {

    private SchemaProvider mockProvider1;
    private SchemaProvider mockProvider2;

    @BeforeEach
    void setUp() {
        // Reinitialize factory to clear all registered providers
        // This is needed because the factory auto-discovers providers via SPI
        SchemaProviderFactory.reinitialize();

        mockProvider1 = mock(SchemaProvider.class);
        mockProvider2 = mock(SchemaProvider.class);

        // Set up default behavior for mocks
        when(mockProvider1.getProviderId()).thenReturn("mock-provider-1");
        when(mockProvider2.getProviderId()).thenReturn("mock-provider-2");
    }

    @Test
    void testRegisterAndCreateProvider() {
        // Arrange
        when(mockProvider1.getProviderId()).thenReturn("test-provider-1");

        // Act
        SchemaProviderFactory.registerProvider("test-provider-1", mockProvider1);
        SchemaProvider result = SchemaProviderFactory.createProvider("test-provider-1");

        // Assert
        assertNotNull(result);
        assertEquals(mockProvider1, result);
    }

    @Test
    void testCreateProviderThrowsForNullType() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.createProvider((String) null));

        assertEquals("Provider ID must not be null or empty", exception.getMessage());
    }

    @Test
    void testCreateProviderThrowsForUnregisteredType() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.createProvider("test-provider-unknown"));

        assertTrue(exception.getMessage().contains("Provider not found with ID"));
        assertTrue(exception.getMessage().contains("test-provider-unknown"));
    }

    @Test
    void testCreateProviderThrowsForUnregisteredTypeCustom() {
        // Arrange
        when(mockProvider1.getProviderId()).thenReturn("test-provider-custom");
        SchemaProviderFactory.registerProvider("test-provider-custom", mockProvider1);

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.createProvider("test-provider-unknown"));

        assertTrue(exception.getMessage().contains("Provider not found with ID"));
        assertTrue(exception.getMessage().contains("test-provider-unknown"));
    }

    @Test
    void testRegisterProviderThrowsForNullType() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.registerProvider((String) null, mockProvider1));

        assertEquals("Provider ID must not be null or empty", exception.getMessage());
    }

    @Test
    void testRegisterProviderThrowsForNullProvider() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.registerProvider("test-provider", null));

        assertEquals("Provider must not be null", exception.getMessage());
    }

    @Test
    void testRegisterProviderOverridesExistingProvider() {
        // Arrange
        SchemaProvider newProvider = mock(SchemaProvider.class);
        when(newProvider.getProviderId()).thenReturn("new-mock-provider");

        SchemaProviderFactory.registerProvider("test-provider-1", mockProvider1);
        SchemaProviderFactory.registerProvider("test-provider-2", newProvider);

        // Act
        SchemaProvider result1 = SchemaProviderFactory.createProvider("test-provider-1");
        SchemaProvider result2 = SchemaProviderFactory.createProvider("test-provider-2");

        // Assert
        assertEquals(mockProvider1, result1);
        assertEquals(newProvider, result2);
        assertNotEquals(result1, result2);
    }

    @Test
    void testIsProviderRegisteredReturnsTrueForRegistered() {
        // Arrange
        SchemaProviderFactory.registerProvider("test-provider", mockProvider1);

        // Act
        boolean result = SchemaProviderFactory.isProviderRegistered("test-provider");

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsProviderRegisteredReturnsFalseForUnregistered() {
        // Act
        boolean result = SchemaProviderFactory.isProviderRegistered("test-provider-unknown");

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsProviderRegisteredReturnsFalseForNull() {
        // Act - test with null provider ID
        boolean result = SchemaProviderFactory.isProviderRegistered((String) null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testMultipleProviderTypesCanBeRegistered() {
        // Arrange
        SchemaProvider directoryProvider = mock(SchemaProvider.class);
        SchemaProvider databaseProvider = mock(SchemaProvider.class);
        SchemaProvider gitProvider = mock(SchemaProvider.class);

        when(directoryProvider.getProviderId()).thenReturn("test-directory");
        when(databaseProvider.getProviderId()).thenReturn("test-database");
        when(gitProvider.getProviderId()).thenReturn("test-git");

        // Act
        SchemaProviderFactory.registerProvider("test-directory", directoryProvider);
        SchemaProviderFactory.registerProvider("test-database", databaseProvider);
        SchemaProviderFactory.registerProvider("test-git", gitProvider);

        // Assert
        assertEquals(directoryProvider, SchemaProviderFactory.createProvider("test-directory"));
        assertEquals(databaseProvider, SchemaProviderFactory.createProvider("test-database"));
        assertEquals(gitProvider, SchemaProviderFactory.createProvider("test-git"));
    }

    @Test
    void testFactoryCannotBeInstantiated() {
        // Act & Assert - In Java 8, private constructor throws IllegalAccessException
        assertThrows(
                IllegalAccessException.class,
                () -> {
                    SchemaProviderFactory.class.getDeclaredConstructor().newInstance();
                });
    }
}
