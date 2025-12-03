# Research: Schema Provider System Implementation

**Date**: 2025-12-04 (Updated) | **Feature**: Schema Provider System | **Status**: ✅ Complete - Implementation Analyzed

## Research Summary

This document consolidates research findings and implementation decisions for the Schema Provider System. The system is fully implemented with 7 modules, supporting 5 source types (directory, live database, git repository, JAR file, custom), 4 database platforms (MySQL, PostgreSQL, MariaDB, SQLite), comprehensive testing via Testcontainers, and SPI-based extensibility.

---

## 1. Gradle Multi-Module Best Practices

**Decision**: Adopt hierarchical multi-module structure with common build configuration

**Rationale**: Gradle best practices recommend:
- Root build.gradle with common plugins, dependencies, and configurations
- Individual module build.gradle files for module-specific settings
- settings.gradle listing all modules
- Use of `java-library` plugin for API/impl separation where needed
- Consistent source sets and test configurations across modules

**Alternatives Considered**:
- Flat structure: rejected due to poor scalability
- Single build.gradle: rejected due to hard maintenance
- Composite builds: overkill for single project

---

## 2. SQL Parser Options for .db/.tbl Files

**Decision**: Use existing druid-parser library (already in dependencies) for SQL parsing

**Rationale**:
- druid-parser is already included in project dependencies (com.aidvps:druid-parser:1.2.28-SNAPSHOT)
- Proven SQL parser with CREATE DATABASE and CREATE TABLE support
- Supports MySQL, PostgreSQL, MariaDB, SQLite dialects
- Can extract complete DDL including columns, constraints, indexes, foreign keys
- Java 8 compatible

**Alternatives Considered**:
- JSQLParser: Additional dependency, similar capability
- Custom parser: High maintenance burden, limited dialect support
- SQLLineage: More complex than needed

**Implementation Strategy**:
- Parse .db files with CREATE DATABASE statements
- Parse .tbl files with CREATE TABLE statements
- Extract schema elements into internal model

---

## 3. Database Metadata Extraction

**Decision**: Use JDBC DatabaseMetadata API with SQL dialect abstraction

**Rationale**:
- Standard JDBC API available in Java 8
- Works with all target databases (MySQL, PostgreSQL, MariaDB, SQLite)
- Can extract: databases, tables, columns, primary keys, foreign keys, indexes
- Testcontainers integration for testing

**Implementation Strategy**:
- DatabaseProvider uses JDBC connection
- DatabaseMetadata to introspect schema
- Dialect-specific SQL generation for platform compatibility
- Support for multiple databases on single connection

---

## 4. JGit Integration Patterns

**Decision**: Use JGit library for git repository access

**Rationale**:
- Native Java git library, no native dependencies
- Supports clone, checkout, branch/tag references
- Works with local and remote repositories
- Java 8 compatible

**Implementation Strategy**:
- Clone repository to temporary location
- Checkout specific branch/tag/commit
- Access schema files from checked out directory
- Clean up temporary files after processing

---

## 5. JAR Resource Loading Patterns

**Decision**: Use ClassLoader.getResource() and getResourceAsStream()

**Rationale**:
- Standard Java API for accessing classpath resources
- Works with JAR files and filesystem
- No additional dependencies
- Thread-safe when properly managed

**Implementation Strategy**:
- Use Thread.currentThread().getContextClassLoader()
- Load resources as InputStream
- Parse .db/.tbl files from JAR classpath
- Support multiple database directories in single JAR

---

## 6. Credential Management Integration

**Decision**: Support both environment variables and pluggable secret manager

**Rationale**:
- Environment variables: universal, simple, no dependencies
- External secret managers: enterprise security, centralized management
- Pluggable interface allows custom implementations

**Implementation Strategy**:
- SecretProvider interface with getSecret(String key) method
- Built-in EnvironmentVariableSecretProvider
- Pluggable implementations for Vault, AWS Secrets Manager, etc.
- Fallback hierarchy: explicit config → environment → secret manager

---

## 7. Migration Generation Algorithm

**Decision**: Three-phase comparison: detect differences, order dependencies, generate SQL

**Rationale**:
- Database-level migrations require proper ordering
- Foreign key dependencies must be respected
- Drop operations last, create operations first
- Alter operations in dependency order

**Implementation Strategy**:
1. **Diff Detection**: Compare schemas element-by-element
   - New databases, dropped databases
   - New tables, dropped tables, modified tables
   - Column additions, removals, modifications
   - Index/constraint changes

2. **Dependency Analysis**: Build dependency graph
   - Foreign key dependencies between tables
   - Database usage dependencies
   - Circular dependency detection

3. **Generation**: Generate SQL in correct order
   - Create databases
   - Create tables (in dependency order)
   - Add constraints and indexes
   - Alter existing tables
   - Drop constraints and indexes
   - Drop tables (reverse order)
   - Drop databases

---

## 8. Schema Model Design

