# Data Model: Schema Provider System

**Date**: 2025-12-01 | **Feature**: Schema Provider System | **Module**: schema-core

## Overview

The schema-core module defines the canonical data model for representing database schemas. All schema providers (directory, database, git, JAR, custom) produce instances of these model classes.

## Design Principles

1. **Immutability**: All model objects are immutable after construction
2. **Type Safety**: Strong typing for all schema elements
3. **Validation**: Built-in validation at construction time
4. **Completeness**: Full DDL representation (columns, constraints, indexes, foreign keys)
5. **Dialect Awareness**: Support for MySQL, PostgreSQL, MariaDB, SQLite

## Core Entities

### Schema
Root entity representing a complete database schema.

```java
public final class Schema {
    private final Map<String, Database> databases;
    private final DatabasePlatform platform;

    // Methods
    public Optional<Database> getDatabase(String name)
    public Collection<Database> getDatabases()
    public boolean hasDatabase(String name)
}
```

**Fields**:
- `databases`: Map of database name → Database instance
- `platform`: Database platform (MYSQL, POSTGRESQL, MARIADB, SQLITE)

---

### Database
Represents a single database (CREATE DATABASE statement).

```java
public final class Database {
    private final String name;
    private final Map<String, Table> tables;
    private final Map<String, String> properties;

    // Methods
    public Optional<Table> getTable(String name)
    public Collection<Table> getTables()
    public boolean hasTable(String name)
}
```

**Fields**:
- `name`: Database name (must match directory/file name)
- `tables`: Map of table name → Table instance
- `properties`: Optional database properties (charset, collation, etc.)

**Validation**:
- Name cannot be null or empty
- Name must match filesystem directory name (for file-based providers)
- All tables must have unique names

---

### Table
Represents a single table (CREATE TABLE statement).

```java
public final class Table {
    private final String name;
    private final List<Column> columns;
    private final Map<String, Index> indexes;
    private final Map<String, Constraint> constraints;
    private final TableProperties properties;

    // Methods
    public Optional<Column> getColumn(String name)
    public List<Column> getColumns()
    public Optional<Index> getIndex(String name)
    public Optional<Constraint> getConstraint(String name)
}
```

**Fields**:
- `name`: Table name (must match .tbl filename for file-based providers)
- `columns`: Ordered list of columns (order matters for some databases)
- `indexes`: Map of index name → Index
- `constraints`: Map of constraint name → Constraint
- `properties`: Table-level properties (engine, charset, etc.)

**Validation**:
- Name cannot be null or empty
- At least one column required
- Column names must be unique
- Primary key constraint (if exists) must reference existing columns

---

### Column
Represents a column in a table.

```java
public final class Column {
    private final String name;
    private final DataType type;
    private final boolean nullable;
    private final Object defaultValue;
    private final List<String> annotations;

    // Methods
    public String getName()
    public DataType getType()
    public boolean isNullable()
    public Object getDefaultValue()
}
```

**Fields**:
- `name`: Column name
- `type`: Data type (see DataType below)
- `nullable`: Whether column accepts NULL values
- `defaultValue`: Default value (can be NULL, literal, or expression)
- `annotations`: Additional column metadata (AUTO_INCREMENT, etc.)

**Validation**:
- Name cannot be null or empty
- Type cannot be null
- Unique within table

---

### DataType
Represents SQL data types with dialect awareness.

```java
public final class DataType {
    private final String baseType;     // VARCHAR, INTEGER, etc.
    private final List<String> params; // (255), (10,2), etc.
    private final DatabasePlatform dialect;

    // Methods
    public String getBaseType()
    public List<String> getParams()
    public DatabasePlatform getDialect()
    public String toString()  // Full type string
}
```

**Supported Types**:
- Numeric: INT, BIGINT, DECIMAL, FLOAT, DOUBLE
- String: VARCHAR, CHAR, TEXT
- Binary: BLOB, VARBINARY
- Date/Time: DATE, TIME, DATETIME, TIMESTAMP
- Boolean: BOOLEAN
- JSON: JSON (platform-specific)

**Validation**:
- Base type must be supported for the dialect
- Parameters validated per type (e.g., VARCHAR requires length)

---

### Constraint
Represents table constraints (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK).

```java
public abstract class Constraint {
    private final String name;

    // Subtypes
    public static class PrimaryKey extends Constraint { ... }
    public static class ForeignKey extends Constraint { ... }
    public static class Unique extends Constraint { ... }
    public static class Check extends Constraint { ... }
}
```

**Primary Key**:
- Columns: List of column names
- Only one per table

