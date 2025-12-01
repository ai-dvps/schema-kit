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

import com.aidvps.druid.differ.internal.model.Database;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for DatabaseDiff. */
class DatabaseDiffTest {

    @Test
    void testBuilderWithAllFields() {
        // Arrange
        Database source = Mockito.mock(Database.class);
        Database target = Mockito.mock(Database.class);
        TableDiff tableDiff = Mockito.mock(TableDiff.class);
        when(source.getName()).thenReturn("sourcedb");
        when(target.getName()).thenReturn("targetdb");

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder()
                        .databaseName("mydb")
                        .source(source)
                        .target(target)
                        .tableDiff(tableDiff)
                        .isNew(false)
                        .isDeleted(false)
                        .build();

        // Assert
        assertEquals("mydb", diff.getDatabaseName());
        assertEquals(source, diff.getSource());
        assertEquals(target, diff.getTarget());
        assertEquals(1, diff.getTableDiffs().size());
        assertTrue(diff.getTableDiffs().contains(tableDiff));
        assertFalse(diff.isNew());
        assertFalse(diff.isDeleted());
    }

    @Test
    void testBuilderWithMinimalConfig() {
        // Act
        DatabaseDiff diff = DatabaseDiff.builder().databaseName("testdb").build();

        // Assert
        assertEquals("testdb", diff.getDatabaseName());
        assertNull(diff.getSource());
        assertNull(diff.getTarget());
        assertEquals(0, diff.getTableDiffs().size());
        assertFalse(diff.isNew());
        assertFalse(diff.isDeleted());
        assertFalse(diff.isModified());
    }

    @Test
    void testBuilderWithNewDatabase() {
        // Arrange
        Database target = Mockito.mock(Database.class);
        when(target.getName()).thenReturn("newdb");

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder().databaseName("newdb").target(target).isNew(true).build();

        // Assert
        assertTrue(diff.isNew());
        assertFalse(diff.isDeleted());
        assertFalse(diff.isModified());
    }

    @Test
    void testBuilderWithDeletedDatabase() {
        // Arrange
        Database source = Mockito.mock(Database.class);
        when(source.getName()).thenReturn("olddb");

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder().databaseName("olddb").source(source).isDeleted(true).build();

        // Assert
        assertTrue(diff.isDeleted());
        assertFalse(diff.isNew());
        assertFalse(diff.isModified());
    }

    @Test
    void testIsModifiedReturnsTrue() {
        // Arrange
        TableDiff tableDiff = Mockito.mock(TableDiff.class);

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder().databaseName("testdb").tableDiff(tableDiff).build();

        // Assert
        assertTrue(diff.isModified());
    }

    @Test
    void testIsModifiedReturnsFalseForNewDatabase() {
        // Arrange
        Database target = Mockito.mock(Database.class);

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder().databaseName("newdb").target(target).isNew(true).build();

        // Assert
        assertFalse(diff.isModified());
    }

    @Test
    void testIsModifiedReturnsFalseForDeletedDatabase() {
        // Arrange
        Database source = Mockito.mock(Database.class);

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder().databaseName("olddb").source(source).isDeleted(true).build();

        // Assert
        assertFalse(diff.isModified());
    }

