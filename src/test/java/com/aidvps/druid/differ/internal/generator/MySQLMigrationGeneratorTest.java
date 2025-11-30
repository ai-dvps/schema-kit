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

import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.comparator.ChangeDetector;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for MySQL migration SQL generation. */
public class MySQLMigrationGeneratorTest {

    private DruidParserAdapter parserAdapter;
    private ChangeDetector changeDetector;
    private MySQLMigrationGenerator generator;

    @BeforeEach
    void setUp() {
        parserAdapter = new DruidParserAdapter("mysql");
        changeDetector = new ChangeDetector();
        generator = new MySQLMigrationGenerator(true);
    }

    @Test
    void testGenerateAlterTableAddColumn() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("ADD COLUMN"));
        assertTrue(sql.contains("email"));
    }

    @Test
    void testGenerateAlterTableDropColumn() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("DROP COLUMN"));
        assertTrue(sql.contains("email"));
    }

    @Test
    void testGenerateAlterTableModifyColumn() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(200)" + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("MODIFY COLUMN") || sql.contains("MODIFY"));
        assertTrue(sql.contains("name"));
    }

    @Test
    void testGenerateCreateTable() throws SchemaParsingException {
        String sourceSchema = "";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                        + "  name VARCHAR(100) NOT NULL,"
                        + "  email VARCHAR(255)"
                        + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("CREATE TABLE"));
        assertTrue(sql.contains("users"));
        assertTrue(sql.contains("id"));
        assertTrue(sql.contains("name"));
    }

    @Test
    void testGenerateDropTable() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema = "";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("DROP TABLE"));
        assertTrue(sql.contains("users"));
    }

    @Test
    void testGenerateMultipleStatements() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(200),"
                        + "  email VARCHAR(255),"
                        + "  phone VARCHAR(20)"
                        + ");"
                        + "CREATE TABLE posts ("
                        + "  id INT PRIMARY KEY,"
                        + "  user_id INT,"
                        + "  title VARCHAR(200)"
                        + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertTrue(statements.size() >= 3);

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("CREATE TABLE"));
    }

    @Test
    void testGenerateWithComments() throws SchemaParsingException {
        generator = new MySQLMigrationGenerator(true);

        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("--") || sql.contains("/*"));
    }

    @Test
    void testGenerateWithoutComments() throws SchemaParsingException {
        generator = new MySQLMigrationGenerator(false);

        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        // Comments should not be present
        assertFalse(sql.contains("--") && !sql.trim().startsWith("--"));
    }

    @Test
    void testGenerateMySqlSpecificSyntax() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE orders ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                        + "  total DECIMAL(10,2) DEFAULT 0.00"
                        + ")";

        String targetSchema =
                "CREATE TABLE orders ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                        + "  total DECIMAL(10,2) DEFAULT 0.00,"
                        + "  status ENUM('pending', 'completed', 'cancelled')"
                        + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("ADD COLUMN"));
    }

    @Test
    void testGenerateForIdenticalSchemas() throws SchemaParsingException {
        String schema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        SchemaDiff diff = createDiff(schema, schema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertTrue(statements.isEmpty(), "No statements should be generated for identical schemas");
    }

    @Test
    void testGenerateWithPrimaryKeyChange() throws SchemaParsingException {
        String sourceSchema = "CREATE TABLE users (" + "  id INT," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        // Should generate statement for primary key change
        assertFalse(statements.isEmpty());
    }

    @Test
    void testGenerateAlterColumnNullability() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100) NOT NULL"
                        + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        SchemaDiff diff = createDiff(sourceSchema, targetSchema);

        List<String> statements = generator.generate(diff);

        assertNotNull(statements);
        assertFalse(statements.isEmpty());

        String sql = String.join("\n", statements);
        assertTrue(sql.contains("ALTER TABLE"));
    }

    private SchemaDiff createDiff(String sourceSchema, String targetSchema)
            throws SchemaParsingException {
        com.aidvps.druid.differ.internal.model.Schema source =
                parserAdapter.parseSchema(sourceSchema);
        com.aidvps.druid.differ.internal.model.Schema target =
                parserAdapter.parseSchema(targetSchema);
        return changeDetector.compare(source, target);
    }
}
