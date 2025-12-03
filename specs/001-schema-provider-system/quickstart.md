# Quick Start Guide: Schema Provider System

**Version**: 1.1.0 | **Date**: 2025-12-04 | **Module**: Multi-module Gradle Project

## Overview

This guide helps you get started with the Schema Provider System, a multi-source database schema comparison and migration generation tool supporting 5 source types and 4 database platforms with SPI-based architecture.

## Prerequisites

- Java 8 or higher
- Gradle 7.0 or higher
- Git (for git-based providers)
- Docker (for Testcontainers integration tests)

## Project Structure

The project is organized as a multi-module Gradle build:

```
schema-kit-v2/
├── build.gradle                          # Root build configuration
├── settings.gradle                       # Module declarations
│
├── schema-core/                          # Core schema model & differ engine
│   └── src/main/java/com/aidvps/druid/differ/
│       ├── internal/model/               # Schema, Database, Table models
│       └── differ/                       # Schema comparison engine
│
├── schema-provider-api/                  # Provider interfaces & SPI
│   └── src/main/java/com/aidvps/schemakit/provider/
│       ├── SchemaProvider.java           # Core provider interface
│       ├── SchemaProviderFactory.java    # SPI-based factory
│       ├── SchemaProviderConfig.java     # Base configuration
│       └── BuiltInProviders.java         # Provider ID constants
│
├── schema-provider-dir/                  # Directory-based provider
│   └── src/main/java/com/aidvps/schemakit/provider/dir/
│       ├── DirectorySchemaProvider.java
│       ├── DirectorySchemaProviderConfig.java
│       └── parsers/
│
├── schema-provider-db/                   # Live database provider
│   └── src/main/java/com/aidvps/schemakit/provider/db/
│       ├── DatabaseSchemaProvider.java
│       ├── DatabaseSchemaProviderConfig.java
│       └── introspector/                 # MySQL, PostgreSQL, MariaDB, SQLite
│
├── schema-provider-git/                  # Git repository provider
│   └── src/main/java/com/aidvps/schemakit/provider/git/
│       ├── GitSchemaProvider.java
│       └── GitSchemaProviderConfig.java
│
├── schema-provider-jar/                  # JAR-embedded provider
│   └── src/main/java/com/aidvps/schemakit/provider/jar/
│       ├── JarSchemaProvider.java
│       └── JarSchemaProviderConfig.java
│
└── schema-migrator/                      # Migration generation engine
    └── src/main/java/com/aidvps/schemakit/migrator/
```

## Setup

### 1. Build the Project

```bash
# From project root
./gradlew build

# Or for a specific module
./gradlew :schema-core:build

# Run tests (including Testcontainers integration tests)
./gradlew test
```

### 2. Add Dependencies

If using as a library in another project:

```gradle
dependencies {
    implementation 'com.aidvps.schemakit:schema-core:1.1.0'
    implementation 'com.aidvps.schemakit:schema-provider-api:1.1.0'
    implementation 'com.aidvps.schemakit:schema-provider-dir:1.1.0'
    implementation 'com.aidvps.schemakit:schema-migrator:1.1.0'
}
```

---

## Usage Examples

### Example 1: Directory to Directory Migration

Compare schemas from two directories and generate migration SQL.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.schemakit.provider.dir.*;
import com.aidvps.druid.differ.internal.model.*;
import java.nio.file.Paths;

// Create directory provider
SchemaProvider provider = SchemaProviderFactory.createProvider("directory");

// Configure source schema
DirectorySchemaProviderConfig sourceConfig = DirectorySchemaProviderConfig.builder()
    .directoryPath("schemas/v1.0")
    .validateStructure(true)
    .build();

// Configure target schema
DirectorySchemaProviderConfig targetConfig = DirectorySchemaProviderConfig.builder()
    .directoryPath("schemas/v2.0")
    .validateStructure(true)
    .build();

// Get schemas
Schema sourceSchema = provider.getSchema(sourceConfig);
Schema targetSchema = provider.getSchema(targetConfig);

// Compare schemas
System.out.println("Source tables: " + sourceSchema.getTableCount());
System.out.println("Target tables: " + targetSchema.getTableCount());

