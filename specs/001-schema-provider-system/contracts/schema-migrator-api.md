# Contract: Schema Migrator API

**Module**: schema-migrator | **Version**: 1.0.0 | **Date**: 2025-12-01

## Overview

The Schema Migrator API provides database-level migration generation and comparison between schemas from different sources.

## Core Interface

### SchemaMigrator

```java
public interface SchemaMigrator {
    /**
     * Generate migration script from source to target schema.
     *
     * @param source Source schema
     * @param target Target schema
     * @param config Migration configuration
     * @return Migration script with SQL statements
     * @throws MigrationException if generation fails
     */
    MigrationScript generateMigration(Schema source, Schema target, MigrationConfig config)
        throws MigrationException;

    /**
     * Compare two schemas and return differences.
     *
     * @param source Source schema
     * @param target Target schema
     * @return SchemaDiff describing all differences
     * @throws MigrationException if comparison fails
     */
    SchemaDiff compare(Schema source, Schema target) throws MigrationException;

    /**
     * Validate migration script against target schema.
     *
     * @param script Migration script to validate
     * @param targetSchema Target schema
     * @return ValidationResult
     */
    ValidationResult validateMigration(MigrationScript script, Schema targetSchema);
}
```

**Contract**:
- Must handle any source/target combination (directory↔database, git↔jar, etc.)
- Must respect foreign key dependencies
- Must generate platform-appropriate SQL
- Must complete within 30 seconds for typical schemas

---

## Configuration

### MigrationConfig

Configuration for migration generation.

```java
public interface MigrationConfig {
    /**
     * Get target database platform.
     *
     * @return DatabasePlatform (MYSQL, POSTGRESQL, MARIADB, SQLITE)
     */
    DatabasePlatform getTargetPlatform();

    /**
     * Get migration mode.
     *
     * @return MigrationMode
     */
    MigrationMode getMode();

    /**
     * Whether to include DROP statements.
     *
     * @return true to include, false to skip
     */
    boolean isIncludeDrops();

    /**
     * Whether to wrap in transactions.
     *
     * @return true to wrap in transactions
     */
    boolean isTransactional();

    /**
     * Get custom SQL generator for specific features.
     *
     * @return Optional custom generator
     */
    Optional<CustomSqlGenerator> getCustomGenerator();

    /**
     * Create builder.
     */
    static Builder builder() { ... }

    interface Builder {
        Builder targetPlatform(DatabasePlatform platform);
        Builder mode(MigrationMode mode);
        Builder includeDrops(boolean include);
        Builder transactional(boolean transactional);
        Builder customGenerator(CustomSqlGenerator generator);
        MigrationConfig build();
    }
}

public enum MigrationMode {
    /**
     * Full migration: create new objects, alter existing, drop removed
     */
    FULL("Complete migration with all changes"),

    /**
     * Forward-only: create new objects, alter existing (no drops)
     */
    FORWARD_ONLY("Forward migration only, no drops"),

    /**
     * Diff-only: return SQL for specific changes
     */
    DIFF_ONLY("Generate SQL for detected changes only");
}
```

---

## Results

### MigrationScript

Generated migration SQL with metadata.

```java
public final class MigrationScript {
    private final List<MigrationStatement> statements;
    private final DatabasePlatform targetPlatform;
    private final MigrationMode mode;
    private final Map<String, Object> metadata;

    /**
     * Add statement to script.
     */
    public MigrationScript addStatement(MigrationStatement statement)

    /**
     * Get all statements.
     */
    public List<MigrationStatement> getStatements()

    /**
     * Convert to SQL string.
     */
    public String toSql()

    /**
     * Convert to formatted SQL string.
     */
    public String toFormattedSql()
}
```

### MigrationStatement

Individual SQL statement with metadata.

```java
public final class MigrationStatement {
    private final String sql;
    private final StatementType type;
    private final String description;
    private final int order;
    private final Set<String> dependencies;

    public enum StatementType {
        CREATE_DATABASE,
        DROP_DATABASE,
        CREATE_TABLE,
        ALTER_TABLE,
        DROP_TABLE,
        CREATE_INDEX,
        DROP_INDEX,
        CREATE_CONSTRAINT,
        DROP_CONSTRAINT,
        CUSTOM
    }
}
```

