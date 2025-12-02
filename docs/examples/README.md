# Usage Examples

This directory contains comprehensive usage examples for schema-kit-v2.

## Basic Examples

### 1. Directory Provider

**Location**: `schema-provider-dir/src/test/java/com/aidvps/schemakit/provider/dir/`

- `DirectorySchemaProviderContractTest.java` - Contract tests
- `DirectoryFileParsingIntegrationTest.java` - File parsing
- `DatabaseFileParserTest.java` - .db file parsing
- `TableFileParserTest.java` - .tbl file parsing
- `E2EDirectoryToMigrationTest.java` - End-to-end example

### 2. Database Provider

**Location**: `schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/`

- `DatabaseSchemaProviderContractTest.java` - Contract tests
- `MySQLIntegrationTest.java` - MySQL with Testcontainers
- `PostgreSQLIntegrationTest.java` - PostgreSQL with Testcontainers
- `MariaDBIntegrationTest.java` - MariaDB with Testcontainers
- `SQLiteIntegrationTest.java` - SQLite integration
- `LiveDatabaseMigrationE2ETest.java` - End-to-end example

### 3. Git Provider

**Location**: `schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/`

- `GitSchemaProviderContractTest.java` - Contract tests
- `GitRepositoryCloningTest.java` - Repository cloning
- `GitBranchCheckoutIntegrationTest.java` - Branch operations
- `GitTagReferenceTest.java` - Tag references
- `GitCommitReferenceTest.java` - Commit references
- `GitToMigrationE2ETest.java` - End-to-end example

### 4. JAR Provider

**Location**: `schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/`

- `JarSchemaProviderContractTest.java` - Contract tests
- `JarResourceLoaderTest.java` - JAR resource loading
- `EmbeddedSchemaExtractionTest.java` - Schema extraction
- `MultipleDatabasesInJarTest.java` - Multiple databases
- `JarToMigrationE2ETest.java` - End-to-end example

### 5. Custom Provider

**Location**: `schema-provider-api/src/test/java/com/aidvps/schemakit/provider/`

- `CustomProviderRegistrationTest.java` - Registration
- `CustomProviderIntegrationTest.java` - Integration
- `CustomProviderIsolationTest.java` - Isolation
- `CustomProviderE2ETest.java` - End-to-end
- `MixedSourceTypesTest.java` - Mixed sources

### 6. Migration Examples

**Location**: `schema-migrator/src/test/java/com/aidvps/schemakit/migrator/`

- `SchemaComparisonAccuracyTest.java` - Schema accuracy
- `CrossDialectMigrationTest.java` - Cross-dialect migration
- `DirectoryToDatabaseMigrationTest.java` - Directory to database
- `GitToDatabaseMigrationTest.java` - Git to database
- `JarToDirectoryMigrationTest.java` - JAR to directory
- `DatabaseToGitMigrationTest.java` - Database to git

## Performance Tests

**Location**: `tests/performance/`

- `MigrationPerformanceTest.java` - SC-001 performance tests
- `SchemaAccuracyTest.java` - SC-003 accuracy tests
- `LargeSchemaTest.java` - Large schema handling
- `MemoryUsageTest.java` - Memory usage validation

## Pattern Examples

### File Structure Patterns

#### Directory Structure

```
schemas/
└── mydb/
    ├── mydb.db
    ├── users.tbl
    ├── orders.tbl
    └── products.tbl
```

**mydb.db**:
```sql
CREATE DATABASE mydb;
```

**users.tbl**:
```sql
CREATE TABLE users (
  id INT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  email VARCHAR(255),
  created_at TIMESTAMP
);
```

### Code Patterns

#### Pattern 1: Simple Migration

```java
DirectorySchemaProvider provider = new DirectorySchemaProvider();
DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .path(Paths.get("/path/to/schemas"))
    .build();

Schema schema = provider.getSchema(config);

DefaultSchemaMigrator migrator = new DefaultSchemaMigrator();
SchemaDiff diff = migrator.compareSchemas(sourceSchema, targetSchema);
MigrationScript migration = migrator.generateMigration(diff, MigrationMode.ALTER);

System.out.println(migration.getScript());
```

#### Pattern 2: Environment-Specific Configuration

```java
// Production config
DatabaseSchemaProviderConfig prodConfig = DatabaseSchemaProviderConfig.builder()
    .dataSource(productionDataSource)
    .includeDatabases("production")
    .credentialProvider(new EnvironmentVariableSecretProvider())
    .build();

// Staging config
DatabaseSchemaProviderConfig stagingConfig = DatabaseSchemaProviderConfig.builder()
    .dataSource(stagingDataSource)
    .includeDatabases("staging")
    .credentialProvider(new EnvironmentVariableSecretProvider())
    .build();
```

#### Pattern 3: Custom Provider

```java
public class DynamoDbProvider implements SchemaProvider {
    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        DynamoDbConfig dbConfig = (DynamoDbConfig) config;

        // Extract schema from DynamoDB
        List<String> tables = listTables(dbConfig);

        Map<String, Table> tableMap = new HashMap<>();
        for (String tableName : tables) {
            TableInfo info = getTableInfo(tableName, dbConfig);
            tableMap.put(tableName, info);
        }

        return Schema.builder()
            .platform(DatabasePlatform.CUSTOM)
            .database(Database.builder()
                .name(dbConfig.getDatabaseName())
                .tables(tableMap)
                .build())
            .build();
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }
}
```

