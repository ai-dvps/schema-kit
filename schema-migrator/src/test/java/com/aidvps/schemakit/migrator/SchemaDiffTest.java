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
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for SchemaDiff. */
class SchemaDiffTest {

    @Test
    void testBuilderWithDatabaseDiff() {
        // Arrange
        DatabaseDiff diff1 = Mockito.mock(DatabaseDiff.class);
        DatabaseDiff diff2 = Mockito.mock(DatabaseDiff.class);
        when(diff1.getDatabaseName()).thenReturn("db1");
        when(diff2.getDatabaseName()).thenReturn("db2");

        // Act
        SchemaDiff schemaDiff =
                SchemaDiff.builder().databaseDiff(diff1).databaseDiff(diff2).build();

        // Assert
        Set<DatabaseDiff> diffs = schemaDiff.getDatabaseDiffs();
        assertEquals(2, diffs.size());
        assertTrue(diffs.contains(diff1));
        assertTrue(diffs.contains(diff2));
    }

    @Test
    void testBuilderWithChanges() {
        // Arrange
        SchemaChange change1 = Mockito.mock(SchemaChange.class);
        SchemaChange change2 = Mockito.mock(SchemaChange.class);

        // Act
        SchemaDiff schemaDiff = SchemaDiff.builder().change(change1).change(change2).build();

        // Assert
        Set<SchemaChange> changes = schemaDiff.getChanges();
        assertEquals(2, changes.size());
        assertTrue(changes.contains(change1));
        assertTrue(changes.contains(change2));
    }

    @Test
    void testBuilderWithBothDiffsAndChanges() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        when(diff.getDatabaseName()).thenReturn("testdb");
        SchemaChange change = Mockito.mock(SchemaChange.class);

        // Act
        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).change(change).build();

        // Assert
        assertEquals(1, schemaDiff.getDatabaseDiffs().size());
        assertEquals(1, schemaDiff.getChanges().size());
        assertTrue(schemaDiff.hasChanges());
    }

    @Test
    void testBuilderWithNoChanges() {
        // Act
        SchemaDiff schemaDiff = SchemaDiff.builder().build();

        // Assert
        assertFalse(schemaDiff.hasChanges());
        assertEquals(0, schemaDiff.getDatabaseDiffs().size());
        assertEquals(0, schemaDiff.getChanges().size());
    }

    @Test
    void testGetDatabaseDiffReturnsPresent() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        when(diff.getDatabaseName()).thenReturn("mydb");

        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).build();

        // Act
        java.util.Optional<DatabaseDiff> result = schemaDiff.getDatabaseDiff("mydb");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(diff, result.get());
    }

    @Test
    void testGetDatabaseDiffReturnsEmpty() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        when(diff.getDatabaseName()).thenReturn("db1");

        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).build();

        // Act
        java.util.Optional<DatabaseDiff> result = schemaDiff.getDatabaseDiff("db2");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testGetDatabaseDiffsReturnsUnmodifiableSet() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).build();

        // Act
        Set<DatabaseDiff> diffs = schemaDiff.getDatabaseDiffs();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    diffs.add(Mockito.mock(DatabaseDiff.class));
                });
    }

    @Test
    void testGetChangesReturnsUnmodifiableSet() {
        // Arrange
        SchemaChange change = Mockito.mock(SchemaChange.class);
        SchemaDiff schemaDiff = SchemaDiff.builder().change(change).build();

        // Act
        Set<SchemaChange> changes = schemaDiff.getChanges();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    changes.add(Mockito.mock(SchemaChange.class));
                });
    }

    @Test
    void testHasChangesReturnsTrue() {
        // Arrange
        SchemaChange change = Mockito.mock(SchemaChange.class);

        // Act
        SchemaDiff schemaDiff = SchemaDiff.builder().change(change).build();

        // Assert
        assertTrue(schemaDiff.hasChanges());
    }

    @Test
    void testHasChangesReturnsFalseForEmpty() {
        // Act
        SchemaDiff schemaDiff = SchemaDiff.builder().build();

        // Assert
        assertFalse(schemaDiff.hasChanges());
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        when(diff.getDatabaseName()).thenReturn("db1");

        SchemaDiff schemaDiff1 = SchemaDiff.builder().databaseDiff(diff).build();

        SchemaDiff schemaDiff2 = SchemaDiff.builder().databaseDiff(diff).build();

        // Act & Assert
        assertEquals(schemaDiff1, schemaDiff2);
        assertEquals(schemaDiff1.hashCode(), schemaDiff2.hashCode());
    }

    @Test
    void testEqualsWithDifferentDiffs() {
        // Arrange
        DatabaseDiff diff1 = Mockito.mock(DatabaseDiff.class);
        DatabaseDiff diff2 = Mockito.mock(DatabaseDiff.class);
        when(diff1.getDatabaseName()).thenReturn("db1");
        when(diff2.getDatabaseName()).thenReturn("db2");

        SchemaDiff schemaDiff1 = SchemaDiff.builder().databaseDiff(diff1).build();

        SchemaDiff schemaDiff2 = SchemaDiff.builder().databaseDiff(diff2).build();

        // Act & Assert
        assertNotEquals(schemaDiff1, schemaDiff2);
    }

    @Test
    void testEqualsWithSelf() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).build();

        // Act & Assert
        assertEquals(schemaDiff, schemaDiff);
    }

    @Test
    void testEqualsWithNull() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).build();

        // Act & Assert
        assertNotEquals(schemaDiff, null);
    }

    @Test
    void testToString() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);
        when(diff.getDatabaseName()).thenReturn("testdb");
        SchemaChange change = Mockito.mock(SchemaChange.class);

        // Act
        SchemaDiff schemaDiff = SchemaDiff.builder().databaseDiff(diff).change(change).build();

        // Act
        String str = schemaDiff.toString();

        // Assert
        assertTrue(str.contains("SchemaDiff"));
        assertTrue(str.contains("databaseDiffs=1"));
        assertTrue(str.contains("changes=1"));
    }

    @Test
    void testBuilderReturnsThisForMethodChaining() {
        // Act
        SchemaDiff.Builder builder = SchemaDiff.builder();

        // Assert
        assertSame(builder, builder.databaseDiff(Mockito.mock(DatabaseDiff.class)));
        assertSame(builder, builder.change(Mockito.mock(SchemaChange.class)));
    }

    @Test
    void testBuilderReuseProducesIndependentInstances() {
        // Arrange
        DatabaseDiff diff = Mockito.mock(DatabaseDiff.class);

        // Act
        SchemaDiff schemaDiff1 = SchemaDiff.builder().databaseDiff(diff).build();

        SchemaDiff schemaDiff2 = SchemaDiff.builder().build();

        // Assert
        assertNotSame(schemaDiff1, schemaDiff2);
        assertEquals(1, schemaDiff1.getDatabaseDiffs().size());
        assertEquals(0, schemaDiff2.getDatabaseDiffs().size());
    }
}
