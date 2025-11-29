# SQL Table Differ - Quick Start Guide

## Overview

The SQL Table Differ is a Java library for comparing database schemas and generating migration SQL. It uses the druid-parser library to parse CREATE TABLE statements and generates database-specific migration scripts for MySQL, PostgreSQL, and Oracle.

## Installation

### Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.aidvps:schema-kit-differ:1.0.0'
    implementation 'com.aidvps:druid-parser:1.2.28-SNAPSHOT'
}

repositories {
    // Check local Maven repository first for SNAPSHOT dependencies
    mavenLocal()
    // Fall back to central repository
    mavenCentral()
}
```

**Important**: The `mavenLocal()` repository is essential for accessing SNAPSHOT versions like `druid-parser:1.2.28-SNAPSHOT`.
```

### Maven

```xml
<dependency>
    <groupId>com.aidvps</groupId>
    <artifactId>schema-kit-differ</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Druid Parser (provided dependency) -->
<dependency>
    <groupId>com.aidvps</groupId>
    <artifactId>druid-parser</artifactId>
    <version>1.2.28-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>/path/to/druid-parser-1.2.28-SNAPSHOT.jar</systemPath>
</dependency>
```

## Basic Usage

### Simple Migration Generation

Generate migration SQL from source schema to target schema:

```java
import com.aidvps.druid.differ.TableDiffer;
import com.aidvps.druid.differ.model.MigrationPlan;

public class Example {
    public static void main(String[] args) {
        // Define source schema
        String sourceSchema = """
            CREATE TABLE users (
                id INT PRIMARY KEY,
                name VARCHAR(255)
            );
            """;

        // Define target schema
        String targetSchema = """
            CREATE TABLE users (
                id INT PRIMARY KEY,
                name VARCHAR(255),
                email VARCHAR(255) NOT NULL
            );
            """;

        // Create TableDiffer instance
        TableDiffer differ = TableDiffer.builder()
            .withDialect(DatabaseDialect.MYSQL)
            .build();

        // Generate migration
        MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

        // Print generated SQL
        System.out.println("Generated Migration SQL:");
        for (String statement : plan.getStatements()) {
            System.out.println(statement + ";");
        }

        // Check for warnings
        if (!plan.getWarnings().isEmpty()) {
            System.out.println("\nWarnings:");
            plan.getWarnings().forEach(warning ->
                System.out.println("  - " + warning.getMessage()));
        }

        // Check for destructive operations
        if (plan.hasDestructiveOperations()) {
            System.out.println("\n⚠️  This migration contains destructive operations!");
        }
    }
}
```

**Output:**

```
Generated Migration SQL:
-- Migration from source to target schema
-- Generated: 2025-11-29

ALTER TABLE users ADD COLUMN email VARCHAR(255) NOT NULL;
```

### Multi-Table Schema Comparison

Compare schemas with multiple tables and relationships:

```java
String sourceSchema = """
    CREATE TABLE users (
        id INT PRIMARY KEY,
        name VARCHAR(255)
    );

    CREATE TABLE posts (
        id INT PRIMARY KEY,
        user_id INT,
        title VARCHAR(255),
        FOREIGN KEY (user_id) REFERENCES users(id)
    );
    """;

String targetSchema = """
    CREATE TABLE users (
        id INT PRIMARY KEY,
        name VARCHAR(255),
        email VARCHAR(255)
    );

    CREATE TABLE posts (
        id INT PRIMARY KEY,
        user_id INT,
        title VARCHAR(255),
        content TEXT,
        FOREIGN KEY (user_id) REFERENCES users(id)
    );

    CREATE TABLE comments (
        id INT PRIMARY KEY,
        post_id INT,
        text TEXT,
        FOREIGN KEY (post_id) REFERENCES posts(id)
    );
    """;

TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.POSTGRESQL)
    .build();

MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
```

**Generated PostgreSQL SQL:**

```sql
-- Add email column to users
ALTER TABLE users ADD COLUMN email VARCHAR(255);

-- Add content column to posts
ALTER TABLE posts ADD COLUMN content TEXT;

-- Create new comments table
CREATE TABLE comments (
    id INT PRIMARY KEY,
    post_id INT,
    text TEXT
);

-- Add foreign key to comments
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_post
    FOREIGN KEY (post_id) REFERENCES posts(id);
```

