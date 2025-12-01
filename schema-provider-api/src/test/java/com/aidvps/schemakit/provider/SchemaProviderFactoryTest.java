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
        // Clear any previously registered providers
        try {
            java.lang.reflect.Field field =
                    SchemaProviderFactory.class.getDeclaredField("providers");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<ProviderType, SchemaProvider> providers =
                    (java.util.Map<ProviderType, SchemaProvider>) field.get(null);
            providers.clear();
        } catch (Exception e) {
            // Ignore if reflection fails
        }

        mockProvider1 = mock(SchemaProvider.class);
        mockProvider2 = mock(SchemaProvider.class);
    }

    @Test
    void testRegisterAndCreateProvider() {
        // Arrange
        when(mockProvider1.getType()).thenReturn(ProviderType.DIRECTORY);

        // Act
        SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, mockProvider1);
        SchemaProvider result = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);

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
                        () -> SchemaProviderFactory.createProvider(null));

        assertEquals("Provider type must not be null", exception.getMessage());
    }

    @Test
    void testCreateProviderThrowsForUnregisteredType() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.createProvider(ProviderType.DIRECTORY));

        assertTrue(exception.getMessage().contains("Provider type not supported"));
        assertTrue(exception.getMessage().contains("DIRECTORY"));
    }

    @Test
    void testCreateProviderThrowsForUnregisteredTypeCustom() {
        // Arrange
        SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, mockProvider1);

        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.createProvider(ProviderType.CUSTOM));

        assertTrue(exception.getMessage().contains("Provider type not supported"));
        assertTrue(exception.getMessage().contains("CUSTOM"));
    }

    @Test
    void testRegisterProviderThrowsForNullType() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.registerProvider(null, mockProvider1));

        assertEquals("Provider type must not be null", exception.getMessage());
    }

    @Test
    void testRegisterProviderThrowsForNullProvider() {
        // Act & Assert
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, null));

        assertEquals("Provider must not be null", exception.getMessage());
    }

    @Test
    void testRegisterProviderOverridesExistingProvider() {
        // Arrange
        SchemaProvider newProvider = mock(SchemaProvider.class);
        SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, mockProvider1);
        SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, newProvider);

        // Act
        SchemaProvider result = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);

        // Assert
        assertEquals(newProvider, result);
        assertNotEquals(mockProvider1, result);
    }

    @Test
    void testIsProviderRegisteredReturnsTrueForRegistered() {
        // Arrange
        SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, mockProvider1);

        // Act
        boolean result = SchemaProviderFactory.isProviderRegistered(ProviderType.DIRECTORY);

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsProviderRegisteredReturnsFalseForUnregistered() {
        // Act
        boolean result = SchemaProviderFactory.isProviderRegistered(ProviderType.DIRECTORY);

        // Assert
        assertFalse(result);
    }

    @Test
    void testIsProviderRegisteredReturnsFalseForNull() {
        // Act
        boolean result = SchemaProviderFactory.isProviderRegistered(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testMultipleProviderTypesCanBeRegistered() {
        // Arrange
        SchemaProvider directoryProvider = mock(SchemaProvider.class);
        SchemaProvider databaseProvider = mock(SchemaProvider.class);
        SchemaProvider gitProvider = mock(SchemaProvider.class);

        when(directoryProvider.getType()).thenReturn(ProviderType.DIRECTORY);
        when(databaseProvider.getType()).thenReturn(ProviderType.DATABASE);
        when(gitProvider.getType()).thenReturn(ProviderType.GIT);

        // Act
        SchemaProviderFactory.registerProvider(ProviderType.DIRECTORY, directoryProvider);
        SchemaProviderFactory.registerProvider(ProviderType.DATABASE, databaseProvider);
        SchemaProviderFactory.registerProvider(ProviderType.GIT, gitProvider);

        // Assert
        assertEquals(
                directoryProvider, SchemaProviderFactory.createProvider(ProviderType.DIRECTORY));
        assertEquals(databaseProvider, SchemaProviderFactory.createProvider(ProviderType.DATABASE));
        assertEquals(gitProvider, SchemaProviderFactory.createProvider(ProviderType.GIT));
    }

    @Test
    void testFactoryCannotBeInstantiated() {
        // Act & Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    SchemaProviderFactory.class.getDeclaredConstructor().newInstance();
                });
    }
}
