# Data Model: SQL Table Differ

## Overview

This document defines the core data models for the SQL Table Differ component, representing database schemas, tables, columns, and migration plans.

---

## Core Entities

### Schema

**Purpose**: Represents a complete database structure including all tables, columns, constraints, indexes, and relationships.

**Properties**:
- `tables`: Map of table name → Table object
- `dialect`: Database dialect (MySQL, PostgreSQL, Oracle)
- `version`: Optional schema version identifier
- `metadata`: Additional metadata (creation time, source, etc.)

**Lifecycle**:
1. Created from CREATE TABLE statements via SchemaParser
2. Validated for consistency and completeness
3. Compared with another schema to detect differences
4. Used as input for migration plan generation

**Constraints**:
- Table names must be unique within schema
- All referenced tables must exist (foreign key validation)
- Schema must be internally consistent

### Table

**Purpose**: Represents a single database table with its structure and relationships.

**Properties**:
- `name`: Table name (case-sensitive for most databases)
- `columns`: List of Column objects defining table structure
- `constraints`: Map of constraint name → Constraint object
- `indexes`: List of Index objects
- `comment`: Optional table comment
- `options`: Table-specific options (engine, tablespace, etc.)

**Relationships**:
- May reference other tables via foreign key constraints
- May be referenced by other tables
- Part of a parent Schema

**Constraints**:
- Must have at least one column
- Column names must be unique within table
- Primary key, if present, must reference existing columns
- All foreign key references must point to existing tables/columns

### Column

**Purpose**: Represents an individual data field within a table.

**Properties**:
- `name`: Column name (case-sensitive)
- `dataType`: Data type (VARCHAR, INT, TIMESTAMP, etc.)
- `length`: Optional length parameter (for VARCHAR(255))
- `precision`: Optional precision (for DECIMAL(10,2))
- `scale`: Optional scale (for DECIMAL(10,2))
- `nullable`: Boolean indicating if column allows NULL values
- `defaultValue`: Optional default value expression
- `autoIncrement`: Boolean indicating auto-increment/identity
- `comment`: Optional column comment
- `constraints`: Column-level constraints (PRIMARY KEY, UNIQUE, NOT NULL, CHECK, FOREIGN KEY)
- `characterSet`: Optional character set (MySQL)
- `collation`: Optional collation (MySQL)

**Constraints**:
- Data type must be valid for the database dialect
- Default value must match column type
- Primary key columns are automatically NOT NULL
- Auto-increment columns must be numeric

**State Transitions**:
- May be added in migration (new column)
- May be removed in migration (dropped column)
- May be modified (type, constraints, nullability changes)

### Constraint

**Purpose**: Represents a table or column constraint (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK).

**Subtypes**:
- `PrimaryKey`: Primary key constraint
- `ForeignKey`: Foreign key constraint
- `UniqueConstraint`: Unique constraint
- `CheckConstraint`: Check constraint
- `NotNullConstraint`: NOT NULL constraint (often represented at column level)

**Properties (Common)**:
- `name`: Constraint name (optional for some databases)
- `enabled`: Boolean indicating if constraint is enabled
- `deferred`: Boolean indicating if constraint is deferred (PostgreSQL, Oracle)

**PrimaryKey Specific**:
- `columns`: List of columns in the primary key

**ForeignKey Specific**:
- `referencedTable`: Name of referenced table
- `referencedColumns`: List of columns in referenced table
- `onDelete`: Action on delete (CASCADE, SET NULL, RESTRICT, NO ACTION)
- `onUpdate`: Action on update (CASCADE, SET NULL, RESTRICT, NO ACTION)

**UniqueConstraint Specific**:
- `columns`: List of columns in the unique constraint

**CheckConstraint Specific**:
- `expression`: SQL expression defining the check

### Index

**Purpose**: Represents an index on one or more columns.

**Properties**:
- `name`: Index name
- `columns`: List of indexed columns in order
- `unique`: Boolean indicating if index enforces uniqueness
- `type`: Index type (BTREE, HASH, etc.)
- `options`: Index-specific options (parser, algorithm, etc.)

**Relationships**:
- Tied to a specific table
- May reference one or more columns

### SchemaDiff

