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

package com.aidvps.druid.differ.internal.generator;

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.internal.comparator.ChangeDetector;
import com.aidvps.druid.differ.internal.model.*;
import com.aidvps.druid.differ.internal.model.constraint.PrimaryKey;
import com.aidvps.druid.differ.internal.model.constraint.UniqueConstraint;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for OracleMigrationGenerator.
 *
 * <p>These tests verify Oracle-specific syntax including:
 *
 * <ul>
 *   <li>VARCHAR2 instead of VARCHAR
 *   <li>NUMBER instead of INT/BIGINT
 *   <li>Parentheses for multi-column ALTER TABLE operations
 *   <li>CLOB for large text fields
 *   <li>Constraint name requirements for all DROP operations
 *   <li>GENERATED ALWAYS AS IDENTITY for auto-increment
 * </ul>
 */
public class OracleMigrationGeneratorTest {

    private OracleMigrationGenerator generator;
    private ChangeDetector detector;

    @BeforeEach
    public void setup() {
        generator = new OracleMigrationGenerator(false);
        detector = new ChangeDetector();
    }

    /** Helper method to create a SchemaDiff for table modifications. */
    private SchemaDiff createTableDiff(String tableName, Table sourceTable, Table targetTable) {
        Schema sourceSchema = Schema.builder(DatabaseDialect.ORACLE).addTable(sourceTable).build();
        Schema targetSchema = Schema.builder(DatabaseDialect.ORACLE).addTable(targetTable).build();
        return detector.compare(sourceSchema, targetSchema);
    }

    /** Helper method to create a SchemaDiff for added tables. */
    private SchemaDiff createAddedTableDiff(Table table) {
        Schema sourceSchema = Schema.builder(DatabaseDialect.ORACLE).build();
        Schema targetSchema = Schema.builder(DatabaseDialect.ORACLE).addTable(table).build();
        return detector.compare(sourceSchema, targetSchema);
    }

