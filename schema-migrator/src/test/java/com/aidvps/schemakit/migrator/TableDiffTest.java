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

import com.aidvps.druid.differ.internal.model.Table;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for TableDiff. */
class TableDiffTest {

    @Test
    void testBuilderWithAllFields() {
        // Arrange
        Table source = Mockito.mock(Table.class);
        Table target = Mockito.mock(Table.class);
        TableDiff.ColumnDiff columnDiff = Mockito.mock(TableDiff.ColumnDiff.class);
        TableDiff.IndexDiff indexDiff = Mockito.mock(TableDiff.IndexDiff.class);
        TableDiff.ConstraintDiff constraintDiff = Mockito.mock(TableDiff.ConstraintDiff.class);
        when(source.getName()).thenReturn("source_table");
        when(target.getName()).thenReturn("target_table");

        // Act
        TableDiff diff =
                TableDiff.builder()
                        .tableName("mytable")
                        .source(source)
                        .target(target)
                        .columnDiff(columnDiff)
                        .indexDiff(indexDiff)
                        .constraintDiff(constraintDiff)
                        .isNew(false)
                        .isDeleted(false)
                        .isModified(true)
                        .build();

        // Assert
        assertEquals("mytable", diff.getTableName());
        assertEquals(source, diff.getSource());
        assertEquals(target, diff.getTarget());
        assertEquals(1, diff.getColumnDiffs().size());
        assertEquals(1, diff.getIndexDiffs().size());
        assertEquals(1, diff.getConstraintDiffs().size());
        assertFalse(diff.isNew());
        assertFalse(diff.isDeleted());
        assertTrue(diff.isModified());
    }

    @Test
    void testBuilderWithMinimalConfig() {
        // Act
        TableDiff diff = TableDiff.builder().tableName("testtable").build();

        // Assert
        assertEquals("testtable", diff.getTableName());
        assertNull(diff.getSource());
        assertNull(diff.getTarget());
        assertEquals(0, diff.getColumnDiffs().size());
        assertEquals(0, diff.getIndexDiffs().size());
        assertEquals(0, diff.getConstraintDiffs().size());
        assertFalse(diff.isNew());
        assertFalse(diff.isDeleted());
        assertFalse(diff.isModified());
    }

    @Test
    void testBuilderWithNewTable() {
        // Arrange
        Table target = Mockito.mock(Table.class);
        when(target.getName()).thenReturn("new_table");

        // Act
        TableDiff diff =
                TableDiff.builder().tableName("new_table").target(target).isNew(true).build();

        // Assert
        assertTrue(diff.isNew());
        assertFalse(diff.isDeleted());
        assertFalse(diff.isModified());
    }

    @Test
    void testBuilderWithDeletedTable() {
        // Arrange
        Table source = Mockito.mock(Table.class);
        when(source.getName()).thenReturn("deleted_table");

        // Act
        TableDiff diff =
                TableDiff.builder()
                        .tableName("deleted_table")
                        .source(source)
                        .isDeleted(true)
                        .build();

        // Assert
        assertTrue(diff.isDeleted());
        assertFalse(diff.isNew());
        assertFalse(diff.isModified());
    }

    @Test
    void testBuilderWithModifiedTable() {
        // Arrange
        TableDiff.ColumnDiff columnDiff = Mockito.mock(TableDiff.ColumnDiff.class);

        // Act
        TableDiff diff =
                TableDiff.builder()
                        .tableName("modified_table")
                        .columnDiff(columnDiff)
                        .isModified(true)
                        .build();

        // Assert
        assertTrue(diff.isModified());
        assertFalse(diff.isNew());
        assertFalse(diff.isDeleted());
    }