**Decision**: Immutable model objects with builder pattern

**Rationale**:
- Immutable objects thread-safe and predictable
- Builder pattern for complex object construction
- Clear separation between schema definition and provider implementation
- Supports validation at construction time

**Core Entities**:
- Schema: Root object containing databases
- Database: Contains tables, represents CREATE DATABASE
- Table: Contains columns, constraints, indexes
- Column: Name, type, nullable, default, etc.
- Constraint: Primary key, foreign key, unique, check
- Index: Name, columns, unique flag

---

## 9. Configuration API Design

**Decision**: Fluent builder API with validation

**Rationale**:
- Type-safe configuration at compile time
- IDE autocomplete and documentation
- Validation prevents runtime errors
- Supports all provider types uniformly

**Example**:
```java
SchemaProvider dirProvider = DirectorySchemaProvider.builder()
    .path("/path/to/schemas")
    .validateStructure(true)
    .build();

SchemaProvider dbProvider = DatabaseSchemaProvider.builder()
    .connection(dataSource)
    .includeDatabases("prod", "staging")
    .build();
```

---

## 10. Testing Strategy

**Decision**: Three-layer testing: unit, integration, contract

**Rationale**:
- Unit tests: Fast, isolated, development-time feedback
- Integration tests: Real database behavior, Testcontainers
- Contract tests: API compatibility between modules

**Implementation**:
- JUnit 5 for all tests
- Mockito for mocking
- Testcontainers for MySQL, PostgreSQL, MariaDB, SQLite
- Given-When-Then BDD style
- Coverage target: 80% minimum

---

## 11. SPI Implementation: SchemaProviderFactory

**Decision**: ConcurrentHashMap-based provider registry with automatic registration

**Implementation Details**:
```java
public class SchemaProviderFactory {
    private static final Map<String, SchemaProvider> PROVIDERS = new ConcurrentHashMap<>();

    public static void registerProvider(String id, SchemaProvider provider) {
        // Thread-safe registration with duplicate validation
    }

    public static SchemaProvider createProvider(String id) {
        return PROVIDERS.get(id);
    }

    // Automatic discovery via META-INF/services files
    static {
        ServiceLoader<SchemaProvider> loader = ServiceLoader.load(SchemaProvider.class);
        loader.forEach(factory -> registerProvider(
            factory.getProviderId(), factory));
    }
}
```

**Features**:
- Thread-safe concurrent hashmap
- Automatic registration on class load
- Manual registration for custom providers
- Duplicate ID detection
- No synchronization required

**SPI Registration** (META-INF/services/com.aidvps.schemakit.provider.SchemaProvider):
```
com.aidvps.schemakit.provider.dir.DirectorySchemaProvider
com.aidvps.schemakit.provider.db.DatabaseSchemaProvider
com.aidvps.schemakit.provider.git.GitSchemaProvider
com.aidvps.schemakit.provider.jar.JarSchemaProvider
```

---

## 12. Testing Organization: Multi-Layer Test Strategy

**Decision**: 41 test files organized by module and test type

**Test Structure by Module**:

1. **schema-provider-api** (4 test files):
   - Contract tests for provider API compliance
   - Factory SPI registration tests
   - Secret provider tests

2. **schema-provider-dir** (3 test files):
   - Contract validation tests
   - File parsing integration tests
   - End-to-end directory-to-migration flow

3. **schema-provider-db** (5 test files):
   - Contract tests
   - MySQL integration (Testcontainers)
   - PostgreSQL integration (Testcontainers)
   - MariaDB integration (Testcontainers)
   - SQLite integration (Testcontainers)

4. **schema-provider-git** (5 test files):
   - Contract validation
   - Repository cloning behavior
   - Branch checkout integration
   - Commit reference handling
   - Tag reference handling

5. **schema-core** & **schema-migrator** (24+ test files):
   - JMH performance benchmarks
   - Schema comparison accuracy
   - Migration generation tests
   - Multi-dialect integration

**Testing Technologies**:
- JUnit 5.10.0 with platform runner
- Mockito 4.11.0 for mocking
- Testcontainers 1.19.2 for real databases
- Jacoco 0.8.11 for coverage reporting
- Spotless for code formatting (Google Java Format 1.7)

**Test Patterns**:
```java
// Contract test pattern
@ExtendWith(MockitoExtension.class)
class DirectorySchemaProviderContractTest {
    private DirectorySchemaProvider provider;

    @Test
    @DisplayName("Should load schema from valid directory structure")
    void shouldLoadValidDirectory() {
        // Test implementation
    }
}

// Integration test pattern with Testcontainers
@SpringBootTest
class MySQLIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Test
    void shouldExtractSchemaFromMySQL() {
        // Test with real MySQL instance
    }
}
```

---

## 13. Exception Hierarchy Design

**Decision**: Centralized exception handling with error codes

**Hierarchy**:
```java
SchemaProviderException (base)
├── ConfigValidationException
├── SourceNotFoundException
├── AuthenticationException
├── ParseException
└── MigrationException
```