---

## Schema Comparison

### SchemaDiff

Complete diff between two schemas.

```java
public final class SchemaDiff {
    private final Map<String, DatabaseDiff> databaseDiffs;
    private final Set<SchemaChange> changes;

    /**
     * Get diff for specific database.
     */
    public Optional<DatabaseDiff> getDatabaseDiff(String databaseName)

    /**
     * Check if any changes detected.
     */
    public boolean hasChanges()

    /**
     * Get all changes.
     */
    public Set<SchemaChange> getChanges()
}
```

### DatabaseDiff

Diff for a specific database.

```java
public final class DatabaseDiff {
    private final String databaseName;
    private final Database source;
    private final Database target;
    private final Set<TableDiff> tableDiffs;
    private final boolean isNew;
    private final boolean isDeleted;
}
```

### TableDiff

Diff for a specific table.

```java
public final class TableDiff {
    private final String tableName;
    private final Table source;
    private final Table target;
    private final Set<ColumnDiff> columnDiffs;
    private final Set<IndexDiff> indexDiffs;
    private final Set<ConstraintDiff> constraintDiffs;
    private final boolean isNew;
    private final boolean isDeleted;
    private final boolean isModified;
}
```

### SchemaChange

Individual change description.

```java
public abstract class SchemaChange {
    private final ChangeType type;
    private final String description;
    private final String entityPath;  // e.g., "mydb.users.email"

    public enum Type {
        DATABASE_CREATED,
        DATABASE_DROPPED,
        TABLE_CREATED,
        TABLE_DROPPED,
        TABLE_MODIFIED,
        COLUMN_ADDED,
        COLUMN_DROPPED,
        COLUMN_MODIFIED,
        INDEX_CREATED,
        INDEX_DROPPED,
        CONSTRAINT_ADDED,
        CONSTRAINT_DROPPED
    }
}
```

---

## Validation

### ValidationResult

Result of migration validation.

```java
public final class ValidationResult {
    private final boolean isValid;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;

    /**
     * Check if validation passed.
     */
    public boolean isValid()

    /**
     * Get all errors.
     */
    public List<ValidationError> getErrors()

    /**
     * Get all warnings.
     */
    public List<ValidationWarning> getWarnings()
}

public final class ValidationError {
    private final String code;
    private final String message;
    private final String statement;
    private final int lineNumber;
}

public final class ValidationWarning {
    private final String code;
    private final String message;
    private final String statement;
}
```

---

## Custom Generation

### CustomSqlGenerator

Interface for custom SQL generation.

```java
public interface CustomSqlGenerator {
    /**
     * Generate SQL for custom change.
     *
     * @param change Custom change
     * @return SQL statement
     */
    MigrationStatement generateSql(CustomChange change);

    /**
     * Check if generator handles this change type.
     *
     * @param change Change to check
     * @return true if can handle
     */
    boolean canHandle(CustomChange change);
}
```

---

## Usage Examples

### Basic Migration

```java
SchemaMigrator migrator = new DefaultSchemaMigrator();

Schema source = ...;  // From directory provider
Schema target = ...;  // From database provider

MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.MYSQL)
    .mode(MigrationMode.FULL)
    .includeDrops(true)
    .transactional(true)
    .build();

MigrationScript script = migrator.generateMigration(source, target, config);

System.out.println(script.toFormattedSql());
```

### Compare Only

```java
SchemaMigrator migrator = new DefaultSchemaMigrator();

Schema schema1 = ...;
Schema schema2 = ...;

SchemaDiff diff = migrator.compare(schema1, schema2);

if (diff.hasChanges()) {
    System.out.println("Differences detected:");
    diff.getChanges().forEach(change -> {
        System.out.println("  " + change.getType() + ": " + change.getDescription());
    });
}
```

### Forward-Only Migration

```java
MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.POSTGRESQL)
    .mode(MigrationMode.FORWARD_ONLY)
    .includeDrops(false)  // Skip DROP statements
    .build();

MigrationScript script = migrator.generateMigration(source, target, config);
```

### Custom SQL Generator

