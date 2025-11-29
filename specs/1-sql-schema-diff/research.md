# Research: SQL Table Differ Implementation

## Research Summary

This document captures findings from research into druid-parser integration, database dialect differences, and schema comparison algorithms for the SQL Table Differ component.

---

## 0. Build System and Parser Version

### Build Tool: Gradle
**Decision**: Use Gradle instead of Maven
**Rationale**: The project uses Gradle as its build system. All build configurations, dependencies, and tasks should be defined in `build.gradle`.

**Gradle Configuration Notes**:
- Java 8 compatibility (sourceCompatibility and targetCompatibility set to VERSION_1_8)
- Separate test configurations for unit tests, integration tests, and JMH benchmarks
- Testcontainers support for integration testing
- **Repository Configuration**: `mavenLocal()` must be included for SNAPSHOT dependencies

### Parser Version: com.aidvps:druid-parser:1.2.28-SNAPSHOT
**Version**: 1.2.28-SNAPSHOT
**Source**: /Users/shunyun/workspace/java/druid/core
**Integration**: Add as implementation dependency in Gradle

**Repository Configuration**:
```gradle
repositories {
    mavenLocal()  // Essential for SNAPSHOT dependencies
    mavenCentral()
}
```

**Compatibility Notes**:
- Verify API stability across snapshot versions
- Check for breaking changes in AST classes
- Monitor for new features in snapshot releases
- **Important**: `mavenLocal()` must be configured before `mavenCentral()` to access SNAPSHOT versions

---

## 1. Druid-Parser AST Structure and Integration Patterns

### Decision: Leverage druid-parser's SQLCreateTableStatement AST
**Rationale**: Druid-parser provides comprehensive AST classes for SQL statements, eliminating the need for custom parsing logic.

### Key AST Classes Identified

**SQLCreateTableStatement**
- Primary entry point for CREATE TABLE parsing
- Contains tableName, tableElementList, dbType properties
- Supports parsing all table elements including columns, constraints, indexes
- Location: `com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement`

**SQLColumnDefinition**
- Represents individual column definitions
- Contains name, dataType, defaultExpr, constraints properties
- Supports column-level constraints: PRIMARY KEY, UNIQUE, NOT NULL, CHECK, FOREIGN KEY
- Special properties: autoIncrement, onUpdate (MySQL), comment
- Location: `com.alibaba.druid.sql.ast.statement.SQLColumnDefinition`

**SQLConstraint**
- Base class for all constraints
- Subtypes: SQLPrimaryKey, SQLUniqueKey, SQLForeignKey, SQLCheck
- Represents both column-level and table-level constraints
- Location: `com.alibaba.druid.sql.ast.statement.SQLConstraint`

**SQLTableElement**
- Interface implemented by all table elements (columns, constraints, indexes)
- Polymorphic handling enables generic element processing
- Location: `com.alibaba.druid.sql.ast.statement.SQLTableElement`

### Integration Pattern

```java
// Parse CREATE TABLE statement
SQLCreateTableParser parser = new SQLCreateTableParser(sql);
SQLCreateTableStatement statement = parser.parseCreateTable();

// Extract table name
SQLName tableName = statement.getName();

// Extract all table elements
List<SQLTableElement> elements = statement.getTableElementList();

// Iterate through elements
for (SQLTableElement element : elements) {
    if (element instanceof SQLColumnDefinition) {
        SQLColumnDefinition column = (SQLColumnDefinition) element;
        // Process column
    } else if (element instanceof SQLConstraint) {
        SQLConstraint constraint = (SQLConstraint) element;
        // Process constraint
    }
}
```

### Database Dialect Support

**Supported Dialects**: MySQL, PostgreSQL, Oracle, SQL Server, H2, and 20+ others
**Configuration**: Pass DbType enum to parser constructor
**Extensibility**: Custom dialect-specific parsing via subclassing

---

## 2. Database-Specific SQL Dialect Differences

### Decision: Implement strategy pattern for database-specific SQL generation
**Rationale**: Different databases require different ALTER TABLE syntax, constraint handling, and data type conversions.

### MySQL-Specific Considerations

**ALTER TABLE Syntax**
```sql
-- Add column
ALTER TABLE table_name ADD COLUMN column_name VARCHAR(255);

-- Drop column
ALTER TABLE table_name DROP COLUMN column_name;

-- Modify column (type change)
ALTER TABLE table_name MODIFY COLUMN column_name TEXT;

-- Add constraint
ALTER TABLE table_name ADD PRIMARY KEY (column_name);

-- Drop constraint
ALTER TABLE table_name DROP PRIMARY KEY;
```

