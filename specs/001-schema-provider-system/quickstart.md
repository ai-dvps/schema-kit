# Quick Start Guide: Schema Provider System

**Version**: 1.0.0 | **Date**: 2025-12-01 | **Module**: Multi-module Gradle Project

## Overview

This guide helps you get started with the Schema Provider System, a multi-source database schema comparison and migration generation tool supporting 5 source types and 4 database platforms.

## Prerequisites

- Java 8 or higher
- Gradle 7.0 or higher
- Git (for git-based providers)

## Project Structure

The project is organized as a multi-module Gradle build:

```
schema-kit-v2/
├── build.gradle                          # Root build configuration
├── settings.gradle                       # Module declarations
│
├── schema-core/                          # Core schema model
│   └── src/main/java/...
│
├── schema-provider-api/                  # Provider interfaces
│   └── src/main/java/...
│
├── schema-provider-dir/                  # Directory provider (P1)
│   └── src/main/java/...
│
├── schema-provider-db/                   # Database provider (P2)
│   └── src/main/java/...
│
├── schema-provider-git/                  # Git provider (P3)
│   └── src/main/java/...
│
├── schema-provider-jar/                  # JAR provider (P4)
│   └── src/main/java/...
│
└── schema-migrator/                      # Migration generator
    └── src/main/java/...
```

## Setup

### 1. Build the Project

```bash
# From project root
./gradlew build

# Or for a specific module
./gradlew :schema-core:build
```

### 2. Add Dependencies

If using as a library in another project:

```gradle
dependencies {
    implementation 'com.aidvps.schemakit:schema-core:1.0.0'
    implementation 'com.aidvps.schemakit:schema-provider-api:1.0.0'
    implementation 'com.aidvps.schemakit:schema-provider-dir:1.0.0'
    implementation 'com.aidvps.schemakit:schema-migrator:1.0.0'
}
```

---

## Usage Examples

### Example 1: Directory to Directory Migration

Compare schemas from two directories and generate migration SQL.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.schemakit.migrator.*;

// Create directory providers
SchemaProvider sourceProvider = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);
SchemaProvider targetProvider = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);

// Configure source
DirectorySchemaProviderConfig sourceConfig = DirectorySchemaProviderConfig.builder()
    .path(Paths.get("schemas/v1.0"))
    .validateStructure(true)
    .encoding("UTF-8")
    .build();

// Configure target
DirectorySchemaProviderConfig targetConfig = DirectorySchemaProviderConfig.builder()
    .path(Paths.get("schemas/v2.0"))
    .validateStructure(true)
    .encoding("UTF-8")
    .build();

// Get schemas
Schema sourceSchema = sourceProvider.getSchema(sourceConfig);
Schema targetSchema = targetProvider.getSchema(targetConfig);

// Generate migration
SchemaMigrator migrator = new DefaultSchemaMigrator();
MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.MYSQL)
    .mode(MigrationMode.FULL)
    .includeDrops(true)
    .transactional(true)
    .build();

MigrationScript migration = migrator.generateMigration(sourceSchema, targetSchema, config);

// Print SQL
System.out.println(migration.toFormattedSql());
```

### Example 2: Live Database Comparison

Compare production database with desired state from files.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.schemakit.migrator.*;
import javax.sql.DataSource;

// Create providers
SchemaProvider prodProvider = SchemaProviderFactory.createProvider(ProviderType.DATABASE);
SchemaProvider fileProvider = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);

// Configure production database
DatabaseSchemaProviderConfig prodConfig = DatabaseSchemaProviderConfig.builder()
    .dataSource(productionDataSource)
    .includeDatabases("production_db")
    .credentialProvider(new EnvironmentVariableSecretProvider())
    .build();

// Configure file-based desired state
DirectorySchemaProviderConfig desiredConfig = DirectorySchemaProviderConfig.builder()
    .path(Paths.get("desired-state/schemas"))
    .validateStructure(true)
    .build();

// Get schemas
Schema prodSchema = prodProvider.getSchema(prodConfig);
Schema desiredSchema = fileProvider.getSchema(desiredConfig);

// Compare
SchemaMigrator migrator = new DefaultSchemaMigrator();
SchemaDiff diff = migrator.compare(prodSchema, desiredSchema);

if (diff.hasChanges()) {
    System.out.println("Schema changes detected:");
    diff.getChanges().forEach(change -> {
        System.out.println("  " + change.getType() + ": " + change.getDescription());
    });

    // Generate migration
    MigrationScript migration = migrator.generateMigration(prodSchema, desiredSchema, config);
    System.out.println("\nMigration SQL:");
    System.out.println(migration.toSql());
}
```

### Example 3: Git Repository Migration

Compare schemas between git branches.

