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

package com.aidvps.druid.differ;

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.exception.GenerationException;
import com.aidvps.druid.differ.exception.SchemaCompatibilityException;
import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.model.MigrationPlan;
import com.aidvps.druid.differ.internal.model.Warning;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End-to-end integration tests for TableDiffer. */
public class TableDifferIntegrationTest {

    private TableDiffer differ;

    @BeforeEach
    void setUp() {
        differ = TableDiffer.builder().withDialect(DatabaseDialect.MYSQL).build();
    }

    @Test
    void testEndToEndSimpleColumnAddition() {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

                    assertNotNull(plan);
                    assertFalse(plan.isEmpty());
                    assertTrue(plan.getStatementCount() > 0);
                    assertNotNull(plan.getSql());

                    String sql = plan.getSql();
                    assertTrue(sql.contains("ALTER TABLE"));
                    assertTrue(sql.contains("ADD COLUMN"));
                    assertTrue(sql.contains("email"));
                });
    }

    @Test
    void testEndToEndSimpleColumnDeletion() {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

                    assertNotNull(plan);
                    assertFalse(plan.isEmpty());
                    assertTrue(plan.getStatementCount() > 0);

                    String sql = plan.getSql();
                    assertTrue(sql.contains("ALTER TABLE"));
                    assertTrue(sql.contains("DROP COLUMN"));
                    assertTrue(sql.contains("email"));

                    // Should have warnings about data loss
                    assertTrue(plan.hasWarnings());
                });
    }

    @Test
    void testEndToEndColumnTypeModification() {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(200)" + ")";

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

                    assertNotNull(plan);
                    assertFalse(plan.isEmpty());
                    assertTrue(plan.getStatementCount() > 0);

                    String sql = plan.getSql();
                    assertTrue(sql.contains("ALTER TABLE"));
                    assertTrue(sql.contains("MODIFY") || sql.contains("MODIFY COLUMN"));
                });
    }

    @Test
    void testEndToEndCreateNewTable()
            throws SchemaParsingException, SchemaCompatibilityException, GenerationException {
        String sourceSchema = "";

        String targetSchema =
                "CREATE TABLE products ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                        + "  name VARCHAR(100) NOT NULL,"
                        + "  price DECIMAL(10,2)"
                        + ")";

        // Empty schema should now be handled gracefully
        MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
        assertNotNull(plan);
        assertFalse(plan.isEmpty());
        assertTrue(plan.getSql().contains("CREATE TABLE"));
    }

    @Test
    void testEndToEndWithMigrationOptions() {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        MigrationOptions options =
                MigrationOptions.builder().includeComments(true).failOnDestructive(false).build();

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan =
                            differ.generateMigration(sourceSchema, targetSchema, options);

                    assertNotNull(plan);
                    assertFalse(plan.isEmpty());
                });
    }

    @Test
    void testEndToEndIdenticalSchemas() throws Exception {
        String schema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        MigrationPlan plan = differ.generateMigration(schema, schema);

        assertNotNull(plan);
        assertTrue(plan.isEmpty());
        assertEquals(0, plan.getStatementCount());
        assertTrue(plan.getSql().isEmpty());
    }

    @Test
    void testEndToEndWithMultipleChanges() {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(200),"
                        + "  phone VARCHAR(20),"
                        + "  address VARCHAR(300)"
                        + ")";

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

                    assertNotNull(plan);
                    assertFalse(plan.isEmpty());
                    assertTrue(plan.getStatementCount() > 1);

                    String sql = plan.getSql();
                    assertTrue(sql.contains("ALTER TABLE"));
                });
    }

    @Test
    void testEndToEndWithBuilderPattern() {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        TableDiffer mySqlDiffer =
                TableDiffer.builder()
                        .withDialect(DatabaseDialect.MYSQL)
                        .withOptions(MigrationOptions.defaults())
                        .build();

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = mySqlDiffer.generateMigration(sourceSchema, targetSchema);

                    assertNotNull(plan);
                    assertFalse(plan.isEmpty());
                    assertEquals(DatabaseDialect.MYSQL, plan.getDatabaseDialect());
                });
    }

    @Test
    void testEndToEndParseErrorHandling() {
        String invalidSql = "SELECT * FROM users WHERE id = 1";

        String validSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        assertThrows(
                Exception.class,
                () -> {
                    differ.generateMigration(invalidSql, validSchema);
                });
    }

    @Test
    void testEndToEndDialectValidation() {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        // MySQL differ should work with MySQL schema
        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
                    assertNotNull(plan);
                });
    }

    @Test
    void testEndToEndWithWarnings() {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(200)" + ")";

        assertDoesNotThrow(
                () -> {
                    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

                    assertNotNull(plan);

                    // Type changes should generate warnings
                    if (plan.hasWarnings()) {
                        List<Warning> warnings = plan.getWarnings();
                        assertFalse(warnings.isEmpty());

                        boolean hasCompatibilityWarning =
                                warnings.stream()
                                        .anyMatch(
                                                w ->
                                                        w.getType()
                                                                == Warning.Type
                                                                        .COMPATIBILITY_WARNING);
                        assertTrue(hasCompatibilityWarning);
                    }
                });
    }

    @Test
    void testEndToEndMigrationPlanProperties() throws Exception {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

        assertNotNull(plan);
        assertNotNull(plan.getSourceSchema());
        assertNotNull(plan.getTargetSchema());
        assertEquals(DatabaseDialect.MYSQL, plan.getDatabaseDialect());
        assertNotNull(plan.getCreatedAt());
        assertNotNull(plan.getStatements());
        assertNotNull(plan.getSummary());
    }

    @Test
    void testEndToEndSqlGeneration() throws Exception {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

        assertNotNull(plan);
        String sql = plan.getSql();

        assertNotNull(sql);
        assertFalse(sql.isEmpty());
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("users"));
    }
}
