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

package com.aidvps.druid.differ.internal.comparator;

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for schema comparison functionality. */
public class SchemaComparatorTest {

    private DruidParserAdapter parserAdapter;
    private ChangeDetector changeDetector;

    @BeforeEach
    void setUp() {
        parserAdapter = new DruidParserAdapter("mysql");
        changeDetector = new ChangeDetector();
    }

    @Test
    void testDetectAddedTables() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100)"
                        + ");"
                        + "CREATE TABLE posts ("
                        + "  id INT PRIMARY KEY,"
                        + "  title VARCHAR(200)"
                        + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getAddedTables().size());
        assertTrue(diff.getAddedTables().containsKey("posts"));
        assertEquals(0, diff.getRemovedTables().size());
        assertEquals(0, diff.getModifiedTables().size());
    }

    @Test
    void testDetectRemovedTables() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100)"
                        + ");"
                        + "CREATE TABLE posts ("
                        + "  id INT PRIMARY KEY,"
                        + "  title VARCHAR(200)"
                        + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(0, diff.getAddedTables().size());
        assertEquals(1, diff.getRemovedTables().size());
        assertTrue(diff.getRemovedTables().containsKey("posts"));
    }

    @Test
    void testDetectAddedColumns() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getModifiedTables().size());

        com.aidvps.druid.differ.internal.model.TableDiff tableDiff =
                diff.getModifiedTables().get("users");
        assertNotNull(tableDiff);
        assertEquals(1, tableDiff.getAddedColumns().size());
        assertEquals("email", tableDiff.getAddedColumns().get(0).getName());
    }

    @Test
    void testDetectRemovedColumns() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100),"
                        + "  email VARCHAR(255)"
                        + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getModifiedTables().size());

        com.aidvps.druid.differ.internal.model.TableDiff tableDiff =
                diff.getModifiedTables().get("users");
        assertNotNull(tableDiff);
        assertEquals(1, tableDiff.getRemovedColumns().size());
        assertEquals("email", tableDiff.getRemovedColumns().get(0));
    }

    @Test
    void testDetectModifiedColumnType() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(200)" + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getModifiedTables().size());

        com.aidvps.druid.differ.internal.model.TableDiff tableDiff =
                diff.getModifiedTables().get("users");
        assertNotNull(tableDiff);
        assertEquals(1, tableDiff.getModifiedColumns().size());

        com.aidvps.druid.differ.internal.model.ColumnDiff columnDiff =
                tableDiff.getModifiedColumns().get("name");
        assertNotNull(columnDiff);
        assertTrue(
                columnDiff.hasChange(
                        com.aidvps.druid.differ.internal.model.ColumnDiff.ChangeType
                                .DATA_TYPE_CHANGED));
    }

    @Test
    void testDetectModifiedColumnNullability() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  name VARCHAR(100) NOT NULL"
                        + ")";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getModifiedTables().size());

        com.aidvps.druid.differ.internal.model.TableDiff tableDiff =
                diff.getModifiedTables().get("users");
        assertNotNull(tableDiff);
        assertEquals(1, tableDiff.getModifiedColumns().size());

        com.aidvps.druid.differ.internal.model.ColumnDiff columnDiff =
                tableDiff.getModifiedColumns().get("name");
        assertNotNull(columnDiff);
        assertTrue(
                columnDiff.hasChange(
                        com.aidvps.druid.differ.internal.model.ColumnDiff.ChangeType
                                .NULLABILITY_CHANGED));
    }

    @Test
    void testDetectModifiedDefaultValue() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  status VARCHAR(20) DEFAULT 'active'"
                        + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id INT PRIMARY KEY,"
                        + "  status VARCHAR(20) DEFAULT 'pending'"
                        + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getModifiedTables().size());

        com.aidvps.druid.differ.internal.model.TableDiff tableDiff =
                diff.getModifiedTables().get("users");
        assertNotNull(tableDiff);
        assertEquals(1, tableDiff.getModifiedColumns().size());

        com.aidvps.druid.differ.internal.model.ColumnDiff columnDiff =
                tableDiff.getModifiedColumns().get("status");
        assertNotNull(columnDiff);
        assertTrue(
                columnDiff.hasChange(
                        com.aidvps.druid.differ.internal.model.ColumnDiff.ChangeType
                                .DEFAULT_VALUE_CHANGED));
    }

    @Test
    void testCompareIdenticalSchemas() throws SchemaParsingException {
        String schema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        Schema source = parserAdapter.parseSchema(schema);
        Schema target = parserAdapter.parseSchema(schema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertTrue(diff.isEmpty());
        assertEquals(0, diff.getAddedTables().size());
        assertEquals(0, diff.getRemovedTables().size());
        assertEquals(0, diff.getModifiedTables().size());
    }

    @Test
    void testCompareIgnoringWhitespace() throws SchemaParsingException {
        String sourceSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        String targetSchema =
                "CREATE TABLE users ("
                        + "  id    INT   PRIMARY   KEY,"
                        + "  name VARCHAR(100)"
                        + ")";

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertTrue(diff.isEmpty(), "Whitespace differences should be ignored");
    }

    @Test
    void testDetectMultipleChanges() throws SchemaParsingException {
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

        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        SchemaDiff diff = changeDetector.compare(source, target);

        assertNotNull(diff);
        assertEquals(1, diff.getModifiedTables().size());

        com.aidvps.druid.differ.internal.model.TableDiff tableDiff =
                diff.getModifiedTables().get("users");
        assertNotNull(tableDiff);

        // Name type changed
        assertTrue(tableDiff.getModifiedColumns().containsKey("name"));

        // Email removed
        assertTrue(tableDiff.getRemovedColumns().contains("email"));

        // Phone and address added
        assertTrue(
                tableDiff.getAddedColumns().stream()
                        .anyMatch(col -> col.getName().equals("phone")));
        assertTrue(
                tableDiff.getAddedColumns().stream()
                        .anyMatch(col -> col.getName().equals("address")));
    }

    @Test
    void testCompareEmptyToSchema() throws SchemaParsingException {
        String sourceSchema = "";

        String targetSchema =
                "CREATE TABLE users (" + "  id INT PRIMARY KEY," + "  name VARCHAR(100)" + ")";

        // Empty schema should now be handled gracefully
        Schema source = parserAdapter.parseSchema(sourceSchema);
        assertNotNull(source);
        assertEquals(0, source.getTableCount());
    }
}