**Purpose**: Structured representation of differences between two schemas.

**Properties**:
- `addedTables`: Map of table name → Table (new in target)
- `removedTables`: Map of table name → Table (removed from target)
- `modifiedTables`: Map of table name → TableDiff (changed in target)
- `sourceSchema`: Source schema
- `targetSchema`: Target schema

**Sub-Entity: TableDiff**

**Purpose**: Represents differences for a single table.

**Properties**:
- `addedColumns`: List of Column objects
- `removedColumns`: List of Column objects (name only needed)
- `modifiedColumns`: Map of column name → ColumnDiff
- `addedConstraints`: List of Constraint objects
- `removedConstraints`: List of Constraint names
- `modifiedConstraints`: Map of constraint name → Constraint
- `addedIndexes`: List of Index objects
- `removedIndexes`: List of Index names
- `modifiedIndexes`: Map of index name → IndexDiff

**Sub-Entity: ColumnDiff**

**Purpose**: Represents semantic differences between two column definitions.

**Properties**:
- `oldColumn`: Original column definition
- `newColumn`: New column definition
- `changes`: Set of ChangeType indicating what changed
  - DATA_TYPE_CHANGED
  - NULLABILITY_CHANGED
  - DEFAULT_VALUE_CHANGED
  - AUTO_INCREMENT_CHANGED
  - CHARACTER_SET_CHANGED
  - COLLATION_CHANGED
  - COMMENT_CHANGED

### MigrationPlan

**Purpose**: Immutable plan for migrating from source schema to target schema.

**Properties**:
- `statements`: Ordered list of SQL statements to execute
- `warnings`: List of Warning objects for user attention
- `destructiveOperations`: List of DestructiveOperation objects
- `sourceSchema`: Source schema representation
- `targetSchema`: Target schema representation
- `databaseDialect`: Target database dialect
- `createdAt`: Timestamp of plan generation

**Sub-Entity: Warning**

**Purpose**: Non-blocking notification for users about potential issues.

**Properties**:
- `type`: Warning type (DEPRECATED_SYNTAX, DATA_LOSS_RISK, etc.)
- `message`: Human-readable warning message
- `severity`: Severity level (INFO, WARN, ERROR)
- `statementIndex`: Optional index of related SQL statement

**Sub-Entity: DestructiveOperation**

**Purpose**: Represents operations that may cause data loss.

**Properties**:
- `operationType`: Type (DROP_TABLE, DROP_COLUMN, etc.)
- `targetObject`: Object being destroyed (table name, column name)
- `sqlStatement`: The SQL statement that will be executed
- `severity`: Severity level
- `dataLossRisk`: Boolean indicating if data will be lost
- `estimatedRowsAffected`: Optional estimate of affected rows

### DatabaseDialect

**Purpose**: Enumeration of supported database dialects.

**Values**:
- `MYSQL`: MySQL/MariaDB
- `POSTGRESQL`: PostgreSQL
- `ORACLE`: Oracle Database

**Responsibilities**:
- Determine appropriate SQL generation syntax
- Validate schema compatibility with dialect
- Provide dialect-specific type mappings

### MigrationOptions

**Purpose**: Configuration options for migration generation.

**Properties**:
- `wrapInTransaction`: Boolean (wrap migration in transaction if supported)
- `includeComments`: Boolean (include explanatory comments in SQL)
- `failOnDestructive`: Boolean (fail if destructive operations detected)
- `transactionalDdl`: Boolean (use transactional DDL if supported)
- `includeRollback`: Boolean (generate rollback SQL)

---

## Validation Rules

### Schema Validation

1. **Table Existence**: All foreign key references must point to existing tables
2. **Column Existence**: All constraint columns must exist
3. **Primary Key**: Primary key columns must be NOT NULL
4. **Unique Constraints**: Column sets must be unique within table
5. **Circular Dependencies**: Check for circular foreign key dependencies (error)

### Schema Diff Validation

1. **Non-Overlapping Changes**: Added and removed tables/columns must not overlap
2. **Change Consistency**: Modified elements must exist in both source and target
3. **Constraint References**: Added constraints must reference valid columns/tables

### Migration Plan Validation