**Characteristics**:
- MODIFY COLUMN for type changes (not ALTER COLUMN)
- First column in PRIMARY KEY automatically NOT NULL
- AUTO_INCREMENT syntax (not AUTOINCREMENT or GENERATED)
- ENGINE clause for storage engine specification
- Supports IF NOT EXISTS for CREATE TABLE

**Column Type Mappings**:
- VARCHAR(255) → TEXT (for large VARCHAR)
- INT → BIGINT (for auto-increment in some cases)
- TIMESTAMP vs DATETIME behavior differences

### PostgreSQL-Specific Considerations

**ALTER TABLE Syntax**
```sql
-- Add column
ALTER TABLE table_name ADD COLUMN column_name VARCHAR(255);

-- Drop column
ALTER TABLE table_name DROP COLUMN column_name;

-- Alter column (type change)
ALTER TABLE table_name ALTER COLUMN column_name TYPE TEXT;

-- Add constraint
ALTER TABLE table_name ADD PRIMARY KEY (column_name);

-- Drop constraint
ALTER TABLE table_name DROP CONSTRAINT constraint_name;
```

**Characteristics**:
- Requires explicit constraint names for dropping
- TYPE keyword for column type changes
- USING clause for complex type conversions
- SERIAL for auto-increment (not AUTO_INCREMENT)
- Supports INHERITS for table inheritance

**Column Type Mappings**:
- VARCHAR without length → TEXT
- AUTO_INCREMENT → SERIAL or GENERATED BY DEFAULT AS IDENTITY
- BOOLEAN vs TINYINT(1) (MySQL → PostgreSQL)

### Oracle-Specific Considerations

**ALTER TABLE Syntax**
```sql
-- Add column
ALTER TABLE table_name ADD (column_name VARCHAR2(255));

-- Drop column
ALTER TABLE table_name DROP (column_name);

-- Modify column
ALTER TABLE table_name MODIFY (column_name CLOB);

-- Add constraint
ALTER TABLE table_name ADD (CONSTRAINT pk_name PRIMARY KEY (column_name));

-- Drop constraint
ALTER TABLE table_name DROP CONSTRAINT constraint_name;
```

**Characteristics**:
- Parentheses required for multiple column operations
- VARCHAR2 instead of VARCHAR
- NUMBER instead of INT/BIGINT
- Constraint names required for drops
- Supports CLOB for large text

**Column Type Mappings**:
- VARCHAR → VARCHAR2
- INT → NUMBER(10) or NUMBER(38)
- AUTO_INCREMENT via triggers/sequences

### Comparison Table

| Feature | MySQL | PostgreSQL | Oracle |
|---------|-------|------------|--------|
| Column Type Change | MODIFY COLUMN | ALTER COLUMN TYPE | MODIFY |
| Drop Column | DROP COLUMN | DROP COLUMN | DROP (parentheses) |
| Constraint Drop Name Required | No | Yes | Yes |
| Auto-increment | AUTO_INCREMENT | SERIAL/IDENTITY | Trigger/Sequence |
| String Type | VARCHAR | VARCHAR/TEXT | VARCHAR2 |
| Integer Type | INT/BIGINT | INT/BIGINT | NUMBER |
| Large Text | TEXT | TEXT | CLOB |
| Schema Qualified | database.table | schema.table | schema.table |

---

## 3. Schema Comparison Algorithm Design

### Decision: Implement diff algorithm using object equivalence with semantic awareness
**Rationale**: Must detect meaningful differences while ignoring formatting/whitespace variations.

### Comparison Strategy

**Phase 1: Quick Comparison**
```java
// Check if schemas are identical
if (sourceSchema.equals(targetSchema)) {
    return SchemaDiff.empty();
}
```

**Phase 2: Table-Level Diff**
```java
// Detect added/removed/modified tables
Set<String> sourceTables = sourceSchema.getTableNames();
Set<String> targetTables = targetSchema.getTableNames();

Set<String> addedTables = targetTables - sourceTables;
Set<String> removedTables = sourceTables - targetTables;
Set<String> potentialModifiedTables = targetTables ∩ sourceTables;
```