### Using Migration Options

Configure migration generation with options:

```java
MigrationOptions options = MigrationOptions.builder()
    .wrapInTransaction(true)
    .includeComments(true)
    .failOnDestructive(true)
    .includeRollback(true)
    .build();

TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.ORACLE)
    .withValidationLevel(ValidationLevel.STRICT)
    .build();

MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema, options);
```

**Generated Oracle SQL:**

```sql
-- Migration from source to target schema
-- Generated: 2025-11-29
-- Database: Oracle

BEGIN
    -- Add email column to users
    EXECUTE IMMEDIATE 'ALTER TABLE users ADD (email VARCHAR2(255))';

    -- Create new comments table
    EXECUTE IMMEDIATE 'CREATE TABLE comments (
        id NUMBER(10) PRIMARY KEY,
        post_id NUMBER(10),
        text CLOB
    )';

    -- Add foreign key constraint
    EXECUTE IMMEDIATE 'ALTER TABLE comments
        ADD CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(id)';

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/
```

## Advanced Examples

### Detecting Schema Differences

Get detailed information about schema changes:

```java
// Generate migration and inspect the plan
MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

// Print summary
System.out.println("Migration Summary:");
System.out.println("  Source schema hash: " + plan.getSourceSchemaHash());
System.out.println("  Target schema hash: " + plan.getTargetSchemaHash());
System.out.println("  Number of statements: " + plan.getStatements().size());
System.out.println("  Number of warnings: " + plan.getWarnings().size());
System.out.println("  Has destructive operations: " + plan.hasDestructiveOperations());

// Inspect destructive operations
if (plan.hasDestructiveOperations()) {
    System.out.println("\nDestructive Operations:");
    plan.getDestructiveOperations().forEach(op -> {
        System.out.println("  Type: " + op.getOperationType());
        System.out.println("  Target: " + op.getTargetObject());
        System.out.println("  Severity: " + op.getSeverity());
        System.out.println("  Data Loss Risk: " + op.isDataLossRisk());
        System.out.println();
    });
}
```

### Generating Rollback SQL

Generate SQL to revert a migration:

```java
// First, generate the forward migration
MigrationPlan forwardPlan = differ.generateMigration(sourceSchema, targetSchema);

// Generate rollback SQL
String rollbackSql = differ.generateRollback(forwardPlan);

// Or use the MigrationPlan object directly
String rollbackSql2 = differ.generateRollback(forwardPlan);

// Execute rollback (example)
System.out.println("Rollback SQL:");
System.out.println(rollbackSql);
```

### Handling Errors

The library provides detailed error information:

```java
try {
    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
} catch (SchemaParsingException e) {
    System.err.println("Failed to parse schema:");
    System.err.println("  Error: " + e.getMessage());
    if (e.getLine() > 0) {
        System.err.println("  Location: Line " + e.getLine() + ", Column " + e.getColumn());
    }
} catch (SchemaCompatibilityException e) {
    System.err.println("Schemas are incompatible:");
    System.err.println("  Error: " + e.getMessage());
} catch (GenerationException e) {
    System.err.println("Failed to generate migration:");
    System.err.println("  Error: " + e.getMessage());
    e.printStackTrace();
}
```

### Custom Validation

Add custom validation before generating migration:

```java
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .withValidationLevel(ValidationLevel.STRICT)
    .build();

// Pre-validate schemas
ValidationResult result = differ.validateSchemas(sourceSchema, targetSchema);
if (!result.isValid()) {
    System.err.println("Validation failed:");
    result.getViolations().forEach(violation ->
        System.err.println("  - " + violation));
    return;
}

// Proceed with migration generation
MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
```

## Database-Specific Features

### MySQL

- Supports `AUTO_INCREMENT` syntax
- Uses `MODIFY COLUMN` for type changes
- Supports table engine specifications
- Handles `IF NOT EXISTS` clauses

**Example:**

```java
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .build();
```

### PostgreSQL

- Uses `SERIAL` or `IDENTITY` for auto-increment
- Requires constraint names for drops
- Supports `INHERITS` for table inheritance
- Uses `TYPE` keyword for column type changes

