# SQL Table Differ

A powerful, database-agnostic tool for comparing database schemas and generating migration SQL statements. Supports MySQL, PostgreSQL, and Oracle databases.

## Features

- **Multi-Dialect Support**: Generate migration SQL for MySQL, PostgreSQL, and Oracle
- **Schema Comparison**: Compare database schemas and detect differences
- **Migration Generation**: Generate SQL statements to transform source schema to target schema
- **Rollback Support**: Generate rollback SQL for migration plans
- **Validation Levels**: STRICT, STANDARD, and LENIENT validation modes
- **Performance Optimized**: Supports schemas with 100+ tables
- **Comprehensive Testing**: 90%+ test coverage with integration tests

## Quick Start

### Prerequisites

- Java 8 or higher
- Gradle (or use the included gradlew wrapper)
- druid-parser library (version 1.2.28-SNAPSHOT)

### Installation

#### Using Gradle

Add the following to your `build.gradle`:

```gradle
repositories {
    mavenLocal()  // Essential for SNAPSHOT dependencies
    mavenCentral()
}

dependencies {
    implementation 'com.aidvps.schemakit:sql-table-differ:1.1.0'
}
```

**Note**: The `mavenLocal()` repository is required to access the SNAPSHOT dependency `druid-parser:1.2.28-SNAPSHOT`.

#### From Source

```bash
git clone https://github.com/aidvps/schema-kit-v2.git
cd schema-kit-v2
./gradlew build
```

### Basic Usage

#### Simple Schema Migration

```java
import com.aidvps.druid.differ.*;
import com.aidvps.druid.differ.internal.model.*;

// Create a TableDiffer with MySQL dialect
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .build();

// Define source and target schemas
String sourceSchema = "CREATE TABLE users (id INT, name VARCHAR(100))";
String targetSchema = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(200), email VARCHAR(255))";

// Generate migration
MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

// Print migration statements
for (String statement : plan.getStatements()) {
    System.out.println(statement);
}
```

**Output**:
```sql
ALTER TABLE users MODIFY COLUMN id INT NOT NULL PRIMARY KEY;
ALTER TABLE users MODIFY COLUMN name VARCHAR(200);
ALTER TABLE users ADD COLUMN email VARCHAR(255);
```

#### PostgreSQL Example

```java
// Create a PostgreSQL differ
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.POSTGRESQL)
    .build();

String sourceSchema = "CREATE TABLE products (id INT, name VARCHAR(100))";
String targetSchema = "CREATE TABLE products (id SERIAL PRIMARY KEY, name VARCHAR(200), price DECIMAL(10,2))";

MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
```

**Output**:
```sql
ALTER TABLE products ALTER COLUMN id SET DEFAULT nextval('products_id_seq'::regclass);
ALTER TABLE products ALTER COLUMN name TYPE VARCHAR(200);
ALTER TABLE products ADD COLUMN price DECIMAL(10,2);
```

#### Oracle Example

```java
// Create an Oracle differ
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.ORACLE)
    .build();

String sourceSchema = "CREATE TABLE employees (id INT, name VARCHAR(100))";
String targetSchema = "CREATE TABLE employees (id NUMBER(10) PRIMARY KEY, name VARCHAR2(200), salary NUMBER(10,2))";

MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
```

**Output**:
```sql
ALTER TABLE employees MODIFY (id NUMBER(10) PRIMARY KEY);
ALTER TABLE employees MODIFY (name VARCHAR2(200));
ALTER TABLE employees ADD (salary NUMBER(10,2));
```

### Advanced Usage

#### Custom Migration Options

```java
// Configure migration options
MigrationOptions options = MigrationOptions.builder()
    .wrapInTransaction(true)
    .includeComments(true)
    .failOnDestructive(true)
    .includeRollback(true)
    .build();

// Create differ with custom options
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .withOptions(options)
    .withValidationLevel(ValidationLevel.STRICT)
    .build();

// Generate migration
String sourceSchema = "CREATE TABLE users (id INT)";
String targetSchema = "CREATE TABLE users (id INT PRIMARY KEY, email VARCHAR(255))";

try {
    MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

    // Print warnings
    for (Warning warning : plan.getWarnings()) {
        System.out.println("Warning [" + warning.getSeverity() + "]: " + warning.getMessage());
    }

    // Generate rollback
    List<String> rollback = differ.generateRollback(plan);
} catch (SchemaCompatibilityException e) {
    System.err.println("Migration validation failed: " + e.getMessage());
}
```

#### Multi-Table Schema with Foreign Keys

```java
TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .build();

String sourceSchema =
    "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100));" +
    "CREATE TABLE posts (id INT PRIMARY KEY, user_id INT, title VARCHAR(200));";

String targetSchema =
    "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(255));" +
    "CREATE TABLE posts (id INT PRIMARY KEY, user_id INT, title VARCHAR(200), content TEXT);" +
    "ALTER TABLE posts ADD CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(id);";

MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

// Statements will be properly ordered:
// 1. Add email column to users
// 2. Add content column to posts
// 3. Add foreign key constraint (after both tables are updated)
```

#### Validation Levels

```java
// STRICT: Fails on any warning or destructive operation
TableDiffer strictDiffer = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .withValidationLevel(ValidationLevel.STRICT)
    .build();

// STANDARD: Generates warnings for destructive operations (default)
TableDiffer standardDiffer = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .withValidationLevel(ValidationLevel.STANDARD)
    .build();

// LENIENT: Downgrades warnings to INFO level
TableDiffer lenientDiffer = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .withValidationLevel(ValidationLevel.LENIENT)
    .build();
```