1. **Dependency Order**: Statements must be ordered to satisfy all dependencies
2. **No Orphaned Operations**: All operations must contribute to achieving target schema
3. **Transaction Safety**: Transaction wrapping must be compatible with dialect

---

## Object Relationships

```
Schema
├── Table (0..*)
│   ├── Column (1..*)
│   │   ├── Constraint (0..*)
│   │   └── CharacterSet/Collation
│   ├── Constraint (0..*)
│   │   ├── PrimaryKey (0..1)
│   │   ├── ForeignKey (0..*)
│   │   ├── UniqueConstraint (0..*)
│   │   └── CheckConstraint (0..*)
│   └── Index (0..*)
│
└── Metadata

SchemaDiff
├── AddedTables (Table)
├── RemovedTables (Table)
└── ModifiedTables (TableDiff)
    ├── AddedColumns (Column)
    ├── RemovedColumns (Column)
    ├── ModifiedColumns (ColumnDiff)
    ├── AddedConstraints (Constraint)
    ├── RemovedConstraints (Constraint)
    └── ModifiedConstraints (Constraint)

MigrationPlan
├── Statements (SQL)
├── Warnings (Warning)
├── DestructiveOperations (DestructiveOperation)
└── Options (MigrationOptions)
```

---

## Immutable Design

All data model classes follow immutable design patterns:

1. **Final Classes**: All classes are final to prevent inheritance
2. **Final Fields**: All fields are private and final
3. **No Setters**: No setter methods; only constructors/factories
4. **Builders**: Use builder pattern for complex object construction
5. **Thread-Safe**: Immutable objects are inherently thread-safe
6. **Defensive Copies**: Collections are copied in constructors to prevent external modification

**Example: Table Class Design**

```java
public final class Table {
    private final String name;
    private final List<Column> columns;
    private final Map<String, Constraint> constraints;
    private final List<Index> indexes;

    private Table(Builder builder) {
        this.name = builder.name;
        this.columns = Collections.unmodifiableList(new ArrayList<>(builder.columns));
        this.constraints = Collections.unmodifiableMap(new HashMap<>(builder.constraints));
        this.indexes = Collections.unmodifiableList(new ArrayList<>(builder.indexes));
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    // Getters only (no setters)
    public String getName() { return name; }
    public List<Column> getColumns() { return columns; }
    public Map<String, Constraint> getConstraints() { return constraints; }

    // Builder pattern
    public static class Builder {
        // ... builder implementation
    }
}
```

---

## Serialization Considerations

**JSON Serialization**:
- Use Jackson or Gson for JSON representation
- Support round-trip serialization for schema comparison
- Exclude internal/dynamic fields (caches, transient data)

**Binary Serialization**:
- Consider Java serialization or Protocol Buffers for internal use
- Versioning required for forward compatibility
- Optimize for performance with large schemas

**XML Support**:
- JAXB for Java-to-XML mapping
- Useful for enterprise integration scenarios

---

## Performance Considerations

1. **Structural Sharing**: Use shared references for unchanged schema components
2. **Lazy Loading**: Load expensive properties on-demand
3. **Hash-Based Comparison**: Compute hashes for quick equality checks
4. **Memory Efficiency**: Prefer Lists over Sets unless uniqueness is required
5. **Caching**: Cache hash codes and computed properties

---

## Error Handling

All validation errors follow consistent pattern:

```java
public class ValidationException extends Exception {
    private final ErrorCode errorCode;
    private final List<String> violations;

    public enum ErrorCode {
        TABLE_NOT_FOUND,
        COLUMN_NOT_FOUND,
        CONSTRAINT_VIOLATION,
        CIRCULAR_DEPENDENCY,
        INVALID_DATA_TYPE
    }
}
```

**Error Codes**:
- `TABLE_NOT_FOUND`: Referenced table does not exist
- `COLUMN_NOT_FOUND`: Referenced column does not exist
- `CONSTRAINT_VIOLATION`: Constraint definition violates rules
- `CIRCULAR_DEPENDENCY`: Circular foreign key reference detected
- `INVALID_DATA_TYPE`: Data type not supported or invalid
- `INCOMPATIBLE_DIALECT`: Feature not supported in target dialect
- `CONSTRAINT_NAME_REQUIRED`: Constraint name required but not provided