**Error Codes**:
- SOURCE_NOT_FOUND: Path/URL doesn't exist
- ACCESS_DENIED: Permission denied
- INVALID_CONFIG: Missing required configuration
- PARSE_ERROR: SQL parsing failure
- AUTH_FAILED: Authentication failed
- UNSUPPORTED_OPERATION: Feature not supported

---

## 14. Connection Pooling Strategy

**Decision**: HikariCP 5.0.1 for all database connections

**Rationale**:
- Industry-standard connection pool
- High performance (fastest in benchmarks)
- Minimal dependencies
- Java 8 compatible
- Proper resource cleanup

**Implementation**:
```java
HikariDataSource dataSource = new HikariDataSource();
dataSource.setJdbcUrl(url);
dataSource.setUsername(username);
dataSource.setPassword(password);
dataSource.setMaximumPoolSize(10);
dataSource.setConnectionTimeout(30000);
```

---

## 15. Platform-Specific Introspection Patterns

**Decision**: Specialized introspectors per database platform

**Implementation** (schema-provider-db module):
```
introspector/
├── PlatformIntrospector.java         (base interface)
├── MySQLIntrospector.java             (MySQL 5.7+, 8.0+)
├── PostgreSQLIntrospector.java        (PostgreSQL 9.1+)
├── MariaDBIntrospector.java           (MariaDB 10.0+)
└── SQLiteIntrospector.java            (SQLite 3.x)
```

**Key Differences**:
- MySQL: AUTO_INCREMENT, ENUM types, ENGINE clauses
- PostgreSQL: SERIAL types, custom domains
- MariaDB: Similar to MySQL with additional features
- SQLite: No ALTER TABLE ADD COLUMN, no ALTER COLUMN

---

## Technology Stack Summary

| Category | Technology | Version | Rationale |
|----------|-----------|---------|-----------|
| Build Tool | Gradle | 7.x | Multi-module support, Java 8 toolchain |
| Language | Java | 8 | Constitution requirement, source/target compatibility |
| SQL Parser | druid-parser | 1.2.28-SNAPSHOT | Comprehensive DDL support, multi-dialect |
| Git Integration | Eclipse JGit | 5.13.1.202206130422-r | Pure Java, no native dependencies |
| Connection Pool | HikariCP | 5.0.1 | High performance, production-tested |
| Testing | JUnit | 5.10.0 | Modern test framework with BOM |
| Mocking | Mockito | 4.11.0 | Standard Java mocking framework |
| Integration Tests | Testcontainers | 1.19.2 | Real database instances, Docker-based |
| Code Coverage | JaCoCo | 0.8.11 | Gradle integration, HTML reports |
| Code Formatting | Spotless | 5.17.0 | Google Java Format 1.7 |

---

## Risks and Mitigations

1. **SQL Parsing Complexity**: Use proven druid-parser library with comprehensive dialect support
2. **Database Dialect Differences**: Abstract dialect logic via PlatformIntrospector hierarchy, extensive Testcontainers testing
3. **Git Repository Size**: Implement size limits, shallow clones, automatic cleanup of temporary repositories
4. **Credential Security**: EnvironmentVariableSecretProvider interface, never log credentials, secure memory handling
5. **Performance at Scale**: Lazy loading, streaming for large schemas, bounded memory by schema size
6. **Multi-Platform Testing**: Testcontainers ensures compatibility across MySQL, PostgreSQL, MariaDB, SQLite
7. **Provider Discovery**: Java ServiceLoader ensures automatic discovery without manual registration
8. **Configuration Validation**: Builder pattern with compile-time type safety and runtime validation

---

## Module Dependency Graph

```
schema-core (no dependencies)
  ↑
schema-provider-api (depends on schema-core)
  ↑
  ├── schema-provider-dir (depends on api, core)
  ├── schema-provider-db (depends on api, core)
  ├── schema-provider-git (depends on api, core)
  └── schema-provider-jar (depends on api, core)
  ↑
schema-migrator (depends on core, api)
```

**Dependency Rules**:
- schema-core: No external dependencies (library-first)
- schema-provider-api: Only schema-core
- Provider modules: schema-provider-api + schema-core
- schema-migrator: schema-core + schema-provider-api
- No circular dependencies
- All modules: Java 8 compatible

---

## Implementation Metrics

- **Total Source Files**: 101
- **Total Test Files**: 41
- **Test Coverage**: Configured via Jacoco (80% minimum target)
- **Database Platforms**: 4 (MySQL, PostgreSQL, MariaDB, SQLite)
- **Provider Types**: 5 (Directory, Database, Git, JAR, Custom)
- **Modules**: 7 (core, api, 4 providers, migrator)
- **Build Time**: ~2 minutes for full build
- **Integration Tests**: 5 database-specific test suites
- **Performance**: < 30 seconds for schema comparison (constitution requirement)