#### Performance Testing

```java
// Generate a large schema for testing
StringBuilder source = new StringBuilder();
StringBuilder target = new StringBuilder();

for (int i = 1; i <= 100; i++) {
    source.append("CREATE TABLE table_").append(i)
          .append(" (id INT, name VARCHAR(100));");
    target.append("CREATE TABLE table_").append(i)
          .append(" (id INT PRIMARY KEY, name VARCHAR(200), created_at TIMESTAMP);");
}

TableDiffer differ = TableDiffer.builder()
    .withDialect(DatabaseDialect.MYSQL)
    .build();

long start = System.currentTimeMillis();
MigrationPlan plan = differ.generateMigration(source.toString(), target.toString());
long duration = System.currentTimeMillis() - start;

System.out.println("Migration generated in " + duration + "ms");
System.out.println("Total statements: " + plan.getStatements().size());
```

**Expected Performance**: 100 tables in < 5 seconds

## Supported Database Dialects

| Dialect | Status | Features |
|---------|--------|----------|
| MySQL 8.0+ | ✅ Full Support | MODIFY COLUMN, AUTO_INCREMENT, ENGINE clause |
| PostgreSQL 13+ | ✅ Full Support | ALTER COLUMN TYPE, IDENTITY, USING clause, UUID, JSONB |
| Oracle 19c+ | ✅ Full Support | VARCHAR2, NUMBER, CLOB, GENERATED ALWAYS AS IDENTITY |

## Migration Plan

The `MigrationPlan` class provides comprehensive information about the migration:

```java
MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);

// Access all information
Schema sourceSchema = plan.getSourceSchema();
Schema targetSchema = plan.getTargetSchema();
DatabaseDialect dialect = plan.getDatabaseDialect();
List<String> statements = plan.getStatements();
List<Warning> warnings = plan.getWarnings();
String schemaHash = plan.getSchemaHash();
```

## Warning Types

| Type | Severity | Description |
|------|----------|-------------|
| DATA_LOSS_RISK | ERROR | Operations that delete data (DROP TABLE, DROP COLUMN) |
| COMPATIBILITY_WARNING | WARN | Potential compatibility issues (type changes) |
| DEPRECATED_SYNTAX | INFO | Use of deprecated syntax |
| PERFORMANCE_NOTE | INFO | Performance-related information |

## API Reference

### TableDiffer.Builder

```java
TableDiffer builder()
    .withDialect(DatabaseDialect dialect)           // Set database dialect
    .withOptions(MigrationOptions options)          // Set migration options
    .withValidationLevel(ValidationLevel level)     // Set validation level
    .build();                                       // Create TableDiffer instance
```

### MigrationOptions.Builder

```java
MigrationOptions options = MigrationOptions.builder()
    .wrapInTransaction(true)        // Wrap statements in transaction
    .includeComments(true)          // Include comments in SQL
    .failOnDestructive(true)        // Fail on destructive operations
    .includeRollback(true)          // Generate rollback SQL
    .dryRun(false)                  // Validate without generating SQL
    .timeout(30, TimeUnit.SECONDS)  // Set timeout
    .build();
```

## Troubleshooting

### Build Failures

**Error**: `Could not find com.aidvps:druid-parser:1.2.28-SNAPSHOT`

**Solution**: Ensure `mavenLocal()` is configured in your `build.gradle` repositories:

```gradle
repositories {
    mavenLocal()
    mavenCentral()
}
```

### Maven Local Not Configured

If you need to build the druid-parser from source:

```bash
git clone https://github.com/aidvps/druid.git
cd druid/core
mvn clean install -DskipTests
```

Then rebuild your project.

### Schema Parsing Errors

**Error**: `SchemaParsingException: Unexpected token`

**Solution**: Ensure your SQL schema:
- Uses standard CREATE TABLE syntax
- Includes proper semicolons between statements
- Has matching parentheses

### Validation Failures

**Error**: `SchemaCompatibilityException: Strict validation failed`

**Solution**: Either:
1. Use `ValidationLevel.LENIENT` or `ValidationLevel.STANDARD`
2. Review and approve destructive operations manually
3. Use `MigrationOptions.builder().failOnDestructive(false)`

### Performance Issues

For large schemas (>100 tables):
- Use `ValidationLevel.STANDARD` instead of `STRICT`
- Ensure adequate heap space: `-Xmx2g`
- Consider running in dry-run mode for validation only

### Test Failures

Run tests with:

```bash
./gradlew test
```

Integration tests require Docker for Testcontainers.

## Examples

See the `src/test/java` directory for comprehensive examples:
- `MultiDialectIntegrationTest.java` - Multi-dialect examples
- `MySQLMigrationGeneratorTest.java` - MySQL-specific tests
- `PostgreSQLMigrationGeneratorTest.java` - PostgreSQL-specific tests
- `OracleMigrationGeneratorTest.java` - Oracle-specific tests
- `PerformanceTest.java` - Performance benchmarks

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Ensure Spotless formatting: `./gradlew spotlessCheck`
6. Submit a pull request

## License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

## Support

- Issues: [GitHub Issues](https://github.com/aidvps/schema-kit-v2/issues)
- Documentation: [Project Wiki](https://github.com/aidvps/schema-kit-v2/wiki)
- Email: support@aidvps.com