    @Test
    void testGetColumnDiffsReturnsUnmodifiableSet() {
        // Arrange
        TableDiff.ColumnDiff columnDiff = Mockito.mock(TableDiff.ColumnDiff.class);

        // Act
        TableDiff diff = TableDiff.builder().tableName("testtable").columnDiff(columnDiff).build();

        // Act
        Set<TableDiff.ColumnDiff> columnDiffs = diff.getColumnDiffs();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    columnDiffs.add(Mockito.mock(TableDiff.ColumnDiff.class));
                });
    }

    @Test
    void testGetIndexDiffsReturnsUnmodifiableSet() {
        // Arrange
        TableDiff.IndexDiff indexDiff = Mockito.mock(TableDiff.IndexDiff.class);

        // Act
        TableDiff diff = TableDiff.builder().tableName("testtable").indexDiff(indexDiff).build();

        // Act
        Set<TableDiff.IndexDiff> indexDiffs = diff.getIndexDiffs();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    indexDiffs.add(Mockito.mock(TableDiff.IndexDiff.class));
                });
    }

    @Test
    void testGetConstraintDiffsReturnsUnmodifiableSet() {
        // Arrange
        TableDiff.ConstraintDiff constraintDiff = Mockito.mock(TableDiff.ConstraintDiff.class);

        // Act
        TableDiff diff =
                TableDiff.builder().tableName("testtable").constraintDiff(constraintDiff).build();

        // Act
        Set<TableDiff.ConstraintDiff> constraintDiffs = diff.getConstraintDiffs();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    constraintDiffs.add(Mockito.mock(TableDiff.ConstraintDiff.class));
                });
    }

    @Test
    void testEqualsAndHashCode() {
        // Arrange
        Table source1 = Mockito.mock(Table.class);
        Table target1 = Mockito.mock(Table.class);
        Table source2 = Mockito.mock(Table.class);
        Table target2 = Mockito.mock(Table.class);
        when(source1.getName()).thenReturn("table1");
        when(source2.getName()).thenReturn("table1");
        when(target1.getName()).thenReturn("table1");
        when(target2.getName()).thenReturn("table1");

        TableDiff diff1 =
                TableDiff.builder().tableName("testtable").source(source1).target(target1).build();

        TableDiff diff2 =
                TableDiff.builder().tableName("testtable").source(source2).target(target2).build();

        // Act & Assert
        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    void testEqualsWithDifferentTableNames() {
        // Arrange
        TableDiff diff1 = TableDiff.builder().tableName("table1").build();

        TableDiff diff2 = TableDiff.builder().tableName("table2").build();

        // Act & Assert
        assertNotEquals(diff1, diff2);
    }

    @Test
    void testEqualsWithSelf() {
        // Arrange
        TableDiff diff = TableDiff.builder().tableName("testtable").build();

        // Act & Assert
        assertEquals(diff, diff);
    }

    @Test
    void testEqualsWithNull() {
        // Arrange
        TableDiff diff = TableDiff.builder().tableName("testtable").build();

        // Act & Assert
        assertNotEquals(diff, null);
    }

    @Test
    void testEqualsWithDifferentType() {
        // Arrange
        TableDiff diff = TableDiff.builder().tableName("testtable").build();

        // Act & Assert
        assertNotEquals(diff, "testtable");
    }

    @Test
    void testToString() {
        // Arrange
        Table source = Mockito.mock(Table.class);
        Table target = Mockito.mock(Table.class);
        TableDiff.ColumnDiff columnDiff = Mockito.mock(TableDiff.ColumnDiff.class);
        when(source.getName()).thenReturn("source_table");
        when(target.getName()).thenReturn("target_table");

        // Act
        TableDiff diff =
                TableDiff.builder()
                        .tableName("mytable")
                        .source(source)
                        .target(target)
                        .columnDiff(columnDiff)
                        .isNew(false)
                        .isDeleted(false)
                        .isModified(true)
                        .build();

        // Act
        String str = diff.toString();

        // Assert
        assertTrue(str.contains("TableDiff"));
        assertTrue(str.contains("tableName='mytable'"));
        assertTrue(str.contains("isNew=false"));
        assertTrue(str.contains("isDeleted=false"));
        assertTrue(str.contains("isModified=true"));
    }

    @Test
    void testBuilderReturnsThisForMethodChaining() {
        // Act
        TableDiff.Builder builder = TableDiff.builder();

        // Assert
        assertSame(builder, builder.tableName("test"));
        assertSame(builder, builder.source(Mockito.mock(Table.class)));
        assertSame(builder, builder.target(Mockito.mock(Table.class)));
        assertSame(builder, builder.columnDiff(Mockito.mock(TableDiff.ColumnDiff.class)));
        assertSame(builder, builder.indexDiff(Mockito.mock(TableDiff.IndexDiff.class)));
        assertSame(builder, builder.constraintDiff(Mockito.mock(TableDiff.ConstraintDiff.class)));
    }

    @Test
    void testBuilderReuseProducesIndependentInstances() {
        // Arrange
        Table source = Mockito.mock(Table.class);

        // Act
        TableDiff diff1 = TableDiff.builder().tableName("table1").source(source).build();

        TableDiff diff2 = TableDiff.builder().tableName("table2").build();

        // Assert
        assertNotSame(diff1, diff2);
        assertEquals("table1", diff1.getTableName());
        assertEquals("table2", diff2.getTableName());
        assertEquals(source, diff1.getSource());
        assertNull(diff2.getSource());
    }

    @Test
    void testMultipleDiffs() {
        // Arrange
        TableDiff.ColumnDiff columnDiff1 = Mockito.mock(TableDiff.ColumnDiff.class);
        TableDiff.ColumnDiff columnDiff2 = Mockito.mock(TableDiff.ColumnDiff.class);
        TableDiff.IndexDiff indexDiff1 = Mockito.mock(TableDiff.IndexDiff.class);
        TableDiff.IndexDiff indexDiff2 = Mockito.mock(TableDiff.IndexDiff.class);

        // Act
        TableDiff diff =
                TableDiff.builder()
                        .tableName("testtable")
                        .columnDiff(columnDiff1)
                        .columnDiff(columnDiff2)
                        .indexDiff(indexDiff1)
                        .indexDiff(indexDiff2)
                        .build();

        // Assert
        assertEquals(2, diff.getColumnDiffs().size());
        assertEquals(2, diff.getIndexDiffs().size());
        assertTrue(diff.getColumnDiffs().contains(columnDiff1));
        assertTrue(diff.getColumnDiffs().contains(columnDiff2));
        assertTrue(diff.getIndexDiffs().contains(indexDiff1));
        assertTrue(diff.getIndexDiffs().contains(indexDiff2));
    }

    @Test
    void testBuilderWithNullDiffValues() {
        // Act
        TableDiff diff =
                TableDiff.builder()
                        .tableName("testtable")
                        .columnDiff(null)
                        .indexDiff(null)
                        .constraintDiff(null)
                        .build();

        // Assert
        assertEquals(0, diff.getColumnDiffs().size());
        assertEquals(0, diff.getIndexDiffs().size());
        assertEquals(0, diff.getConstraintDiffs().size());
    }
}