// Check for specific tables
if (targetSchema.hasTable("products")) {
    System.out.println("Products table exists in target");
}
```

### Example 2: Live Database Comparison

Compare production database with desired state from files.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.schemakit.provider.db.*;
import com.aidvps.schemakit.provider.dir.*;
import com.aidvps.druid.differ.internal.model.*;
import java.sql.Connection;
import java.sql.DriverManager;

// Create providers
SchemaProvider dbProvider = SchemaProviderFactory.createProvider("database");
SchemaProvider fileProvider = SchemaProviderFactory.createProvider("directory");

// Configure production database connection
Connection prodConnection = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/production", "user", "password");

DatabaseSchemaProviderConfig prodConfig = DatabaseSchemaProviderConfig.builder()
    .connection(prodConnection)
    .platform(DatabasePlatform.MYSQL)
    .build();

// Configure file-based desired state
DirectorySchemaProviderConfig desiredConfig = DirectorySchemaProviderConfig.builder()
    .directoryPath("desired-state/schemas")
    .validateStructure(true)
    .build();

// Get schemas
Schema prodSchema = dbProvider.getSchema(prodConfig);
Schema desiredSchema = fileProvider.getSchema(desiredConfig);

// Compare
System.out.println("Production databases: " + prodSchema.getDatabaseCount());
System.out.println("Desired databases: " + desiredSchema.getDatabaseCount());

// Access specific database
if (prodSchema.hasDatabase("production_db")) {
    Database db = prodSchema.getDatabase("production_db");
    System.out.println("Production tables: " + db.getTableCount());
}

// Close connection
prodConnection.close();
```

### Example 3: Git Repository Migration

Compare schemas between git branches.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.schemakit.provider.git.*;
import com.aidvps.druid.differ.internal.model.*;
import java.nio.file.Paths;

// Create git provider
SchemaProvider gitProvider = SchemaProviderFactory.createProvider("git");

// Configure main branch (current)
GitSchemaProviderConfig mainConfig = GitSchemaProviderConfig.builder()
    .repositoryPath("https://github.com/company/database-schemas.git")
    .branch("main")
    .build();

// Configure feature branch (desired)
GitSchemaProviderConfig featureConfig = GitSchemaProviderConfig.builder()
    .repositoryPath("https://github.com/company/database-schemas.git")
    .reference("feature/user-management")
    .build();

// Get schemas
Schema mainSchema = gitProvider.getSchema(mainConfig);
Schema featureSchema = gitProvider.getSchema(featureConfig);

// Compare
System.out.println("Main branch tables: " + mainSchema.getTableCount());
System.out.println("Feature branch tables: " + featureSchema.getTableCount());

// Check for new tables in feature branch
featureSchema.getDatabases().forEach(db -> {
    if (!mainSchema.hasDatabase(db.getName())) {
        System.out.println("New database: " + db.getName());
    }
});
```

### Example 4: JAR-Embedded Schema Comparison

Compare schemas packaged in JAR files.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.schemakit.provider.jar.*;
import com.aidvps.druid.differ.internal.model.*;

// Create JAR provider
SchemaProvider jarProvider = SchemaProviderFactory.createProvider("jar");

// Configure current version JAR
JarSchemaProviderConfig currentConfig = JarSchemaProviderConfig.builder()
    .jarPath("schemas-current.jar")
    .resourcePath("schemas/")
    .extractToTemporary(true)
    .validateJar(true)
    .build();

// Configure new version JAR
JarSchemaProviderConfig newConfig = JarSchemaProviderConfig.builder()
    .jarPath("schemas-v2.jar")
    .resourcePath("schemas/")
    .extractToTemporary(true)
    .validateJar(true)
    .build();

// Get schemas
Schema currentSchema = jarProvider.getSchema(currentConfig);
Schema newSchema = jarProvider.getSchema(newConfig);

// Compare
System.out.println("Current JAR version tables: " + currentSchema.getTableCount());
System.out.println("New JAR version tables: " + newSchema.getTableCount());

// Access schema from JAR
if (currentSchema.hasDatabase("appdb")) {
    Database appdb = currentSchema.getDatabase("appdb");
    System.out.println("Tables in appdb: " + appdb.getTableCount());
}
```

### Example 5: Custom Provider Implementation

Create a provider for a cloud database service.

