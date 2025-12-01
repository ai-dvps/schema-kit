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

package com.aidvps.schemakit.migrator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aidvps.druid.differ.internal.model.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for SchemaMigrator interface. */
class SchemaMigratorTest {

    private SchemaMigrator migrator;
    private Schema sourceSchema;
    private Schema targetSchema;
    private MigrationConfig config;
    private MigrationScript script;
    private SchemaDiff schemaDiff;

    @BeforeEach
    void setUp() {
        migrator = Mockito.mock(SchemaMigrator.class);
        sourceSchema = Mockito.mock(Schema.class);
        targetSchema = Mockito.mock(Schema.class);
        config = Mockito.mock(MigrationConfig.class);
        script = Mockito.mock(MigrationScript.class);
        schemaDiff = Mockito.mock(SchemaDiff.class);
    }

    @Test
    void testGenerateMigrationReturnsMigrationScript() throws MigrationException {
        // Arrange
        when(migrator.generateMigration(sourceSchema, targetSchema, config)).thenReturn(script);

        // Act
        MigrationScript result = migrator.generateMigration(sourceSchema, targetSchema, config);

        // Assert
        assertNotNull(result);
        assertEquals(script, result);
        verify(migrator).generateMigration(sourceSchema, targetSchema, config);
    }

    @Test
    void testGenerateMigrationThrowsExceptionOnError() throws MigrationException {
        // Arrange
        when(migrator.generateMigration(sourceSchema, targetSchema, config))
                .thenThrow(
                        new MigrationException(
                                MigrationException.ErrorCode.GENERATION_ERROR, "Test error"));

        // Act & Assert
        MigrationException exception =
                assertThrows(
                        MigrationException.class,
                        () -> migrator.generateMigration(sourceSchema, targetSchema, config));

        assertEquals("Test error", exception.getMessage());
    }

    @Test
    void testCompareReturnsSchemaDiff() throws MigrationException {
        // Arrange
        when(migrator.compare(sourceSchema, targetSchema)).thenReturn(schemaDiff);

        // Act
        SchemaDiff result = migrator.compare(sourceSchema, targetSchema);

        // Assert
        assertNotNull(result);
        assertEquals(schemaDiff, result);
        verify(migrator).compare(sourceSchema, targetSchema);
    }

    @Test
    void testCompareThrowsExceptionOnError() throws MigrationException {
        // Arrange
        when(migrator.compare(sourceSchema, targetSchema))
                .thenThrow(
                        new MigrationException(
                                MigrationException.ErrorCode.UNKNOWN_ERROR, "Comparison failed"));

        // Act & Assert
        MigrationException exception =
                assertThrows(
                        MigrationException.class,
                        () -> migrator.compare(sourceSchema, targetSchema));

        assertEquals("Comparison failed", exception.getMessage());
    }

    @Test
    void testValidateMigrationReturnsValidationResult() {
        // Arrange
        ValidationResult mockResult = Mockito.mock(ValidationResult.class);
        when(migrator.validateMigration(script, targetSchema)).thenReturn(mockResult);

        // Act
        ValidationResult result = migrator.validateMigration(script, targetSchema);

        // Assert
        assertNotNull(result);
        assertEquals(mockResult, result);
        verify(migrator).validateMigration(script, targetSchema);
    }
}