    /** Test VARCHAR2 conversion from VARCHAR. */
    @Test
    public void testVarchar2Conversion() {
        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle uses VARCHAR2, not VARCHAR
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("VARCHAR2(100)")),
                "Should use VARCHAR2 for Oracle");
        assertFalse(
                statements.stream()
                        .anyMatch(s -> s.contains("VARCHAR(") && !s.contains("VARCHAR2")),
                "Should not use plain VARCHAR");
    }

    /** Test NUMBER conversion from INT/BIGINT. */
    @Test
    public void testNumberConversion() {
        Table targetTable =
                Table.builder("data")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("big_id", "BIGINT").build())
                        .addColumn(Column.builder("count", "INTEGER").build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle converts INT/INTEGER to NUMBER(10)
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("NUMBER(10)")),
                "Should convert INT to NUMBER(10)");
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("NUMBER(19)")),
                "Should convert BIGINT to NUMBER(19)");
    }

    /** Test CLOB for large text fields. */
    @Test
    public void testClobHandling() {
        Table targetTable =
                Table.builder("documents")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("content", "TEXT").build())
                        .addColumn(Column.builder("description", "CLOB").build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle uses CLOB for TEXT
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("CLOB")), "Should use CLOB for TEXT");
    }

    /** Test GENERATED ALWAYS AS IDENTITY for auto-increment. */
    @Test
    public void testIdentityGeneration() {
        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").autoIncrement(true).build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle uses GENERATED ALWAYS AS IDENTITY
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("GENERATED ALWAYS AS IDENTITY")),
                "Should use GENERATED ALWAYS AS IDENTITY for Oracle");
        assertFalse(
                statements.stream().anyMatch(s -> s.contains("AUTO_INCREMENT")),
                "Should not use MySQL's AUTO_INCREMENT syntax");
    }

    /** Test parentheses in ALTER TABLE operations. */
    @Test
    public void testParenthesesInAlterTable() {
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(200).build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .build();

        List<String> statements =
                generator.generate(createTableDiff("users", sourceTable, targetTable));

        // Oracle groups ALTER TABLE operations in parentheses
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("ADD (") || s.contains("MODIFY (")),
                "Should use parentheses for ALTER TABLE operations");
    }

    /** Test constraint name requirements for DROP operations. */
    @Test
    public void testConstraintNameRequirements() {
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .addConstraint("pk_users", new PrimaryKey("pk_users", List.of("id")))
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        List<String> statements =
                generator.generate(createTableDiff("users", sourceTable, targetTable));

        // Oracle requires constraint names for DROP CONSTRAINT
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("DROP CONSTRAINT")),
                "Should use DROP CONSTRAINT");
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("pk_users")),
                "Should specify constraint name");
    }

    /** Test DECIMAL precision and scale handling. */
    @Test
    public void testDecimalPrecisionAndScale() {
        Table targetTable =
                Table.builder("products")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("price", "DECIMAL").precision(10, 2).build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle handles DECIMAL with precision and scale
        assertTrue(
                statements.stream()
                        .anyMatch(s -> s.contains("DECIMAL(10,2)") || s.contains("NUMBER(10,2)")),
                "Should include precision and scale for DECIMAL");
    }

    /** Test grouped ALTER TABLE operations. */
    @Test
    public void testGroupedAlterTableOperations() {
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(200).build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .addColumn(Column.builder("age", "INT").build())
                        .build();

        List<String> statements =
                generator.generate(createTableDiff("users", sourceTable, targetTable));

        // Oracle groups operations in single ALTER TABLE statement
        long alterTableCount = statements.stream().filter(s -> s.startsWith("ALTER TABLE")).count();

        assertTrue(alterTableCount > 0, "Should have ALTER TABLE statements");
    }

    /** Test CREATE TABLE with Oracle-specific syntax. */
    @Test
    public void testCreateTableGeneration() {
        Table table =
                Table.builder("employees")
                        .addColumn(Column.builder("id", "INT").autoIncrement(true).build())
                        .addColumn(
                                Column.builder("name", "VARCHAR")
                                        .length(100)
                                        .nullable(false)
                                        .build())
                        .addColumn(Column.builder("salary", "DECIMAL").precision(10, 2).build())
                        .addConstraint(
                                "pk_employees", new PrimaryKey("pk_employees", List.of("id")))
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(table));

        String createTableStatement =
                statements.stream().filter(s -> s.contains("CREATE TABLE")).findFirst().orElse("");

        // Verify Oracle-specific syntax
        assertTrue(createTableStatement.contains("CREATE TABLE"), "Should contain CREATE TABLE");
        assertTrue(
                createTableStatement.contains("GENERATED ALWAYS AS IDENTITY"),
                "Should use IDENTITY for auto-increment");
        assertFalse(
                createTableStatement.contains("AUTO_INCREMENT"),
                "Should not use MySQL AUTO_INCREMENT");
    }

    /** Test dropping and adding columns with Oracle syntax. */
    @Test
    public void testDropAndAddColumns() {
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("old_column", "VARCHAR").length(100).build())
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("new_column", "VARCHAR").length(200).build())
                        .build();

        List<String> statements =
                generator.generate(createTableDiff("users", sourceTable, targetTable));

        // Oracle syntax for dropping and adding columns
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("DROP (")),
                "Should use DROP (...) syntax");
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("ADD (")),
                "Should use ADD (...) syntax");
    }

    /** Test handling of UUID as VARCHAR2(36). */
    @Test
    public void testUuidHandling() {
        Table targetTable =
                Table.builder("data")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("uuid_col", "UUID").build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle doesn't have native UUID, use VARCHAR2(36)
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("VARCHAR2(36)")),
                "Should convert UUID to VARCHAR2(36) for Oracle");
    }

    /** Test BOOLEAN as NUMBER(1). */
    @Test
    public void testBooleanHandling() {
        Table targetTable =
                Table.builder("flags")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("active", "BOOLEAN").build())
                        .build();

        List<String> statements = generator.generate(createAddedTableDiff(targetTable));

        // Oracle uses NUMBER(1) for BOOLEAN
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("NUMBER(1)")),
                "Should convert BOOLEAN to NUMBER(1) for Oracle");
    }

    /** Test empty diff produces no statements. */
    @Test
    public void testEmptyDiffProducesNoStatements() {
        Schema emptySchema = Schema.builder(DatabaseDialect.ORACLE).build();
        SchemaDiff emptyDiff = detector.compare(emptySchema, emptySchema);
        List<String> statements = generator.generate(emptyDiff);

        assertTrue(statements.isEmpty(), "Empty diff should produce no statements");
    }

    /** Test adding unique constraint. */
    @Test
    public void testAddUniqueConstraint() {
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .addConstraint(
                                "uk_users_email",
                                new UniqueConstraint("uk_users_email", List.of("email")))
                        .build();

        List<String> statements =
                generator.generate(createTableDiff("users", sourceTable, targetTable));

        assertTrue(
                statements.stream().anyMatch(s -> s.contains("ADD CONSTRAINT")),
                "Should use ADD CONSTRAINT syntax");
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("uk_users_email")),
                "Should specify constraint name");
    }
}