**Foreign Key**:
- Columns: List of column names
- Referenced table and columns
- ON DELETE/UPDATE actions (CASCADE, SET NULL, etc.)

**Unique**:
- Columns: List of column names
- Can be single or composite

**Check**:
- Expression: SQL check expression

---

### Index
Represents database indexes.

```java
public final class Index {
    private final String name;
    private final List<String> columns;
    private final IndexType type;
    private final boolean unique;

    // IndexType: BTREE, HASH, FULLTEXT, etc.
}

**Fields**:
- `name`: Index name
- `columns`: Ordered list of column names
- `type`: Index type (BTREE default)
- `unique`: Whether index enforces uniqueness

**Validation**:
- Name unique within table
- At least one column required
- Column names must exist in table

---

### TableProperties
Table-level properties (engine, charset, etc.).

```java
public final class TableProperties {
    private final Map<String, String> properties;

    // Common properties
    public Optional<String> getEngine()      // MySQL: InnoDB
    public Optional<String> getCharset()     // utf8, utf8mb4
    public Optional<String> getCollation()   // utf8_general_ci
}
```

---

## Builder Pattern

All model objects use builder pattern for construction:

```java
Schema schema = Schema.builder()
    .platform(DatabasePlatform.MYSQL)
    .database(Database.builder()
        .name("mydb")
        .table(Table.builder()
            .name("users")
            .column(Column.builder()
                .name("id")
                .type(DataType.builder()
                    .baseType("INT")
                    .param("10")
                    .build())
                .nullable(false)
                .build())
            .column(Column.builder()
                .name("email")
                .type(DataType.builder()
                    .baseType("VARCHAR")
                    .param("255")
                    .build())
                .nullable(false)
                .build())
            .constraint(Constraint.PrimaryKey.builder()
                .name("pk_users")
                .column("id")
                .build())
            .build())
        .build())
    .build();
```

**Benefits**:
- Type-safe construction
- Required fields enforced at build time
- IDE autocomplete
- Immutable final objects

---

## Validation Rules

### File Structure Validation
For directory and JAR providers:
```
database-name/
├── database-name.db        # One .db file
├── table1.tbl              # Zero or more .tbl files
├── table2.tbl
└── ...
```

**Rules**:
- Directory name must match database name in .db file
- .db file name must match directory name
- .tbl file names must match table names inside files
- Exactly one .db file per directory
- No duplicate table names

### Schema Validation
- All referenced tables exist
- All referenced columns exist
- Foreign keys reference valid tables/columns
- No circular foreign key dependencies (unless explicitly allowed)
- Index columns exist in table
- Constraint columns exist in table

### Dialect Validation
- Data types must be valid for target platform
- Platform-specific features supported only on compatible platforms
- Index types valid for platform
- Constraint types valid for platform

---

## Serialization

**JSON Format** (for interoperability):
```json
{
  "platform": "MYSQL",
  "databases": [
    {
      "name": "mydb",
      "tables": [
        {
          "name": "users",
          "columns": [
            {
              "name": "id",
              "type": {
                "baseType": "INT",
                "params": ["10"]
              },
              "nullable": false
            }
          ],
          "indexes": [...],
          "constraints": [...]
        }
      ]
    }
  ]
}
```

**Use Cases**:
- Testing and debugging
- Inter-provider communication
- Caching and persistence
- CLI output formatting

---

## Error Handling

All validation errors thrown as SchemaValidationException:

```java
public final class SchemaValidationException extends Exception {
    private final List<ValidationError> errors;

    public static class ValidationError {
        private final ErrorCode code;
        private final String message;
        private final String entityPath;  // e.g., "mydb.users.id"
    }

    // Common error codes:
    // MISSING_REQUIRED_FIELD
    // INVALID_DATABASE_NAME
    // DUPLICATE_TABLE_NAME
    // INVALID_COLUMN_REFERENCE
    // UNSUPPORTED_DATA_TYPE
    // INVALID_FOREIGN_KEY
}
```

---

## Performance Considerations

1. **Immutability**: Thread-safe, no synchronization needed
2. **Lazy Loading**: Schemas can be built incrementally
3. **Caching**: Schema instances can be cached by provider
4. **Streaming**: Large schemas processed as streams
5. **Memory**: Bounded by schema size, no unbounded collections

---

## Extensibility

The model is extensible for future needs:
- New constraint types (add subclasses)
- New index types (extend IndexType enum)
- New properties (add to TableProperties)
- Custom annotations (extend Column/Table annotations)

No breaking changes to existing API required.