```java
public class MyCustomGenerator implements CustomSqlGenerator {
    @Override
    public MigrationStatement generateSql(CustomChange change) {
        // Custom SQL generation logic
        String sql = "/* Custom: " + change.getDescription() + " */";
        return MigrationStatement.builder()
            .sql(sql)
            .type(StatementType.CUSTOM)
            .description(change.getDescription())
            .build();
    }

    @Override
    public boolean canHandle(CustomChange change) {
        return change.getType() == CustomChange.Type.MY_FEATURE;
    }
}

MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.MYSQL)
    .customGenerator(new MyCustomGenerator())
    .build();
```

---

## Migration Ordering

The migrator must respect proper SQL execution order:

### Order Rules

1. **Create operations first** (in dependency order)
   - CREATE DATABASE
   - CREATE TABLE (respecting foreign key dependencies)
   - CREATE INDEX
   - CREATE CONSTRAINT

2. **Alter operations next**
   - ALTER TABLE ADD COLUMN
   - ALTER TABLE MODIFY COLUMN
   - ALTER TABLE ADD CONSTRAINT
   - ALTER TABLE ADD INDEX

3. **Drop operations last** (in reverse dependency order)
   - DROP CONSTRAINT
   - DROP INDEX
   - DROP TABLE
   - DROP DATABASE

### Dependency Resolution

```java
/**
 * Build dependency graph and determine execution order.
 *
 * @param changes List of all changes
 * @return Ordered list of changes
 */
List<SchemaChange> orderChanges(List<SchemaChange> changes)
```

**Algorithm**:
1. Build dependency graph (foreign keys reference constraints)
2. Detect circular dependencies
3. Topological sort for create operations
4. Reverse topological sort for drop operations
5. Interleave based on mode (FULL, FORWARD_ONLY, DIFF_ONLY)

---

## Error Handling

### MigrationException

Thrown when migration generation fails.

```java
public class MigrationException extends Exception {
    private final ErrorCode errorCode;
    private final String phase;  // DIFF, ORDER, GENERATE, VALIDATE

    public enum ErrorCode {
        SOURCE_SCHEMA_INVALID,
        TARGET_SCHEMA_INVALID,
        PLATFORM_NOT_SUPPORTED,
        DEPENDENCY_CYCLE_DETECTED,
        GENERATION_ERROR,
        VALIDATION_FAILED,
        TIMEOUT,
        UNKNOWN_ERROR
    }
}
```

---

## Platform Support

### DatabasePlatform

Supported target platforms.

```java
public enum DatabasePlatform {
    MYSQL("com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("org.postgresql.Driver"),
    MARIADB("org.mariadb.jdbc.Driver"),
    SQLITE("org.sqlite.JDBC");

    private final String driverClass;

    public String getDriverClass() { ... }
}
```

**Platform-specific behavior**:
- **MySQL/MariaDB**: AUTO_INCREMENT, ENGINE=InnoDB, charset handling
- **PostgreSQL**: SERIAL, SERIAL4, SERIAL8, schema qualified names
- **SQLite**: Limited ALTER TABLE support, PRAGMA foreign_keys

---

## Contract Testing

All migrator implementations must pass:

1. **Basic Migration**
   - Create new tables, columns, indexes
   - Alter existing structures
   - Drop removed elements

2. **Dependency Handling**
   - Foreign key dependencies respected
   - No circular dependency errors
   - Proper ordering of create/drop

3. **Platform Specificity**
   - Generate correct SQL for target platform
   - Dialect differences handled
   - No cross-dialect errors

4. **Mode Variations**
   - FULL: includes drops
   - FORWARD_ONLY: no drops
   - DIFF_ONLY: only detected changes

5. **Performance**
   - Generate migrations within 30 seconds
   - No memory leaks
   - Efficient for large schemas

6. **Validation**
   - Validate generated SQL
   - Detect invalid operations
   - Provide helpful error messages

---

## Versioning

**Breaking Changes**: Increment major version, maintain compatibility for one major version
**Backward Compatibility**: Generated migrations from older versions still valid
**Deprecation**: Deprecated methods marked @Deprecated, removed after 2 major versions

**Version History**:
- 1.0.0: Initial API