```java
// Create git provider
SchemaProvider gitProvider = SchemaProviderFactory.createProvider(ProviderType.GIT);

// Configure main branch (current)
GitSchemaProviderConfig mainConfig = GitSchemaProviderConfig.builder()
    .repositoryUrl("https://github.com/company/database-schemas.git")
    .reference("main")
    .credentials(GitCredentials.builder()
        .username("deploy-user")
        .password(System.getenv("GIT_TOKEN"))
        .build())
    .build();

// Configure feature branch (desired)
GitSchemaProviderConfig featureConfig = GitSchemaProviderConfig.builder()
    .repositoryUrl("https://github.com/company/database-schemas.git")
    .reference("feature/user-management")
    .credentials(GitCredentials.builder()
        .username("deploy-user")
        .password(System.getenv("GIT_TOKEN"))
        .build())
    .build();

// Get schemas
Schema mainSchema = gitProvider.getSchema(mainConfig);
Schema featureSchema = gitProvider.getSchema(featureConfig);

// Generate PostgreSQL migration
MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.POSTGRESQL)
    .mode(MigrationMode.FORWARD_ONLY)
    .includeDrops(false)
    .build();

MigrationScript migration = migrator.generateMigration(mainSchema, featureSchema, config);

// Write to file
Files.write(Paths.get("migration.sql"), migration.toSql().getBytes());
```

### Example 4: JAR-Embedded Schema Comparison

Compare schemas packaged in JAR files.

```java
// Create JAR provider
SchemaProvider jarProvider = SchemaProviderFactory.createProvider(ProviderType.JAR);

// Configure current version JAR
JarSchemaProviderConfig currentConfig = JarSchemaProviderConfig.builder()
    .jarPath("schemas-current.jar")
    .basePath("schemas/")
    .classLoader(Thread.currentThread().getContextClassLoader())
    .build();

// Configure new version JAR
JarSchemaProviderConfig newConfig = JarSchemaProviderConfig.builder()
    .jarPath("schemas-v2.jar")
    .basePath("schemas/")
    .classLoader(Thread.currentThread().getContextClassLoader())
    .build();

// Get schemas
Schema currentSchema = jarProvider.getSchema(currentConfig);
Schema newSchema = jarProvider.getSchema(newConfig);

// Compare and generate migration
SchemaMigrator migrator = new DefaultSchemaMigrator();
MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.SQLITE)
    .mode(MigrationMode.FULL)
    .includeDrops(true)
    .build();

MigrationScript migration = migrator.generateMigration(currentSchema, newSchema, config);
```

### Example 5: Custom Provider Implementation

Create a provider for a cloud database service.

```java
import com.aidvps.schemakit.provider.*;

public class CloudDatabaseProvider implements SchemaProvider {
    private final CloudDatabaseClient client;

    public CloudDatabaseProvider(CloudDatabaseClient client) {
        this.client = client;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) {
        CloudDatabaseConfig cloudConfig = (CloudDatabaseConfig) config;

        // Fetch schema from cloud service
        CloudSchema cloudSchema = client.getSchema(
            cloudConfig.getInstanceId(),
            cloudConfig.getDatabaseName()
        );

        // Convert to internal model
        return CloudSchemaMapper.toSchema(cloudSchema);
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }

    @Override
    public void validateConfig(SchemaProviderConfig config) throws SchemaProviderException {
        CloudDatabaseConfig cloudConfig = (CloudDatabaseConfig) config;
        if (cloudConfig.getInstanceId() == null) {
            throw new ConfigValidationException("Instance ID required");
        }
    }
}

// Register custom provider
CloudDatabaseClient client = new CloudDatabaseClient("api-key");
SchemaProvider customProvider = new CloudDatabaseProvider(client);

SchemaProviderFactory.registerProvider(ProviderType.CUSTOM, customProvider);

// Use it
SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.CUSTOM);
Schema schema = provider.getSchema(cloudConfig);
```

---

## File Format

### Directory Structure

Schemas are organized as:

```
schemas/
├── mydatabase/
│   ├── mydatabase.db        # Database creation
│   ├── users.tbl            # Table creation
│   ├── products.tbl         # Table creation
│   └── orders.tbl           # Table creation
└── analytics/
    ├── analytics.db
    └── events.tbl
```

### .db File (Database)

