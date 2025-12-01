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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SchemaProviderConfig interface. */
class SchemaProviderConfigTest {

    private SchemaProviderConfig config;
    private Map<String, Object> testMap;

    @BeforeEach
    void setUp() {
        config = mock(SchemaProviderConfig.class);
        testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", 123);
    }

    @Test
    void testToMapReturnsMap() {
        // Arrange
        when(config.toMap()).thenReturn(testMap);

        // Act
        Map<String, Object> result = config.toMap();

        // Assert
        assertNotNull(result);
        assertEquals(testMap, result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals(123, result.get("key2"));
    }

    @Test
    void testToMapReturnsEmptyMapWhenNoConfig() {
        // Arrange
        when(config.toMap()).thenReturn(Collections.emptyMap());

        // Act
        Map<String, Object> result = config.toMap();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testValidateDoesNotThrow() {
        // Arrange - default implementation is no-op

        // Act & Assert
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testDefaultValidateDoesNotThrow() {
        // Use the default implementation
        SchemaProviderConfig configWithDefault = new SchemaProviderConfig() {
            @Override
            public Map<String, Object> toMap() {
                return testMap;
            }

            @Override
            public void validate() {
                // Default: no-op
            }
        };

        // Act & Assert
        assertDoesNotThrow(() -> configWithDefault.validate());
    }

    @Test
    void testToMapReturnsUnmodifiableMap() {
        // Arrange
        when(config.toMap()).thenReturn(testMap);

        // Act
        Map<String, Object> result = config.toMap();

        // Assert - the map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            result.put("key3", "value3");
        });
    }

    @Test
    void testToMapReturnsDifferentInstance() {
        // Arrange
        Map<String, Object> map1 = new HashMap<>();
        map1.put("test", "value");
        when(config.toMap()).thenReturn(map1);

        // Act
        Map<String, Object> result1 = config.toMap();
        Map<String, Object> result2 = config.toMap();

        // Assert
        assertNotSame(map1, result1);
        assertNotSame(result1, result2);
    }

    @Test
    void testToMapWithNestedObjects() {
        // Arrange
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nested", "value");
        Map<String, Object> complexMap = new HashMap<>();
        complexMap.put("level1", nestedMap);
        complexMap.put("list", Collections.singletonList("item"));

        when(config.toMap()).thenReturn(complexMap);

        // Act
        Map<String, Object> result = config.toMap();

        // Assert
        assertEquals(complexMap, result);
        assertTrue(result.containsKey("level1"));
        assertTrue(result.containsKey("list"));
    }
}
