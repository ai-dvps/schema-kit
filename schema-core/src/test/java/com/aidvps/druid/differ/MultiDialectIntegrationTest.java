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
import com.aidvps.druid.differ.internal.comparator.ChangeDetector;
import com.aidvps.druid.differ.internal.generator.MigrationGenerator;
import com.aidvps.druid.differ.internal.generator.MigrationGeneratorFactory;
import com.aidvps.druid.differ.internal.model.*;
import com.aidvps.druid.differ.internal.model.constraint.PrimaryKey;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for multi-dialect migration generation and execution.
 *
 * <p>This test suite verifies that:
 *
 * <ul>
 *   <li>The same schema migration generates correct SQL for MySQL, PostgreSQL, and Oracle
 *   <li>Dialect-specific features are properly handled
 *   <li>Migrations can be executed successfully on real databases
 *   <li>Schema state is correct after migration execution
 * </ul>
 */
@EnabledIf("com.aidvps.druid.differ.MultiDialectIntegrationTest#isDockerAvailable")
@Testcontainers
public class MultiDialectIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    private MigrationGeneratorFactory factory;
    private ChangeDetector detector;

    @BeforeEach
    public void setup() {
        factory = new MigrationGeneratorFactory();
        detector = new ChangeDetector();
    }

    /** Helper method to create SchemaDiff from table comparison. */
    private SchemaDiff createTableDiff(
            DatabaseDialect dialect, String tableName, Table sourceTable, Table targetTable) {
        Schema sourceSchema = Schema.builder(dialect).addTable(sourceTable).build();
        Schema targetSchema = Schema.builder(dialect).addTable(targetTable).build();
        return detector.compare(sourceSchema, targetSchema);
    }

    /** Helper method to create SchemaDiff for added tables. */
    private SchemaDiff createAddedTableDiff(DatabaseDialect dialect, Table table) {
        Schema sourceSchema = Schema.builder(dialect).build();
        Schema targetSchema = Schema.builder(dialect).addTable(table).build();
        return detector.compare(sourceSchema, targetSchema);
    }

    @AfterEach
    public void cleanup() throws Exception {
        // Clean up tables after each test
        cleanupDatabase(createMySQLDataSource());
        cleanupDatabase(createPostgreSQLDataSource());
    }

    /** Creates a DataSource for MySQL container. */
    private DataSource createMySQLDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(mysql.getJdbcUrl());
        config.setUsername(mysql.getUsername());
        config.setPassword(mysql.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return new HikariDataSource(config);
    }

    /** Creates a DataSource for PostgreSQL container. */
    private DataSource createPostgreSQLDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }

    /** Test that the same schema change generates correct SQL for all three dialects. */
    @Test
    public void testMultiDialectSqlGeneration() throws GenerationException {
        // Create a simple schema with various data types
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").autoIncrement(true).build())
                        .addColumn(Column.builder("name", "VARCHAR").length(200).build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .addColumn(Column.builder("created_at", "TIMESTAMP").build())
                        .addConstraint("pk_users", new PrimaryKey("pk_users", Arrays.asList("id")))
                        .build();

        SchemaDiff diff = createTableDiff(DatabaseDialect.MYSQL, "users", sourceTable, targetTable);

        // Test MySQL generation
        MigrationGenerator mysqlGenerator = factory.createGenerator(DatabaseDialect.MYSQL, true);
        List<String> mysqlStatements = mysqlGenerator.generate(diff);

        assertFalse(mysqlStatements.isEmpty(), "MySQL should generate statements");
        assertTrue(
                mysqlStatements.stream().anyMatch(s -> s.contains("MODIFY COLUMN")),
                "MySQL should use MODIFY COLUMN syntax");

        // Test PostgreSQL generation
        MigrationGenerator pgGenerator = factory.createGenerator(DatabaseDialect.POSTGRESQL, true);
        List<String> pgStatements = pgGenerator.generate(diff);

        assertFalse(pgStatements.isEmpty(), "PostgreSQL should generate statements");
        assertTrue(
                pgStatements.stream().anyMatch(s -> s.contains("ALTER COLUMN")),
                "PostgreSQL should use ALTER COLUMN syntax");
        assertTrue(
                pgStatements.stream().anyMatch(s -> s.contains("GENERATED BY DEFAULT AS IDENTITY")),
                "PostgreSQL should use IDENTITY for auto-increment");

        // Test Oracle generation
        MigrationGenerator oracleGenerator = factory.createGenerator(DatabaseDialect.ORACLE, true);
        List<String> oracleStatements = oracleGenerator.generate(diff);

        assertFalse(oracleStatements.isEmpty(), "Oracle should generate statements");
        assertTrue(
                oracleStatements.stream().anyMatch(s -> s.contains("VARCHAR2")),
                "Oracle should use VARCHAR2 instead of VARCHAR");
        assertTrue(
                oracleStatements.stream().anyMatch(s -> s.contains("GENERATED ALWAYS AS IDENTITY")),
                "Oracle should use GENERATED ALWAYS AS IDENTITY");
        assertTrue(
                oracleStatements.stream().anyMatch(s -> s.contains("MODIFY (")),
                "Oracle should use parentheses in MODIFY");
    }

    /** Test PostgreSQL-specific data types are preserved in migration SQL. */
    @Test
    public void testPostgreSQLSpecificTypes() throws GenerationException {
        Table sourceTable =
                Table.builder("data").addColumn(Column.builder("id", "INT").build()).build();

        Table targetTable =
                Table.builder("data")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("uuid_col", "UUID").build())
                        .addColumn(Column.builder("jsonb_col", "JSONB").build())
                        .addColumn(Column.builder("text_col", "TEXT").build())
                        .build();

        ChangeDetector detector = new ChangeDetector();
        SchemaDiff diff = createTableDiff(DatabaseDialect.MYSQL, "users", sourceTable, targetTable);

        MigrationGenerator pgGenerator = factory.createGenerator(DatabaseDialect.POSTGRESQL, false);
        List<String> statements = pgGenerator.generate(diff);

        assertTrue(
                statements.stream().anyMatch(s -> s.contains("UUID")),
                "Should preserve UUID type for PostgreSQL");
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("JSONB")),
                "Should preserve JSONB type for PostgreSQL");
        assertTrue(
                statements.stream().anyMatch(s -> s.contains("TEXT")),
                "Should preserve TEXT type for PostgreSQL");
    }

    /** Test Oracle-specific type conversions. */
    @Test
    public void testOracleTypeConversions() throws GenerationException {
        Table targetTable =
                Table.builder("conversions")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("big_id", "BIGINT").build())
                        .addColumn(Column.builder("varchar_col", "VARCHAR").length(100).build())
                        .addColumn(Column.builder("text_col", "TEXT").build())
                        .addColumn(Column.builder("bool_col", "BOOLEAN").build())
                        .addColumn(Column.builder("uuid_col", "UUID").build())
                        .build();

        SchemaDiff diff = createAddedTableDiff(DatabaseDialect.ORACLE, targetTable);

        MigrationGenerator oracleGenerator = factory.createGenerator(DatabaseDialect.ORACLE, false);
        List<String> statements = oracleGenerator.generate(diff);

        String sql = String.join("\n", statements);

        // Verify Oracle-specific conversions
        assertTrue(sql.contains("NUMBER(10)"), "Should convert INT to NUMBER(10)");
        assertTrue(sql.contains("NUMBER(19)"), "Should convert BIGINT to NUMBER(19)");
        assertTrue(sql.contains("VARCHAR2(100)"), "Should convert VARCHAR to VARCHAR2");
        assertTrue(sql.contains("CLOB"), "Should convert TEXT to CLOB");
        assertTrue(sql.contains("NUMBER(1)"), "Should convert BOOLEAN to NUMBER(1)");
        assertTrue(sql.contains("VARCHAR2(36)"), "Should convert UUID to VARCHAR2(36)");
    }

    /** Test that MySQL migration executes successfully. */
    @Test
    public void testMySQLMigrationExecution() throws Exception {
        // Create source and target schemas
        Table sourceTable =
                Table.builder("products")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("name", "VARCHAR").length(100).build())
                        .build();

        Table targetTable =
                Table.builder("products")
                        .addColumn(Column.builder("id", "INT").autoIncrement(true).build())
                        .addColumn(
                                Column.builder("name", "VARCHAR")
                                        .length(200)
                                        .nullable(false)
                                        .build())
                        .addColumn(Column.builder("price", "DECIMAL").precision(10, 2).build())
                        .addConstraint(
                                "pk_products", new PrimaryKey("pk_products", Arrays.asList("id")))
                        .build();

        // Generate migration
        SchemaDiff diff = createTableDiff(DatabaseDialect.MYSQL, "users", sourceTable, targetTable);

        MigrationGenerator mysqlGenerator = factory.createGenerator(DatabaseDialect.MYSQL, false);
        List<String> statements = mysqlGenerator.generate(diff);

        // Execute migration on MySQL container
        DataSource dataSource = createMySQLDataSource();

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                if (sql.trim().isEmpty() || sql.startsWith("--")) {
                    continue;
                }
                stmt.execute(sql);
            }

            // Verify table was created with correct structure
            ResultSet rs = stmt.executeQuery("DESCRIBE products");
            assertTrue(rs.next(), "products table should exist");

            // Check columns exist
            rs = stmt.executeQuery("DESCRIBE products");
            assertTrue(columnExists(rs, "id"), "id column should exist");
            assertTrue(columnExists(rs, "name"), "name column should exist");
            assertTrue(columnExists(rs, "price"), "price column should exist");

            // Verify primary key
            rs = stmt.executeQuery("SHOW KEYS FROM products WHERE Key_name = 'PRIMARY'");
            assertTrue(rs.next(), "Primary key should exist");
            assertEquals("id", rs.getString("Column_name"), "Primary key should be on id column");
        }
    }

    /** Test that PostgreSQL migration executes successfully. */
    @Test
    public void testPostgreSQLMigrationExecution() throws Exception {
        // Create source and target schemas
        Table sourceTable =
                Table.builder("orders")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("order_date", "TIMESTAMP").build())
                        .build();

        Table targetTable =
                Table.builder("orders")
                        .addColumn(Column.builder("id", "INT").autoIncrement(true).build())
                        .addColumn(Column.builder("order_date", "TIMESTAMP").build())
                        .addColumn(Column.builder("customer_email", "VARCHAR").length(255).build())
                        .addColumn(Column.builder("metadata", "JSONB").build())
                        .addConstraint(
                                "pk_orders", new PrimaryKey("pk_orders", Arrays.asList("id")))
                        .build();

        // Generate migration
        SchemaDiff diff =
                createTableDiff(DatabaseDialect.POSTGRESQL, "orders", sourceTable, targetTable);

        MigrationGenerator pgGenerator = factory.createGenerator(DatabaseDialect.POSTGRESQL, false);
        List<String> statements = pgGenerator.generate(diff);

        // Execute migration on PostgreSQL container
        DataSource dataSource = createPostgreSQLDataSource();

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                if (sql.trim().isEmpty() || sql.startsWith("--")) {
                    continue;
                }
                stmt.execute(sql);
            }

            // Verify table was created with correct structure
            ResultSet rs =
                    stmt.executeQuery(
                            "SELECT column_name, data_type FROM information_schema.columns "
                                    + "WHERE table_name = 'orders' ORDER BY ordinal_position");
            assertTrue(rs.next(), "orders table should exist");

            // Verify all columns exist
            rs =
                    stmt.executeQuery(
                            "SELECT column_name FROM information_schema.columns "
                                    + "WHERE table_name = 'orders'");
            assertTrue(hasColumn(rs, "id"), "id column should exist");
            assertTrue(hasColumn(rs, "order_date"), "order_date column should exist");
            assertTrue(hasColumn(rs, "customer_email"), "customer_email column should exist");
            assertTrue(hasColumn(rs, "metadata"), "metadata column should exist");

            // Verify primary key
            rs =
                    stmt.executeQuery(
                            "SELECT kcu.column_name FROM information_schema.table_constraints tc "
                                    + "JOIN information_schema.key_column_usage kcu "
                                    + "ON tc.constraint_name = kcu.constraint_name "
                                    + "WHERE tc.table_name = 'orders' AND tc.constraint_type = 'PRIMARY KEY'");
            assertTrue(rs.next(), "Primary key should exist");
            assertEquals("id", rs.getString("column_name"), "Primary key should be on id column");
        }
    }

    /** Test constraint handling across dialects. */
    @Test
    public void testConstraintHandlingAcrossDialects() throws GenerationException {
        Table sourceTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .build();

        Table targetTable =
                Table.builder("users")
                        .addColumn(Column.builder("id", "INT").build())
                        .addColumn(Column.builder("email", "VARCHAR").length(255).build())
                        .addIndex(
                                new Index.Builder()
                                        .name("idx_users_email")
                                        .addColumn("email")
                                        .unique(true)
                                        .build())
                        .build();

        SchemaDiff diff = createTableDiff(DatabaseDialect.MYSQL, "users", sourceTable, targetTable);

        // MySQL
        MigrationGenerator mysqlGenerator = factory.createGenerator(DatabaseDialect.MYSQL, false);
        List<String> mysqlStatements = mysqlGenerator.generate(diff);
        assertTrue(
                mysqlStatements.stream().anyMatch(s -> s.contains("CREATE UNIQUE INDEX")),
                "MySQL should generate CREATE INDEX");

        // PostgreSQL
        MigrationGenerator pgGenerator = factory.createGenerator(DatabaseDialect.POSTGRESQL, false);
        List<String> pgStatements = pgGenerator.generate(diff);
        assertTrue(
                pgStatements.stream().anyMatch(s -> s.contains("CREATE UNIQUE INDEX")),
                "PostgreSQL should generate CREATE INDEX");

        // Oracle
        MigrationGenerator oracleGenerator = factory.createGenerator(DatabaseDialect.ORACLE, false);
        List<String> oracleStatements = oracleGenerator.generate(diff);
        assertTrue(
                oracleStatements.stream().anyMatch(s -> s.contains("CREATE UNIQUE INDEX")),
                "Oracle should generate CREATE INDEX");
    }

    /** Test that unsupported dialect throws appropriate exception. */
    @Test
    public void testUnsupportedDialectThrowsException() throws GenerationException {
        GenerationException exception =
                assertThrows(
                        GenerationException.class,
                        () -> {
                            factory.createGenerator(null, false);
                        });

        assertTrue(
                exception.getMessage().contains("Unsupported database dialect"),
                "Should throw GenerationException for unsupported dialect");
    }

    /** Test that factory provides correct dialect information. */
    @Test
    public void testFactoryDialectInformation() {
        assertTrue(
                factory.isDialectSupported(DatabaseDialect.MYSQL), "Factory should support MySQL");
        assertTrue(
                factory.isDialectSupported(DatabaseDialect.POSTGRESQL),
                "Factory should support PostgreSQL");
        assertTrue(
                factory.isDialectSupported(DatabaseDialect.ORACLE),
                "Factory should support Oracle");

        String supportedDialects = factory.getSupportedDialects();
        assertTrue(supportedDialects.contains("MYSQL"), "Should list MySQL");
        assertTrue(supportedDialects.contains("POSTGRESQL"), "Should list PostgreSQL");
        assertTrue(supportedDialects.contains("ORACLE"), "Should list Oracle");

        assertEquals(
                3, factory.getRegistrationCount(), "Factory should have 3 generators registered");
    }

    /** Helper method to check if a column exists in MySQL DESCRIBE output. */
    private boolean columnExists(ResultSet rs, String columnName) throws Exception {
        while (rs.next()) {
            if (columnName.equals(rs.getString("Field"))) {
                return true;
            }
        }
        return false;
    }

    /** Helper method to check if a column exists in PostgreSQL query result. */
    private boolean hasColumn(ResultSet rs, String columnName) throws Exception {
        while (rs.next()) {
            if (columnName.equals(rs.getString("column_name"))) {
                return true;
            }
        }
        return false;
    }

    /** Helper method to clean up database tables. */
    private void cleanupDatabase(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // Drop all tables
            ResultSet rs =
                    stmt.executeQuery(
                            "SELECT table_name FROM information_schema.tables "
                                    + "WHERE table_schema = DATABASE()");
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                if (!tableName.equals("information_schema") && !tableName.equals("mysql")) {
                    stmt.execute("DROP TABLE IF EXISTS " + tableName);
                }
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /** Checks if Docker is available for Testcontainers. */
    private static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