```java
import com.aidvps.schemakit.provider.*;
import com.aidvps.druid.differ.internal.model.*;

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
    public String getProviderId() {
        return "cloud-database";
    }

    @Override
    public void validateConfig(SchemaProviderConfig config) throws SchemaProviderException {
        CloudDatabaseConfig cloudConfig = (CloudDatabaseConfig) config;
        if (cloudConfig.getInstanceId() == null) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.CONFIG_INVALID,
                "Instance ID required");
        }
    }
}

// Register custom provider
CloudDatabaseClient client = new CloudDatabaseClient("api-key");
SchemaProvider customProvider = new CloudDatabaseProvider(client);

SchemaProviderFactory.registerProvider("cloud-database", customProvider);

// Use it
SchemaProvider provider = SchemaProviderFactory.createProvider("cloud-database");
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
    .directoryPath("/path/to/schemas")       // Required: Directory path (String)
    .validateStructure(true)                 // Optional: Validate structure (default: true)
    .followSymlinks(false)                   // Optional: Follow symlinks (default: false)
    .dialect(DatabaseDialect.MYSQL)          // Optional: SQL dialect (default: MYSQL)
    .build();
```

### Database Provider

```java
DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
    .connection(connection)                  // Required: JDBC Connection
    .platform(DatabasePlatform.MYSQL)        // Required: Database platform
    .jdbcUrl("jdbc:mysql://...")             // Optional: JDBC URL
    .build();
```

### Git Provider

```java
GitSchemaProviderConfig config = GitSchemaProviderConfig.builder()
    .repositoryPath("https://...")           // Required: Git URL or local path
    .branch("main")                          // Optional: Branch name (default: "main")
    .reference("v1.0")                       // Optional: Tag/commit (takes precedence over branch)
    .credentials(creds)                      // Optional: GitCredentials
    .cloneToTemporary(true)                  // Optional: Clone to temp (default: true)
    .dialect(DatabaseDialect.MYSQL)          // Optional: SQL dialect (default: MYSQL)
    .build();
```

### JAR Provider

```java
JarSchemaProviderConfig config = JarSchemaProviderConfig.builder()
    .jarPath("schemas.jar")                  // Required: JAR path
    .resourcePath("schemas/")                // Optional: Resource path in JAR (default: null)
    .extractToTemporary(true)                // Optional: Extract to temp (default: true)
    .validateJar(true)                       // Optional: Validate JAR (default: true)
    .dialect(DatabaseDialect.MYSQL)          // Optional: SQL dialect (default: MYSQL)
    .build();
```

### Provider Discovery

```java
// List all registered providers
Collection<String> providerIds = SchemaProviderFactory.getRegisteredProviderIds();
System.out.println("Available providers: " + providerIds);

// Check if provider exists
if (SchemaProviderFactory.isProviderRegistered("database")) {
    SchemaProvider provider = SchemaProviderFactory.createProvider("database");
}

// Register custom provider
SchemaProviderFactory.registerProvider("my-custom", customProvider);
```

---

## Error Handling

```java
import com.aidvps.schemakit.provider.*;

try {
    SchemaProvider provider = SchemaProviderFactory.createProvider("directory");
    DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
        .directoryPath("/path/to/schemas")
        .build();

    Schema schema = provider.getSchema(config);
} catch (SchemaProviderException e) {
    switch (e.getErrorCode()) {
        case CONFIG_INVALID:
            System.err.println("Invalid configuration: " + e.getMessage());
            break;
        case SOURCE_NOT_FOUND:
            System.err.println("Schema source not found: " + e.getMessage());
            break;
        case SOURCE_INACCESSIBLE:
            System.err.println("Cannot access schema source: " + e.getMessage());
            break;
        case PARSE_ERROR:
            System.err.println("Failed to parse schema: " + e.getMessage());
            break;
        case VALIDATION_ERROR:
            System.err.println("Schema validation failed: " + e.getMessage());
            break;
        case AUTHENTICATION_FAILED:
            System.err.println("Authentication failed");
            break;
        case TIMEOUT:
            System.err.println("Operation timed out");
            break;
        default:
            System.err.println("Unknown error: " + e.getMessage());
    }
} catch (IllegalArgumentException e) {
    // Thrown when provider ID is not found
    System.err.println("Provider not found: " + e.getMessage());
}
```