```sql
-- mydatabase.db
CREATE DATABASE IF NOT EXISTS mydatabase
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### .tbl File (Table)

```sql
-- users.tbl
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_email_domain CHECK (email LIKE '%@%.%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## Configuration Reference

### Directory Provider

```java
DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .path(Paths.get("/path/to/schemas"))    // Required: Directory path
    .validateStructure(true)                 // Optional: Validate file structure (default: true)
    .encoding("UTF-8")                       // Optional: File encoding (default: UTF-8)
    .build();
```

### Database Provider

```java
DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
    .dataSource(dataSource)                  // Required: JDBC DataSource
    .includeDatabases("db1", "db2")          // Optional: Include only these databases
    .excludeDatabases("temp")                // Optional: Exclude these databases
    .credentialProvider(provider)            // Optional: Secret provider
    .build();
```

### Git Provider

```java
GitSchemaProviderConfig config = GitSchemaProviderConfig.builder()
    .repositoryUrl("https://...")            // Required: Git URL
    .reference("main")                       // Required: Branch, tag, or commit
    .localPath(Paths.get("/tmp/repo"))       // Optional: Local checkout path
    .credentials(creds)                      // Optional: Authentication
    .build();
```

### JAR Provider

```java
JarSchemaProviderConfig config = JarSchemaProviderConfig.builder()
    .jarPath("schemas.jar")                  // Required: JAR path or classpath pattern
    .basePath("schemas/")                    // Optional: Base path in JAR (default: "")
    .classLoader(cl)                         // Optional: ClassLoader (default: thread context)
    .build();
```

---

## Migration Configuration

```java
MigrationConfig config = MigrationConfig.builder()
    .targetPlatform(DatabasePlatform.MYSQL)  // Required: Target platform
    .mode(MigrationMode.FULL)                // Optional: FULL, FORWARD_ONLY, DIFF_ONLY
    .includeDrops(true)                      // Optional: Include DROP statements (default: true)
    .transactional(true)                     // Optional: Wrap in transactions (default: true)
    .customGenerator(generator)              // Optional: Custom SQL generator
    .build();
```

---

## Credential Management

### Environment Variables

```java
SecretProvider provider = new EnvironmentVariableSecretProvider();

// Use in database config
String dbPassword = provider.getSecret("DB_PASSWORD");
```

### Custom Secret Manager

```java
public class VaultSecretProvider implements SecretProvider {
    private final VaultClient vault;

    @Override
    public String getSecret(String key) {
        return vault.read("secret/" + key);
    }

    @Override
    public boolean hasSecret(String key) {
        return vault.exists("secret/" + key);
    }
}
```

---

## Error Handling

```java
try {
    SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);
    Schema schema = provider.getSchema(config);
} catch (SchemaProviderException e) {
    switch (e.getErrorCode()) {
        case SOURCE_NOT_FOUND:
            System.err.println("Schema source not found: " + e.getMessage());
            break;
        case PARSE_ERROR:
            System.err.println("Failed to parse schema: " + e.getMessage());
            break;
        case AUTHENTICATION_FAILED:
            System.err.println("Authentication failed");
            break;
        default:
            System.err.println("Unknown error: " + e.getMessage());
    }
}
```

---

## Best Practices

### 1. Validation

Always enable structure validation during development:

```java
DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .path(path)
    .validateStructure(true)  // Always validate in development
    .build();
```

### 2. Error Handling

Wrap provider calls in try-catch blocks and handle specific error codes.

### 3. Resource Management

Git providers create temporary directories - ensure cleanup:

```java
try (GitRepository repo = GitRepository.clone(url, ref)) {
    // Use repo
} // Automatically cleaned up
```

### 4. Performance

- Enable lazy loading for large schemas
- Use connection pooling for database providers
- Cache parsed schemas when possible

### 5. Security

- Never log credentials
- Use environment variables for sensitive data
- Implement proper timeout handling

---

## Testing

Run tests:

```bash
# All modules
./gradlew test

# Specific module
./gradlew :schema-provider-dir:test

# Integration tests
./gradlew :schema-provider-db:integrationTest
```

### Testcontainers

Integration tests use Testcontainers for real database testing:

```java
@SpringBootTest
class DatabaseProviderIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void testSchemaExtraction() {
        // Test schema extraction from live MySQL
    }
}
```

---

## Troubleshooting

### Issue: "Schema source not found"

**Solution**: Check path exists and is accessible:
```java
if (!Files.exists(path)) {
    throw new SchemaProviderException("Directory not found: " + path);
}
```

### Issue: "Authentication failed"

**Solution**: Verify credentials:
```java
// Test credential retrieval
try {
    String password = provider.getSecret("DB_PASSWORD");
    if (password == null) {
        throw new ConfigValidationException("DB_PASSWORD not found in environment");
    }
} catch (SecretNotFoundException e) {
    throw new ConfigValidationException("Missing secret: " + e.getKey());
}
```

### Issue: "Parser error on SQL file"

**Solution**: Validate SQL syntax:
```java
// Enable validation
DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .path(path)
    .validateStructure(true)  // This validates SQL syntax
    .build();
```

### Issue: "Migration generation timeout"

**Solution**: Optimize schema size or increase timeout:
```java
// Use streaming for large schemas
SchemaProvider provider = ...;
try (SchemaStream stream = provider.streamSchema(config)) {
    stream.forEach(schema -> {
        // Process incrementally
    });
}
```

---

## Support

- **Documentation**: See `/docs` directory
- **API Reference**: JavaDoc in each module
- **Issues**: GitHub issues for bugs and feature requests
- **Discussions**: GitHub discussions for questions

---

## License

Apache 2.0 - see LICENSE file
