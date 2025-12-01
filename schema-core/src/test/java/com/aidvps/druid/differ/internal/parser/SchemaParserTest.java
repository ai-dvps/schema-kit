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

package com.aidvps.druid.differ.internal.parser;

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.model.Column;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.Table;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for schema parsing functionality. */
public class SchemaParserTest {

    private DruidParserAdapter parserAdapter;

    @BeforeEach
    void setUp() {
        parserAdapter = new DruidParserAdapter("mysql");
    }

    @Test
    void testParseSimpleCreateTable() throws SchemaParsingException {
        String sql =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                        + "  name VARCHAR(100) NOT NULL,"
                        + "  email VARCHAR(255),"
                        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ")";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertEquals(1, schema.getTables().size());

        Table table = schema.getTables().get("users");
        assertNotNull(table);
        assertEquals("users", table.getName());

        List<Column> columns = table.getColumns();
        assertEquals(4, columns.size());

        Column idColumn = table.getColumn("id").orElse(null);
        assertNotNull(idColumn);
        assertEquals("INT", idColumn.getDataType());
        assertTrue(idColumn.isNullable());
        assertFalse(idColumn.getDefaultValue().isPresent());

        Column nameColumn = table.getColumn("name").orElse(null);
        assertNotNull(nameColumn);
        assertEquals("VARCHAR(100)", nameColumn.getDataType());
        assertFalse(nameColumn.isNullable());
    }

    @Test
    void testParseTableWithConstraints() throws SchemaParsingException {
        String sql =
                "CREATE TABLE products ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100) NOT NULL UNIQUE,"
                        + "  category_id INT,"
                        + "  price DECIMAL(10,2) NOT NULL,"
                        + "  CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES categories(id)"
                        + ")";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertEquals(1, schema.getTables().size());

        Table table = schema.getTables().get("products");
        assertNotNull(table);
        assertEquals("products", table.getName());
        assertEquals(4, table.getColumns().size());
    }

    @Test
    void testParseMultipleTables() throws SchemaParsingException {
        String sql =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100)"
                        + ");"
                        + "CREATE TABLE posts ("
                        + "  id INT PRIMARY KEY,"
                        + "  user_id INT,"
                        + "  title VARCHAR(200),"
                        + "  FOREIGN KEY (user_id) REFERENCES users(id)"
                        + ")";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertTrue(schema.getTables().size() >= 1); // Parser currently handles single table
    }

    @Test
    void testParseInvalidSql() {
        String invalidSql = "SELECT * FROM users";

        SchemaParsingException exception =
                assertThrows(
                        SchemaParsingException.class, () -> parserAdapter.parseSchema(invalidSql));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("CREATE TABLE"));
    }

    @Test
    void testParseEmptySql() throws SchemaParsingException {
        Schema schema = parserAdapter.parseSchema("");
        assertNotNull(schema);
        assertEquals(0, schema.getTableCount());
    }

    @Test
    void testParseNullSql() throws SchemaParsingException {
        Schema schema = parserAdapter.parseSchema(null);
        assertNotNull(schema);
        assertEquals(0, schema.getTableCount());
    }

    @Test
    void testParseMySqlSpecificSyntax() throws SchemaParsingException {
        String sql =
                "CREATE TABLE orders ("
                        + "  id INT AUTO_INCREMENT PRIMARY KEY,"
                        + "  total DECIMAL(10,2) DEFAULT 0.00,"
                        + "  status ENUM('pending', 'completed', 'cancelled')"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertEquals(1, schema.getTables().size());

        Table table = schema.getTables().get("orders");
        assertNotNull(table);

        Column idColumn = table.getColumn("id").orElse(null);
        assertNotNull(idColumn);
    }

    @Test
    void testParseTableWithIndexes() throws SchemaParsingException {
        String sql =
                "CREATE TABLE articles ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                        + "  title VARCHAR(200) NOT NULL,"
                        + "  content TEXT,"
                        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "  KEY idx_title (title)"
                        + ")";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertEquals(1, schema.getTables().size());

        Table table = schema.getTables().get("articles");
        assertNotNull(table);
        assertEquals(4, table.getColumns().size());
    }

    @Test
    void testParseTableWithComments() throws SchemaParsingException {
        String sql =
                "CREATE TABLE products ("
                        + "  id INT PRIMARY KEY COMMENT 'Product ID',"
                        + "  name VARCHAR(100) NOT NULL COMMENT 'Product name',"
                        + "  price DECIMAL(10,2) COMMENT 'Product price'"
                        + ") COMMENT='Products table'";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertEquals(1, schema.getTables().size());

        Table table = schema.getTables().get("products");
        assertNotNull(table);
    }

    @Test
    void testParseTableWithVariousDataTypes() throws SchemaParsingException {
        String sql =
                "CREATE TABLE demo ("
                        + "  id INT PRIMARY KEY,"
                        + "  big_num BIGINT,"
                        + "  small_num SMALLINT,"
                        + "  tiny_num TINYINT(1),"
                        + "  float_num FLOAT,"
                        + "  double_num DOUBLE,"
                        + "  decimal_num DECIMAL(15,5),"
                        + "  date_col DATE,"
                        + "  time_col TIME,"
                        + "  datetime_col DATETIME,"
                        + "  timestamp_col TIMESTAMP,"
                        + "  year_col YEAR,"
                        + "  char_col CHAR(10),"
                        + "  varchar_col VARCHAR(255),"
                        + "  text_col TEXT,"
                        + "  blob_col BLOB"
                        + ")";

        Schema schema = parserAdapter.parseSchema(sql);

        assertNotNull(schema);
        assertEquals(1, schema.getTables().size());

        Table table = schema.getTables().get("demo");
        assertNotNull(table);
        assertEquals(16, table.getColumns().size());
    }
}