    @Test
    void testGetTableDiffsReturnsUnmodifiableSet() {
        // Arrange
        TableDiff tableDiff = Mockito.mock(TableDiff.class);

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder().databaseName("testdb").tableDiff(tableDiff).build();

        // Act
        Set<TableDiff> tableDiffs = diff.getTableDiffs();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    tableDiffs.add(Mockito.mock(TableDiff.class));
                });
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        Database source1 = Mockito.mock(Database.class);
        Database target1 = Mockito.mock(Database.class);
        Database source2 = Mockito.mock(Database.class);
        Database target2 = Mockito.mock(Database.class);
        when(source1.getName()).thenReturn("db1");
        when(source2.getName()).thenReturn("db1");
        when(target1.getName()).thenReturn("db1");
        when(target2.getName()).thenReturn("db1");

        DatabaseDiff diff1 =
                DatabaseDiff.builder()
                        .databaseName("testdb")
                        .source(source1)
                        .target(target1)
                        .build();

        DatabaseDiff diff2 =
                DatabaseDiff.builder()
                        .databaseName("testdb")
                        .source(source2)
                        .target(target2)
                        .build();

        // Act & Assert
        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    void testEqualsWithDifferentNames() {
        // Arrange
        DatabaseDiff diff1 = DatabaseDiff.builder().databaseName("db1").build();

        DatabaseDiff diff2 = DatabaseDiff.builder().databaseName("db2").build();

        // Act & Assert
        assertNotEquals(diff1, diff2);
    }

    @Test
    void testEqualsWithSelf() {
        // Arrange
        DatabaseDiff diff = DatabaseDiff.builder().databaseName("testdb").build();

        // Act & Assert
        assertEquals(diff, diff);
    }

    @Test
    void testEqualsWithNull() {
        // Arrange
        DatabaseDiff diff = DatabaseDiff.builder().databaseName("testdb").build();

        // Act & Assert
        assertNotEquals(diff, null);
    }

    @Test
    void testEqualsWithDifferentType() {
        // Arrange
        DatabaseDiff diff = DatabaseDiff.builder().databaseName("testdb").build();

        // Act & Assert
        assertNotEquals(diff, "testdb");
    }

    @Test
    void testToString() {
        // Arrange
        Database source = Mockito.mock(Database.class);
        Database target = Mockito.mock(Database.class);
        TableDiff tableDiff = Mockito.mock(TableDiff.class);
        when(source.getName()).thenReturn("sourcedb");
        when(target.getName()).thenReturn("targetdb");

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder()
                        .databaseName("mydb")
                        .source(source)
                        .target(target)
                        .tableDiff(tableDiff)
                        .build();

        // Act
        String str = diff.toString();

        // Assert
        assertTrue(str.contains("DatabaseDiff"));
        assertTrue(str.contains("databaseName='mydb'"));
        assertTrue(str.contains("isNew=false"));
        assertTrue(str.contains("isDeleted=false"));
    }

    @Test
    void testBuilderReturnsThisForMethodChaining() {
        // Act
        DatabaseDiff.Builder builder = DatabaseDiff.builder();

        // Assert
        assertSame(builder, builder.databaseName("testdb"));
        assertSame(builder, builder.source(Mockito.mock(Database.class)));
        assertSame(builder, builder.target(Mockito.mock(Database.class)));
        assertSame(builder, builder.tableDiff(Mockito.mock(TableDiff.class)));
    }

    @Test
    void testBuilderReuseProducesIndependentInstances() {
        // Arrange
        Database source = Mockito.mock(Database.class);

        // Act
        DatabaseDiff diff1 = DatabaseDiff.builder().databaseName("db1").source(source).build();

        DatabaseDiff diff2 = DatabaseDiff.builder().databaseName("db2").build();

        // Assert
        assertNotSame(diff1, diff2);
        assertEquals("db1", diff1.getDatabaseName());
        assertEquals("db2", diff2.getDatabaseName());
        assertEquals(source, diff1.getSource());
        assertNull(diff2.getSource());
    }

    @Test
    void testMultipleTableDiffs() {
        // Arrange
        TableDiff tableDiff1 = Mockito.mock(TableDiff.class);
        TableDiff tableDiff2 = Mockito.mock(TableDiff.class);

        // Act
        DatabaseDiff diff =
                DatabaseDiff.builder()
                        .databaseName("testdb")
                        .tableDiff(tableDiff1)
                        .tableDiff(tableDiff2)
                        .build();

        // Assert
        assertEquals(2, diff.getTableDiffs().size());
        assertTrue(diff.getTableDiffs().contains(tableDiff1));
        assertTrue(diff.getTableDiffs().contains(tableDiff2));
    }
}
