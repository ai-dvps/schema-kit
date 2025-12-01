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

import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for MigrationScript. */
class MigrationScriptTest {

    @Test
    void testBuilderWithAllOptions() {
        // Arrange
        MigrationStatement statement1 = Mockito.mock(MigrationStatement.class);
        MigrationStatement statement2 = Mockito.mock(MigrationStatement.class);
        when(statement1.getSql()).thenReturn("CREATE TABLE users");
        when(statement2.getSql()).thenReturn("INSERT INTO users");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("author", "test");

        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .statement(statement1)
                        .statement(statement2)
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .mode(MigrationMode.FULL)
                        .metadata("author", "test")
                        .build();

        // Assert
        assertEquals(DatabasePlatform.MYSQL, script.getTargetPlatform());
        assertEquals(MigrationMode.FULL, script.getMode());
        assertEquals(2, script.getStatements().size());
        assertTrue(script.getStatements().contains(statement1));
        assertTrue(script.getStatements().contains(statement2));
        Map<String, Object> scriptMetadata = script.getMetadata();
        assertEquals("test", scriptMetadata.get("author"));
    }

    @Test
    void testBuilderWithMinimalConfig() {
        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.POSTGRESQL)
                        .mode(MigrationMode.FULL)
                        .build();

        // Assert
        assertEquals(DatabasePlatform.POSTGRESQL, script.getTargetPlatform());
        assertEquals(MigrationMode.FULL, script.getMode());
        assertEquals(0, script.getStatements().size());
        assertNotNull(script.getMetadata());
    }

    @Test
    void testAddStatementReturnsSameInstance() {
        // Arrange
        MigrationStatement statement = Mockito.mock(MigrationStatement.class);
        MigrationScript script =
                MigrationScript.builder().targetPlatform(DatabasePlatform.MYSQL).mode(MigrationMode.FULL).build();

        // Act
        MigrationScript result = script.addStatement(statement);

        // Assert
        assertSame(script, result);
        assertEquals(1, script.getStatements().size());
    }

    @Test
    void testGetStatementsReturnsUnmodifiableList() {
        // Arrange
        MigrationStatement statement = Mockito.mock(MigrationStatement.class);
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .statement(statement)
                        .build();

        // Act
        java.util.List<MigrationStatement> statements = script.getStatements();

        // Assert - should be unmodifiable
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    statements.add(Mockito.mock(MigrationStatement.class));
                });
    }

    @Test
    void testToSqlConcatenatesStatements() {
        // Arrange
        MigrationStatement statement1 = Mockito.mock(MigrationStatement.class);
        MigrationStatement statement2 = Mockito.mock(MigrationStatement.class);
        when(statement1.getSql()).thenReturn("CREATE TABLE users");
        when(statement2.getSql()).thenReturn("INSERT INTO users");

        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.MARIADB)
                        .mode(MigrationMode.FULL)
                        .statement(statement1)
                        .statement(statement2)
                        .build();

        // Act
        String sql = script.toSql();

        // Assert
        assertTrue(sql.contains("CREATE TABLE users"));
        assertTrue(sql.contains("INSERT INTO users"));
        assertTrue(sql.endsWith("\n"));
    }

    @Test
    void testToSqlWithNoStatements() {
        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.SQLITE)
                        .mode(MigrationMode.FULL)
                        .build();

        // Act
        String sql = script.toSql();

        // Assert
        assertEquals("", sql);
    }

    @Test
    void testToFormattedSqlIncludesMetadata() {
        // Arrange
        MigrationStatement statement = Mockito.mock(MigrationStatement.class);
        when(statement.getSql()).thenReturn("CREATE TABLE users");
        when(statement.getDescription()).thenReturn("Create users table");

        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.POSTGRESQL)
                        .mode(MigrationMode.FULL)
                        .statement(statement)
                        .build();

        String formattedSql = script.toFormattedSql();

        // Assert
        assertTrue(formattedSql.contains("-- Migration Script"));
        assertTrue(formattedSql.contains("-- Target Platform: PostgreSQL"));
        assertTrue(formattedSql.contains("-- Mode: FULL"));
        assertTrue(formattedSql.contains("-- Create users table"));
        assertTrue(formattedSql.contains("CREATE TABLE users"));
    }

    @Test
    void testToFormattedSqlWithStatementWithoutDescription() {
        // Arrange
        MigrationStatement statement = Mockito.mock(MigrationStatement.class);
        when(statement.getSql()).thenReturn("CREATE TABLE users");
        when(statement.getDescription()).thenReturn(null);

        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .statement(statement)
                        .build();

        String formattedSql = script.toFormattedSql();

        // Assert
        assertTrue(formattedSql.contains("CREATE TABLE users"));
        assertTrue(formattedSql.contains("-- Migration Script"));
    }

    @Test
    void testGetMetadataReturnsCopy() {
        // Arrange
        Map<String, Object> originalMetadata = new HashMap<>();
        originalMetadata.put("key", "value");

        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.MARIADB)
                        .mode(MigrationMode.FULL)
                        .metadata("key", "value")
                        .build();

        // Act
        Map<String, Object> metadata = script.getMetadata();
        metadata.put("newKey", "newValue");

        // Assert - modification should not affect internal state
        Map<String, Object> metadata2 = script.getMetadata();
        assertTrue(metadata2.containsKey("newKey"));
        assertNotSame(metadata, script.getMetadata());
    }

    @Test
    void testBuilderWithNullStatement() {
        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.SQLITE)
                        .mode(MigrationMode.FULL)
                        .statement(null)
                        .build();

        // Assert
        assertEquals(0, script.getStatements().size());
    }

    @Test
    void testMultipleStatementsAdded() {
        // Arrange
        MigrationStatement statement1 = Mockito.mock(MigrationStatement.class);
        MigrationStatement statement2 = Mockito.mock(MigrationStatement.class);
        MigrationStatement statement3 = Mockito.mock(MigrationStatement.class);

        when(statement1.getSql()).thenReturn("SQL1");
        when(statement2.getSql()).thenReturn("SQL2");
        when(statement3.getSql()).thenReturn("SQL3");

        // Act
        MigrationScript script =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .mode(MigrationMode.FULL)
                        .statement(statement1)
                        .statement(statement2)
                        .statement(statement3)
                        .build();

        // Assert
        assertEquals(3, script.getStatements().size());
        assertTrue(script.getStatements().contains(statement1));
        assertTrue(script.getStatements().contains(statement2));
        assertTrue(script.getStatements().contains(statement3));
    }

    @Test
    void testBuilderReturnsThisForMethodChaining() {
        // Act
        MigrationScript.Builder builder = MigrationScript.builder();

        // Assert
        assertSame(builder, builder.targetPlatform(DatabasePlatform.MYSQL));
        assertSame(builder, builder.mode(MigrationMode.FULL));
        assertSame(builder, builder.statement(null));
        assertSame(builder, builder.metadata("key", "value"));
    }

    @Test
    void testBuilderReuseProducesIndependentInstances() {
        // Arrange
        MigrationStatement statement = Mockito.mock(MigrationStatement.class);

        // Act
        MigrationScript script1 =
                MigrationScript.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .mode(MigrationMode.FULL)
                        .statement(statement)
                        .build();

        MigrationScript script2 =
                MigrationScript.builder().targetPlatform(DatabasePlatform.POSTGRESQL).mode(MigrationMode.FULL).build();

        // Assert
        assertNotSame(script1, script2);
        assertEquals(DatabasePlatform.MYSQL, script1.getTargetPlatform());
        assertEquals(DatabasePlatform.POSTGRESQL, script2.getTargetPlatform());
        assertEquals(1, script1.getStatements().size());
        assertEquals(0, script2.getStatements().size());
    }
}