**Phase 3: Column-Level Diff**
```java
// For each modified table, compare columns
for (String tableName : potentialModifiedTables) {
    Table sourceTable = sourceSchema.getTable(tableName);
    Table targetTable = targetSchema.getTable(tableName);

    Set<String> sourceColumns = sourceTable.getColumnNames();
    Set<String> targetColumns = targetTable.getColumnNames();

    // Detect added/removed columns
    // Compare column properties (type, nullability, default, constraints)
}
```

### Semantic Difference Detection

**Column Comparison Criteria**:
1. Name (case-insensitive)
2. Data type (type compatibility check)
3. Nullability (NULL vs NOT NULL)
4. Default value (semantic equivalence)
5. Auto-increment flag
6. Column-level constraints
7. Character set/collation (if applicable)

**Constraint Comparison**:
1. Primary key changes (columns, clustered vs non-clustered)
2. Foreign key changes (referenced table, columns, on delete/update actions)
3. Unique constraint changes (columns, deferred state)
4. Check constraint changes (expression)
5. Index changes (type, columns, unique vs non-unique)

### Dependency Analysis

**Topological Sort for Statement Ordering**

```
Algorithm:
1. Build dependency graph:
   - DROP statements depend on (must come after) nothing
   - CREATE statements depend on nothing
   - ALTER statements depend on their table's CREATE
   - ALTER ADD CONSTRAINT depends on target table/column existence

2. Perform topological sort to determine execution order

3. Detect cycles in dependency graph (error case)

4. Apply constraints:
   - Always drop foreign keys before dropping referenced tables
   - Always drop indexes before dropping tables
   - Always add tables before adding foreign keys to them
   - Always alter columns before adding constraints that reference them
```

**Example Dependency Graph**:
```
CREATE TABLE parent (id INT PRIMARY KEY)

CREATE TABLE child (
    id INT PRIMARY KEY,
    parent_id INT,
    FOREIGN KEY (parent_id) REFERENCES parent(id)
)

-- Required ordering:
-- 1. DROP TABLE child (if exists)
-- 2. DROP TABLE parent (if exists)
-- 3. CREATE TABLE parent
-- 4. CREATE TABLE child
-- 5. ALTER TABLE child ADD CONSTRAINT fk_child_parent
```

---

## 4. Migration Generation Patterns

### Decision: Implement abstract factory pattern for database-specific generators
**Rationale**: Encapsulates dialect-specific logic behind common interface, enabling easy extension to new databases.

### Generator Interface

```java
public interface MigrationGenerator {
    List<String> generateCreateTable(Table table);
    List<String> generateDropTable(Table table);
    List<String> generateAddColumn(Table table, Column column);
    List<String> generateDropColumn(Table table, String columnName);
    List<String> generateModifyColumn(Table table, Column oldColumn, Column newColumn);
    List<String> generateAddConstraint(Table table, Constraint constraint);
    List<String> generateDropConstraint(Table table, String constraintName);
}
```

### Statement Type Priority (Execution Order)

**High Priority (Execute First)**:
1. DROP statements (tables, constraints, indexes)
2. ALTER statements that remove dependencies

**Medium Priority (Execute Middle)**:
3. CREATE TABLE statements
4. ALTER statements that add columns
5. ALTER statements that modify column types

**Low Priority (Execute Last)**:
6. ALTER statements that add constraints (foreign keys, unique keys, check constraints)
7. CREATE INDEX statements
8. UPDATE statements (if data migration needed)

### Example Migration Output

**MySQL Migration**
```sql
-- Migration from schema_v1 to schema_v2
-- Generated: 2025-11-29

-- Drop obsolete columns
ALTER TABLE users DROP COLUMN temporary_field;
ALTER TABLE orders DROP COLUMN legacy_status;

-- Add new columns
ALTER TABLE users ADD COLUMN email VARCHAR(255) NOT NULL;
ALTER TABLE orders ADD COLUMN discount_code VARCHAR(50);

-- Modify column types
ALTER TABLE orders MODIFY COLUMN total_amount DECIMAL(10,2) NOT NULL;

-- Add constraints
ALTER TABLE orders ADD CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Add indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
```

---

## 5. Testing Strategy Research

### TestContainers Integration

**Decision**: Use Testcontainers for integration testing
**Rationale**: Provides real database instances for validation without mocking complexity.

**Supported Databases**:
- MySQL: `mysql:8.0` container
- PostgreSQL: `postgres:15` container
- Oracle: Requires licensing, use XE version or skip in CI

