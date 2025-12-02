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

import org.junit.jupiter.api.Test;

/** Unit tests for ProviderType enum. */
class ProviderTypeTest {

    @Test
    void testDirectoryTypeHasCorrectDescription() {
        // Act
        String description = ProviderType.DIRECTORY.getDescription();

        // Assert
        assertEquals("Directory-based file provider", description);
    }

    @Test
    void testDatabaseTypeHasCorrectDescription() {
        // Act
        String description = ProviderType.DATABASE.getDescription();

        // Assert
        assertEquals("Live database connection provider", description);
    }

    @Test
    void testGitTypeHasCorrectDescription() {
        // Act
        String description = ProviderType.GIT.getDescription();

        // Assert
        assertEquals("Git repository provider", description);
    }

    @Test
    void testJarTypeHasCorrectDescription() {
        // Act
        String description = ProviderType.JAR.getDescription();

        // Assert
        assertEquals("JAR-embedded file provider", description);
    }

    @Test
    void testCustomTypeHasCorrectDescription() {
        // Act
        String description = ProviderType.CUSTOM.getDescription();

        // Assert
        assertEquals("Custom provider", description);
    }

    @Test
    void testAllTypesHaveDescriptions() {
        // Act & Assert
        assertAll(
                "All ProviderTypes should have descriptions",
                () -> assertNotNull(ProviderType.DIRECTORY.getDescription()),
                () -> assertNotNull(ProviderType.DATABASE.getDescription()),
                () -> assertNotNull(ProviderType.GIT.getDescription()),
                () -> assertNotNull(ProviderType.JAR.getDescription()),
                () -> assertNotNull(ProviderType.CUSTOM.getDescription()));
    }

    @Test
    void testAllDescriptionsAreNonEmpty() {
        // Act & Assert
        assertAll(
                "All descriptions should be non-empty",
                () -> assertFalse(ProviderType.DIRECTORY.getDescription().isEmpty()),
                () -> assertFalse(ProviderType.DATABASE.getDescription().isEmpty()),
                () -> assertFalse(ProviderType.GIT.getDescription().isEmpty()),
                () -> assertFalse(ProviderType.JAR.getDescription().isEmpty()),
                () -> assertFalse(ProviderType.CUSTOM.getDescription().isEmpty()));
    }

    @Test
    void testEnumValues() {
        // Arrange
        ProviderType[] types = ProviderType.values();

        // Act & Assert
        assertEquals(5, types.length);
        assertContains(types, ProviderType.DIRECTORY);
        assertContains(types, ProviderType.DATABASE);
        assertContains(types, ProviderType.GIT);
        assertContains(types, ProviderType.JAR);
        assertContains(types, ProviderType.CUSTOM);
    }

    @Test
    void testValueOfReturnsCorrectEnum() {
        // Act
        ProviderType directory = ProviderType.valueOf("DIRECTORY");
        ProviderType database = ProviderType.valueOf("DATABASE");
        ProviderType git = ProviderType.valueOf("GIT");
        ProviderType jar = ProviderType.valueOf("JAR");
        ProviderType custom = ProviderType.valueOf("CUSTOM");

        // Assert
        assertEquals(ProviderType.DIRECTORY, directory);
        assertEquals(ProviderType.DATABASE, database);
        assertEquals(ProviderType.GIT, git);
        assertEquals(ProviderType.JAR, jar);
        assertEquals(ProviderType.CUSTOM, custom);
    }

    @Test
    void testValueOfThrowsForInvalidValue() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    ProviderType.valueOf("INVALID_TYPE");
                });
    }

    @Test
    void testGetDescriptionReturnsDescription() {
        // Act & Assert
        assertEquals("Directory-based file provider", ProviderType.DIRECTORY.getDescription());
        assertEquals("Live database connection provider", ProviderType.DATABASE.getDescription());
        assertEquals("Git repository provider", ProviderType.GIT.getDescription());
        assertEquals("JAR-embedded file provider", ProviderType.JAR.getDescription());
        assertEquals("Custom provider", ProviderType.CUSTOM.getDescription());
    }

    private void assertContains(ProviderType[] types, ProviderType expected) {
        boolean found = false;
        for (ProviderType type : types) {
            if (type == expected) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find " + expected + " in enum values");
    }
}