#### Pattern 4: CI/CD Integration

```java
public class MigrationGenerator {
    public static void main(String[] args) throws Exception {
        String sourceRef = args[0];  // e.g., "main"
        String targetRef = args[1];  // e.g., "feature/new-schema"

        // Load source schema from git
        GitSchemaProviderConfig sourceConfig = GitSchemaProviderConfig.builder()
            .repositoryUrl(System.getenv("GIT_REPO_URL"))
            .reference(sourceRef)
            .credentials(GitCredentials.builder()
                .username(System.getenv("GIT_USERNAME"))
                .password(System.getenv("GIT_TOKEN"))
                .build())
            .build();

        GitSchemaProvider sourceProvider = new GitSchemaProvider();
        Schema sourceSchema = sourceProvider.getSchema(sourceConfig);

        // Load target schema
        GitSchemaProviderConfig targetConfig = GitSchemaProviderConfig.builder()
            .repositoryUrl(System.getenv("GIT_REPO_URL"))
            .reference(targetRef)
            .credentials(GitCredentials.builder()
                .username(System.getenv("GIT_USERNAME"))
                .password(System.getenv("GIT_TOKEN"))
                .build())
            .build();

        GitSchemaProvider targetProvider = new GitSchemaProvider();
        Schema targetSchema = targetProvider.getSchema(targetConfig);

        // Generate migration
        DefaultSchemaMigrator migrator = new DefaultSchemaMigrator();
        SchemaDiff diff = migrator.compareSchemas(sourceSchema, targetSchema);
        MigrationScript migration = migrator.generateMigration(diff, MigrationMode.FULL);

        // Write to file
        Files.write(Paths.get("migration.sql"), migration.getScript().getBytes());
    }
}
```

#### Pattern 5: Testing Custom Providers

```java
@Test
void testMyCustomProvider() throws Exception {
    MyCustomProvider provider = new MyCustomProvider();
    MyCustomConfig config = MyCustomConfig.builder()
        .sourceUrl("http://example.com/api/schema")
        .apiKey("test-key")
        .build();

    // Use TestKit for validation
    CustomProviderTestKit.testProvider(provider)
        .withValidConfig(config)
        .withInvalidConfig(invalidConfig)
        .runAllTests();

    // Verify schema correctness
    Schema schema = provider.getSchema(config);
    assertNotNull(schema);
    assertEquals(1, schema.getDatabases().size());

    // Use Validator
    CustomProviderValidator validator = new CustomProviderValidator();
    CustomProviderValidator.ValidationResult result = validator.validate(provider);

    assertFalse(result.hasErrors(), "Should have no errors: " + result.getErrors());
}
```

## Best Practices

### 1. Always Validate Configuration

```java
try {
    DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
        .path(path)
        .validateStructure(true)
        .build();

    config.validate();  // Throws ConfigValidationException if invalid
} catch (ConfigValidationException e) {
    // Handle validation errors
    System.err.println("Invalid configuration: " + e.getMessage());
}
```

### 2. Use Environment Variables for Credentials

```java
EnvironmentVariableSecretProvider secretProvider = new EnvironmentVariableSecretProvider();

DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
    .dataSource(dataSource)
    .credentialProvider(secretProvider)
    .build();
```

### 3. Test Custom Providers Thoroughly

```java
// Use CustomProviderTestKit
CustomProviderTestKit.testProvider(provider)
    .withValidConfig(config)
    .withInvalidConfig(invalidConfig)
    .runAllTests();

// Use CustomProviderValidator
CustomProviderValidator validator = new CustomProviderValidator();
ValidationResult result = validator.validate(provider);
assertFalse(result.hasErrors());
```

### 4. Handle Errors Gracefully

```java
try {
    Schema schema = provider.getSchema(config);
} catch (SchemaProviderException e) {
    switch (e.getErrorCode()) {
        case SOURCE_NOT_FOUND:
            // Handle missing source
            break;
        case CONFIG_INVALID:
            // Handle invalid config
            break;
        case NETWORK_ERROR:
            // Handle network issues
            break;
        default:
            // Handle other errors
    }
}
```

### 5. Use Appropriate Migration Mode

```java
// ALTER mode - for existing databases
MigrationMode.ALTER

// CREATE mode - for new databases
MigrationMode.CREATE

// FULL mode - for complete migrations
MigrationMode.FULL
```

## Running Examples

### Run All Examples

```bash
./gradlew test
```

### Run Specific Provider Examples

```bash
./gradlew test --tests "*DirectorySchemaProvider*"
./gradlew test --tests "*DatabaseSchemaProvider*"
./gradlew test --tests "*GitSchemaProvider*"
./gradlew test --tests "*JarSchemaProvider*"
```

### Run Performance Examples

```bash
./gradlew test --tests "tests.performance.*"
```

## Additional Resources

- **Custom Provider Guide**: `/docs/CUSTOM_PROVIDER.md`
- **API Documentation**: `/docs/api/README.md`
- **Quick Start**: `/specs/001-schema-provider-system/quickstart.md`
- **Specification**: `/specs/001-schema-provider-system/spec.md`