---

## Best Practices

### 1. Provider Discovery

Check available providers before creating them:

```java
// List all registered providers
Collection<String> providerIds = SchemaProviderFactory.getRegisteredProviderIds();
System.out.println("Available providers: " + providerIds);

// Use BuiltInProviders constants for built-in providers
SchemaProvider dirProvider = SchemaProviderFactory.createProvider(BuiltInProviders.DIRECTORY);
```

### 2. Configuration Validation

Always validate configuration before use:

```java
DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .directoryPath("/path/to/schemas")
    .validateStructure(true)
    .build();

// Validate early
try {
    config.validate();
} catch (Exception e) {
    System.err.println("Configuration invalid: " + e.getMessage());
}
```

### 3. Error Handling

Use specific error codes for targeted handling:

```java
try {
    Schema schema = provider.getSchema(config);
} catch (SchemaProviderException e) {
    switch (e.getErrorCode()) {
        case SOURCE_NOT_FOUND:
            // Handle missing source
        case PARSE_ERROR:
            // Handle SQL parsing errors
        case AUTHENTICATION_FAILED:
            // Handle auth errors
    }
}
```

### 4. Resource Management

Close database connections properly:

```java
Connection conn = null;
try {
    conn = DriverManager.getConnection(url, user, pass);
    DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
        .connection(conn)
        .platform(DatabasePlatform.MYSQL)
        .build();
    Schema schema = provider.getSchema(config);
} finally {
    if (conn != null) {
        conn.close();
    }
}
```

### 5. Performance

- Use temporary directory options for Git/JAR providers (enabled by default)
- Enable validation only in development (disable in production for speed)
- Use followSymlinks carefully (disabled by default for security)

### 6. Security

- Never log credentials or sensitive configuration
- Use cloneToTemporary=true for Git providers (default)
- Use extractToTemporary=true for JAR providers (default)
- Validate JAR structure before processing

---

## Testing

Run tests:

```bash
# All modules
./gradlew test

# Specific module
./gradlew :schema-provider-dir:test

# With coverage report
./gradlew jacocoTestReport

# Clean and rebuild
./gradlew clean build
```

### Testcontainers Integration Tests

Integration tests use Testcontainers for real database testing:

```java
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MySQLIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @Test
    void testSchemaExtraction() throws Exception {
        Connection conn = DriverManager.getConnection(
            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());

        DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
            .connection(conn)
            .platform(DatabasePlatform.MYSQL)
            .build();

        SchemaProvider provider = SchemaProviderFactory.createProvider("database");
        Schema schema = provider.getSchema(config);

        conn.close();
    }
}
```

---

## Troubleshooting

### Issue: "Provider not found with ID"

**Solution**: Check available provider IDs and ensure the provider module is on the classpath:
```java
// List all available providers
Collection<String> providerIds = SchemaProviderFactory.getRegisteredProviderIds();
System.out.println("Available providers: " + providerIds);

// Verify module dependency
// Ensure schema-provider-dir is included for directory provider
```

### Issue: "directoryPath must not be null or empty"

**Solution**: Always provide a valid directory path:
```java
DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .directoryPath("/path/to/schemas")  // Must not be null or empty
    .build();
```

### Issue: "connection must not be null"

**Solution**: Provide a valid JDBC connection:
```java
Connection conn = DriverManager.getConnection(url, user, pass);
DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
    .connection(conn)  // Must provide a valid connection
    .platform(DatabasePlatform.MYSQL)
    .build();
```

### Issue: "repositoryPath must not be null or empty"

**Solution**: Provide a valid Git repository URL or path:
```java
GitSchemaProviderConfig config = GitSchemaProviderConfig.builder()
    .repositoryPath("https://github.com/user/repo.git")  // Required
    .branch("main")
    .build();
```

### Issue: "jarPath must not be null or empty"

**Solution**: Provide a valid JAR path:
```java
JarSchemaProviderConfig config = JarSchemaProviderConfig.builder()
    .jarPath("schemas.jar")  // Must not be null or empty
    .build();
```

### Issue: Testcontainers tests failing

**Solution**: Ensure Docker is running and available:
```bash
# Verify Docker is running
docker ps

# Check Testcontainers can connect
# See: https://www.testcontainers.org/
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
