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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for EnvironmentVariableSecretProvider. */
class EnvironmentVariableSecretProviderTest {

    private EnvironmentVariableSecretProvider provider;
    private Map<String, String> originalEnvVars;

    @BeforeEach
    void setUp() {
        // Save original environment variables
        originalEnvVars = new HashMap<>(System.getenv());

        provider = new EnvironmentVariableSecretProvider();
    }

    @AfterEach
    void tearDown() {
        // Restore original environment variables
        if (originalEnvVars != null) {
            // Note: In Java 8, we cannot fully clear env vars, so we just restore
            // This is a limitation of Java 8's System.getenv()
            for (Map.Entry<String, String> entry : originalEnvVars.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    @Test
    void testGetSecretReturnsValueForExistingKey() throws SecretNotFoundException {
        // Arrange
        setEnvVar("TEST_SECRET", "test_value");

        // Act
        String result = provider.getSecret("TEST_SECRET");

        // Assert
        assertEquals("test_value", result);
    }

    @Test
    void testGetSecretThrowsExceptionForNonExistentKey() {
        // Act & Assert
        SecretNotFoundException exception = assertThrows(
                SecretNotFoundException.class,
                () -> provider.getSecret("NON_EXISTENT_KEY"));

        assertEquals("NON_EXISTENT_KEY", exception.getKey());
    }

    @Test
    void testGetSecretThrowsExceptionForNullKey() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> provider.getSecret(null));
    }

    @Test
    void testGetSecretThrowsExceptionForEmptyKey() {
        // Act & Assert
        SecretNotFoundException exception = assertThrows(
                SecretNotFoundException.class,
                () -> provider.getSecret(""));

        assertEquals("", exception.getKey());
    }

    @Test
    void testHasSecretReturnsTrueForExistingKey() {
        // Arrange
        setEnvVar("TEST_SECRET", "test_value");

        // Act
        boolean result = provider.hasSecret("TEST_SECRET");

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasSecretReturnsFalseForNonExistentKey() {
        // Act
        boolean result = provider.hasSecret("NON_EXISTENT_KEY");

        // Assert
        assertFalse(result);
    }

    @Test
    void testHasSecretReturnsFalseForNullKey() {
        // Act
        boolean result = provider.hasSecret(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void testHasSecretReturnsFalseForEmptyKey() {
        // Act
        boolean result = provider.hasSecret("");

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetSecretReturnsMultipleValuesCorrectly() throws SecretNotFoundException {
        // Arrange
        setEnvVar("SECRET_1", "value1");
        setEnvVar("SECRET_2", "value2");
        setEnvVar("SECRET_3", "value3");

        // Act
        String result1 = provider.getSecret("SECRET_1");
        String result2 = provider.getSecret("SECRET_2");
        String result3 = provider.getSecret("SECRET_3");

        // Assert
        assertEquals("value1", result1);
        assertEquals("value2", result2);
        assertEquals("value3", result3);
    }

    @Test
    void testHasSecretForMultipleKeys() {
        // Arrange
        setEnvVar("SECRET_1", "value1");
        setEnvVar("SECRET_2", "value2");

        // Act & Assert
        assertTrue(provider.hasSecret("SECRET_1"));
        assertTrue(provider.hasSecret("SECRET_2"));
        assertFalse(provider.hasSecret("SECRET_3"));
    }

    @Test
    void testGetSecretWithSpecialCharacters() throws SecretNotFoundException {
        // Arrange
        setEnvVar("SPECIAL_SECRET", "value!@#$%^&*()_+={}[]|\\:;\"'<>,.?/");

        // Act
        String result = provider.getSecret("SPECIAL_SECRET");

        // Assert
        assertEquals("value!@#$%^&*()_+={}[]|\\:;\"'<>,.?/", result);
    }

    @Test
    void testGetSecretWithUnicode() throws SecretNotFoundException {
        // Arrange
        setEnvVar("UNICODE_SECRET", "unicode_value_测试");

        // Act
        String result = provider.getSecret("UNICODE_SECRET");

        // Assert
        assertEquals("unicode_value_测试", result);
    }

    @Test
    void testGetSecretWithEmptyValue() throws SecretNotFoundException {
        // Arrange
        setEnvVar("EMPTY_SECRET", "");

        // Act
        String result = provider.getSecret("EMPTY_SECRET");

        // Assert
        assertEquals("", result);
    }

    @Test
    void testHasSecretWithEmptyValue() {
        // Arrange
        setEnvVar("EMPTY_SECRET", "");

        // Act
        boolean result = provider.hasSecret("EMPTY_SECRET");

        // Assert
        assertTrue(result);
    }

    @Test
    void testCaseSensitiveKeys() {
        // Arrange
        setEnvVar("Secret_Key", "value");

        // Act & Assert
        assertTrue(provider.hasSecret("Secret_Key"));
        assertFalse(provider.hasSecret("secret_key"));
        assertFalse(provider.hasSecret("SECRET_KEY"));
    }

    @Test
    void testConstructorDoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> new EnvironmentVariableSecretProvider());
    }

    private void setEnvVar(String key, String value) {
        System.setProperty(key, value);
    }
}
