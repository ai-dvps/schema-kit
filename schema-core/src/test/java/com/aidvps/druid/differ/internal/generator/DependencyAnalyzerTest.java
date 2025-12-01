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

import com.aidvps.druid.differ.exception.GenerationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for DependencyAnalyzer to ensure proper ordering of migration statements. */
public class DependencyAnalyzerTest {

    private final DependencyAnalyzer analyzer = new DependencyAnalyzer();

    @Test
    public void testCorrectOrderingOfSimpleOperations() throws GenerationException {
        // Create simple operations with no dependencies
        MigrationStatement createTable =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "users",
                        "CREATE TABLE users (id INT PRIMARY KEY)",
                        1);

        MigrationStatement addColumn =
                createStatement(
                        MigrationStatement.Type.ADD_COLUMN,
                        "users",
                        "ALTER TABLE users ADD COLUMN name VARCHAR(100)",
                        2);

        MigrationStatement addConstraint =
                createStatement(
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "users",
                        "ALTER TABLE users ADD CONSTRAINT fk_users_ref FOREIGN KEY (user_id) REFERENCES other_table(id)",
                        3);

        List<MigrationStatement> statements = Arrays.asList(createTable, addColumn, addConstraint);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(3, ordered.size(), "Should return all statements");
        // Verify CREATE TABLE comes first
        assertEquals(MigrationStatement.Type.CREATE_TABLE, ordered.get(0).getType());
    }

    @Test
    public void testOrderingWithForeignKeyDependencies() throws GenerationException {
        // Table creation should come before foreign key addition
        MigrationStatement createParentTable =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "users",
                        "CREATE TABLE users (id INT PRIMARY KEY)",
                        1);

        MigrationStatement createChildTable =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "orders",
                        "CREATE TABLE orders (id INT PRIMARY KEY, user_id INT)",
                        1);

        MigrationStatement addForeignKey =
                createStatement(
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "orders",
                        "ALTER TABLE orders ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)",
                        3);

        List<MigrationStatement> statements =
                Arrays.asList(addForeignKey, createChildTable, createParentTable);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(3, ordered.size(), "Should return all statements");

        // Find indices of each statement type
        int parentIndex =
                findStatementIndex(ordered, "users", MigrationStatement.Type.CREATE_TABLE);
        int childIndex =
                findStatementIndex(ordered, "orders", MigrationStatement.Type.CREATE_TABLE);
        int fkIndex = findStatementIndex(ordered, "orders", MigrationStatement.Type.ADD_CONSTRAINT);

        // Parent table should be created before foreign key
        assertTrue(parentIndex < fkIndex, "Parent table should be created before foreign key");

        // Child table should be created before foreign key
        assertTrue(childIndex < fkIndex, "Child table should be created before foreign key");
    }

    @Test
    public void testOrderingWithColumnToConstraintDependency() throws GenerationException {
        // Column addition should come before constraint addition
        MigrationStatement addColumn =
                createStatement(
                        MigrationStatement.Type.ADD_COLUMN,
                        "users",
                        "ALTER TABLE users ADD COLUMN email VARCHAR(100)",
                        2);

        MigrationStatement addUniqueConstraint =
                createStatement(
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "users",
                        "ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email)",
                        3);

        List<MigrationStatement> statements = Arrays.asList(addUniqueConstraint, addColumn);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(2, ordered.size(), "Should return all statements");

        // ADD_COLUMN should come before ADD_CONSTRAINT
        assertEquals(MigrationStatement.Type.ADD_COLUMN, ordered.get(0).getType());
        assertEquals(MigrationStatement.Type.ADD_CONSTRAINT, ordered.get(1).getType());
    }

    @Test
    public void testOrderingWithDropDependencies() throws GenerationException {
        // Index drop should come before table drop
        MigrationStatement dropIndex =
                createStatement(
                        MigrationStatement.Type.DROP_INDEX,
                        "users",
                        "ALTER TABLE users DROP INDEX idx_users_email",
                        4);

        MigrationStatement dropConstraint =
                createStatement(
                        MigrationStatement.Type.DROP_CONSTRAINT,
                        "users",
                        "ALTER TABLE users DROP CONSTRAINT fk_users_ref",
                        4);

        MigrationStatement dropTable =
                createStatement(MigrationStatement.Type.DROP_TABLE, "users", "DROP TABLE users", 5);

        List<MigrationStatement> statements = Arrays.asList(dropTable, dropConstraint, dropIndex);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(3, ordered.size(), "Should return all statements");

        // DROP_TABLE should be last
        assertEquals(MigrationStatement.Type.DROP_TABLE, ordered.get(2).getType());
    }

    @Test
    public void testComplexDependencyGraphOrdering() throws GenerationException {
        // Test with realistic dependency chain: table -> column -> constraint
        MigrationStatement createUsers =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "users",
                        "CREATE TABLE users (id INT PRIMARY KEY)",
                        1);

        MigrationStatement addEmailColumn =
                createStatementWithDependency(
                        MigrationStatement.Type.ADD_COLUMN,
                        "users",
                        "ALTER TABLE users ADD COLUMN email VARCHAR(100)",
                        2,
                        "users" // depends on users table creation
                        );

        MigrationStatement addUniqueConstraint =
                createStatementWithDependency(
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "users",
                        "ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email)",
                        3,
                        "users" // depends on users table and email column
                        );

        List<MigrationStatement> statements =
                Arrays.asList(addUniqueConstraint, addEmailColumn, createUsers);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(3, ordered.size(), "Should return all statements");

        // Verify correct order: CREATE -> ADD COLUMN -> ADD CONSTRAINT
        assertEquals(MigrationStatement.Type.CREATE_TABLE, ordered.get(0).getType());
        assertEquals("users", ordered.get(0).getTableName());

        assertEquals(MigrationStatement.Type.ADD_COLUMN, ordered.get(1).getType());
        assertEquals("users", ordered.get(1).getTableName());

        assertEquals(MigrationStatement.Type.ADD_CONSTRAINT, ordered.get(2).getType());
        assertEquals("users", ordered.get(2).getTableName());
    }

    @Test
    public void testComplexDependencyGraph() throws GenerationException {
        // Create a more complex dependency graph
        MigrationStatement createUsers =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "users",
                        "CREATE TABLE users (id INT PRIMARY KEY)",
                        1);

        MigrationStatement createProducts =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "products",
                        "CREATE TABLE products (id INT PRIMARY KEY)",
                        1);

        MigrationStatement createOrders =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "orders",
                        "CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, product_id INT)",
                        1);

        MigrationStatement addUserEmailColumn =
                createStatement(
                        MigrationStatement.Type.ADD_COLUMN,
                        "users",
                        "ALTER TABLE users ADD COLUMN email VARCHAR(100)",
                        2);

        MigrationStatement addOrderUserFk =
                createStatement(
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "orders",
                        "ALTER TABLE orders ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)",
                        3);

        MigrationStatement addOrderProductFk =
                createStatement(
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "orders",
                        "ALTER TABLE orders ADD CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products(id)",
                        3);

        List<MigrationStatement> statements =
                Arrays.asList(
                        addOrderUserFk,
                        createOrders,
                        createUsers,
                        addUserEmailColumn,
                        addOrderProductFk,
                        createProducts);

        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(6, ordered.size(), "Should return all statements");

        // Verify CREATE TABLE statements come before ADD_CONSTRAINT
        int usersCreateIdx =
                findStatementIndex(ordered, "users", MigrationStatement.Type.CREATE_TABLE);
        int productsCreateIdx =
                findStatementIndex(ordered, "products", MigrationStatement.Type.CREATE_TABLE);
        int ordersCreateIdx =
                findStatementIndex(ordered, "orders", MigrationStatement.Type.CREATE_TABLE);

        int userFkIdx =
                findStatementIndex(
                        ordered,
                        "orders",
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "fk_orders_user");
        int productFkIdx =
                findStatementIndex(
                        ordered,
                        "orders",
                        MigrationStatement.Type.ADD_CONSTRAINT,
                        "fk_orders_product");

        assertTrue(usersCreateIdx < userFkIdx, "Users table should be created before user FK");
        assertTrue(
                productsCreateIdx < productFkIdx,
                "Products table should be created before product FK");
        assertTrue(ordersCreateIdx < userFkIdx, "Orders table should be created before FKs");
        assertTrue(ordersCreateIdx < productFkIdx, "Orders table should be created before FKs");
    }

    @Test
    public void testEmptyStatementList() throws GenerationException {
        List<MigrationStatement> statements = new ArrayList<>();
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(0, ordered.size(), "Should return empty list for empty input");
    }

    @Test
    public void testSingleStatement() throws GenerationException {
        MigrationStatement stmt =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "users",
                        "CREATE TABLE users (id INT PRIMARY KEY)",
                        1);

        List<MigrationStatement> statements = Arrays.asList(stmt);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(1, ordered.size(), "Should return single statement");
        assertEquals(stmt, ordered.get(0), "Should return the same statement");
    }

    @Test
    public void testStatementsWithNoDependencies() throws GenerationException {
        MigrationStatement stmt1 =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "table1",
                        "CREATE TABLE table1 (id INT PRIMARY KEY)",
                        1);

        MigrationStatement stmt2 =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "table2",
                        "CREATE TABLE table2 (id INT PRIMARY KEY)",
                        1);

        MigrationStatement stmt3 =
                createStatement(
                        MigrationStatement.Type.CREATE_TABLE,
                        "table3",
                        "CREATE TABLE table3 (id INT PRIMARY KEY)",
                        1);

        List<MigrationStatement> statements = Arrays.asList(stmt3, stmt1, stmt2);
        List<MigrationStatement> ordered = analyzer.orderStatements(statements);

        assertEquals(3, ordered.size(), "Should return all statements");
        // Should maintain some order (no strict requirements when no dependencies)
        assertNotNull(ordered.get(0), "Should have first statement");
        assertNotNull(ordered.get(1), "Should have second statement");
        assertNotNull(ordered.get(2), "Should have third statement");
    }

    /** Helper method to create a MigrationStatement. */
    private MigrationStatement createStatement(
            MigrationStatement.Type type, String tableName, String sql, int priority) {
        return new MigrationStatement.Builder()
                .type(type)
                .tableName(tableName)
                .sql(sql)
                .priority(priority)
                .build();
    }

    /** Helper method to create a MigrationStatement with a dependency. */
    private MigrationStatement createStatementWithDependency(
            MigrationStatement.Type type,
            String tableName,
            String sql,
            int priority,
            String dependsOn) {
        return new MigrationStatement.Builder()
                .type(type)
                .tableName(tableName)
                .sql(sql)
                .priority(priority)
                .addDependency(dependsOn)
                .build();
    }

    /** Helper method to find the index of a statement by type and table name. */
    private int findStatementIndex(
            List<MigrationStatement> statements, String tableName, MigrationStatement.Type type) {
        for (int i = 0; i < statements.size(); i++) {
            MigrationStatement stmt = statements.get(i);
            if (stmt.getTableName().equals(tableName) && stmt.getType() == type) {
                return i;
            }
        }
        return -1;
    }

    /** Helper method to find the index of a statement by type, table name, and SQL content. */
    private int findStatementIndex(
            List<MigrationStatement> statements,
            String tableName,
            MigrationStatement.Type type,
            String sqlContains) {
        for (int i = 0; i < statements.size(); i++) {
            MigrationStatement stmt = statements.get(i);
            if (stmt.getTableName().equals(tableName)
                    && stmt.getType() == type
                    && stmt.getSql().contains(sqlContains)) {
                return i;
            }
        }
        return -1;
    }
}