**Test Pattern**:
```java
@SpringBootTest
@Testcontainers
public class MigrationIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    public void testMySQLMigrationGeneration() {
        String sourceSchema = "CREATE TABLE users (id INT PRIMARY KEY)";
        String targetSchema = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255))";

        MigrationPlan plan = tableDiffer.generateMigration(sourceSchema, targetSchema);

        // Execute migration on test container
        mysql.createConnection("").createStatement()
             .execute(plan.getStatements().get(0));

        // Verify schema matches target
        ResultSet rs = mysql.createConnection("").createStatement()
                           .executeQuery("DESCRIBE users");
        // Verify column exists
    }
}
```

---

## 6. Performance Considerations

### Decision: Optimize for parsing and comparison performance
**Rationale**: Success criteria requires <5 seconds for 100 tables.

### Optimization Strategies

**1. Parsing Optimization**
- Cache parsed schemas to avoid re-parsing
- Use lazy loading for column/constraint details
- Parallel parsing for multi-table schemas (if CPU-bound)

**2. Comparison Optimization**
- Hash-based pre-check: compute hash of table/column for quick equality
- Only deep-compare tables that pass hash check
- Use structural sharing for unchanged schemas

**3. Generation Optimization**
- Pre-compute statement templates
- Batch similar operations (multiple ALTER TABLE statements)
- Minimize string concatenation (use StringBuilder with capacity)

**4. Memory Optimization**
- Use immutable models to enable structural sharing
- Avoid defensive copying where not necessary
- Clear caches for long-running applications

---

## Implementation Recommendations

### Immediate Actions (Phase 1)

1. **Create DruidParserAdapter** to wrap druid-parser API
   - Provide clean interface for schema parsing
   - Handle multiple CREATE TABLE statements in single input
   - Extract table metadata into internal model

2. **Define Internal Schema Model**
   - Schema, Table, Column, Constraint classes
   - Immutable with builder pattern for construction
   - Proper equals/hashCode for comparison

3. **Implement MySQL Generator First**
   - Simplest dialect (no constraint names required)
   - Validate approach before adding complexity
   - Use concrete MySQL examples for testing

### Medium-Term Actions (Phase 2)

1. **Add PostgreSQL Support**
   - Implement PostgreSQL-specific ALTER TABLE syntax
   - Handle SERIAL → IDENTITY conversions
   - Test with real PostgreSQL container

2. **Implement Dependency Analyzer**
   - Topological sort for statement ordering
   - Cycle detection and error reporting
   - Validation of generated SQL

3. **Add Comprehensive Testing**
   - Unit tests for all public methods
   - Integration tests with real databases
   - JMH benchmarks for performance

### Long-Term Actions (Phase 3)

1. **Add Oracle Support**
   - Handle VARCHAR2 and NUMBER types
   - Parentheses in ALTER TABLE statements
   - Constraint name requirements

2. **Performance Optimization**
   - Profiling with large schemas (100+ tables)
   - Memory usage optimization
   - GC pressure reduction

3. **Enhanced Features**
   - Transaction wrapping option
   - Destructive operation confirmation
   - Rollback SQL generation

---

## Key Decisions Summary

| Decision | Chosen Approach | Rationale |
|----------|----------------|-----------|
| SQL Parser | Druid-parser AST | Mature, comprehensive, supports multiple dialects |
| Comparison Algorithm | Object equivalence with semantic checks | Detect meaningful differences, ignore formatting |
| SQL Generation Strategy | Abstract factory pattern | Clean separation of dialect-specific logic |
| Statement Ordering | Topological sort with dependency graph | Ensure safe, dependency-aware migration execution |
| Testing Approach | Testcontainers for integration tests | Real database validation without mocks |
| Performance Target | <5 seconds for 100 tables | Cache parsed results, optimize hot paths |
| First Dialect | MySQL | Simplest syntax, good for validation |

---

## Open Questions Requiring Validation

1. **Druid-parser Version**: Verify exact version compatibility and API stability
2. **MySQL Container Testing**: Confirm Testcontainers MySQL image works in CI
3. **Large Schema Handling**: Profile memory usage for 1000+ tables scenario
4. **Foreign Key Circular Dependencies**: Implement cycle detection algorithm
5. **Character Set Handling**: Determine if charset/collation conversion is needed

These questions will be addressed during implementation and testing phases.