**Example:**

```java
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.POSTGRESQL)
    .build();
```

### Oracle

- Uses `VARCHAR2` instead of `VARCHAR`
- Requires parentheses for multiple column operations
- Uses `NUMBER` for integers
- Supports `CLOB` for large text

**Example:**

```java
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.ORACLE)
    .build();
```

## Performance Tips

1. **Cache TableDiffer instances**: Creating instances is expensive; reuse them
2. **Use options wisely**: Only set `failOnDestructive` if you need strict validation
3. **Validate first**: Use `validateSchemas()` to catch issues before generation
4. **Consider transaction wrapping**: Enable for PostgreSQL and Oracle

```java
// Good: Reuse TableDiffer instance
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .build();

for (int i = 0; i < 10; i++) {
    MigrationPlan plan = differ.generateMigration(schemas[i].getSource(), schemas[i].getTarget());
    // Process plan
}
```

## Integration Examples

### Spring Boot Integration

```java
@Service
public class SchemaMigrationService {
    private final TableDiffer differ;

    @Autowired
    public SchemaMigrationService(TableDiffer differ) {
        this.differ = differ;
    }

    public String generateMigration(String source, String target, DatabaseDialect dialect) {
        TableDiffer instance = TableDiffer.builder()
            .withDialect(dialect)
            .build();

        MigrationPlan plan = instance.generateMigration(source, target);
        return String.join("\n", plan.getStatements());
    }
}
```

### CI/CD Pipeline Integration

```java
public class SchemaMigrationPipeline {
    public static void main(String[] args) {
        String sourceSchema = loadFromFile(args[0]);
        String targetSchema = loadFromFile(args[1]);
        DatabaseDialect dialect = DatabaseDialect.valueOf(args[2]);

        TableDiffer differ = TableDiffer.builder()
            .withDialect(dialect)
            .build();

        MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

        // Check for destructive operations
        if (plan.hasDestructiveOperations()) {
            System.err.println("ERROR: Migration contains destructive operations!");
            System.err.println("Manual review required before proceeding.");
            System.exit(1);
        }

        // Write migration to file
        writeMigrationToFile(plan.getStatements(), "migration.sql");

        // Generate rollback script
        String rollback = differ.generateRollback(plan);
        writeMigrationToFile(Arrays.asList(rollback), "rollback.sql");
    }
}
```

## Common Pitfalls

### 1. Forgetting to Check for Destructive Operations

```java
// Bad: Not checking for destructive operations
MigrationPlan plan = differ.generateMigration(source, target);
executeMigration(plan.getStatements()); // May drop data!

// Good: Check first
if (plan.hasDestructiveOperations()) {
    throw new IllegalStateException("Destructive operations detected. Review required.");
}
executeMigration(plan.getStatements());
```

### 2. Not Handling Exceptions

```java
// Bad: No exception handling
MigrationPlan plan = differ.generateMigration(source, target);

// Good: Proper exception handling
try {
    MigrationPlan plan = differ.generateMigration(source, target);
} catch (SchemaParsingException e) {
    // Handle invalid SQL
} catch (SchemaCompatibilityException e) {
    // Handle incompatible schemas
}
```

### 3. Ignoring Warnings

```java
// Bad: Ignoring warnings
MigrationPlan plan = differ.generateMigration(source, target);

// Good: Check and handle warnings
if (!plan.getWarnings().isEmpty()) {
    plan.getWarnings().forEach(w -> {
        if (w.getSeverity() == Warning.Severity.ERROR) {
            throw new MigrationException("Critical warning: " + w.getMessage());
        }
    });
}
```

## Next Steps

- Read the [API Documentation](api.md) for detailed method references
- Explore the [Architecture Guide](architecture.md) for implementation details
- Review the [Testing Guide](testing.md) for best practices
- Check out the [Examples Repository](https://github.com/example/sql-differ-examples)

## Support

- Documentation: [https://docs.aidvps.com/schema-kit-differ](https://docs.aidvps.com/schema-kit-differ)
- Issues: [https://github.com/aidvps/schema-kit-differ/issues](https://github.com/aidvps/schema-kit-differ/issues)
- License: Apache 2.0
