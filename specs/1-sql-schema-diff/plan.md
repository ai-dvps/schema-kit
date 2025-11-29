# Feature Plan: SQL Table Differ

## Overview

The SQL Table Differ enables automated comparison of database table structures across different schema versions and generates database-specific migration SQL to transform one schema into another. This capability reduces manual effort in schema migration and ensures consistent database versioning across environments, essential for CI/CD pipelines and DevOps workflows.

## Constitution Compliance Checklist

- [ ] **Code Style**: All code conforms to Google Java Style Guide with Spotless formatting
- [ ] **SOLID Principles**: Classes follow Single Responsibility, proper dependency inversion
- [ ] **Unit Testing**: 90% line coverage with JUnit 5 and Mockito for all public methods
- [ ] **Integration Testing**: API contract changes tested with Testcontainers
- [ ] **API Design**: Backward compatibility maintained, internal packages marked
- [ ] **Documentation**: JavaDoc for all public APIs with examples
- [ ] **Performance**: JMH benchmarks for performance-critical operations
- [ ] **Resource Management**: Try-with-resources for all I/O, thread-safe collections

## Technical Approach

### Architecture Design

The SQL Table Differ will follow a layered architecture with clear separation of concerns:

**1. Parser Layer (FR-001)**
- **DruidParserAdapter**: Wrapper around com.aidvps:druid-parser to handle SQL parsing
- **SchemaExtractor**: Extracts structured schema data from druid-parser AST objects
- **DatabaseDialectResolver**: Determines appropriate SQL dialect for parsing (MySQL, PostgreSQL, Oracle)

**2. Model Layer (FR-002, FR-006)**
- **Schema**: Immutable representation of complete database structure
- **Table**: Table metadata with columns, constraints, and relationships
- **Column**: Column definition with type, constraints, and properties
- **SchemaDiff**: Immutable difference model capturing all changes between two schemas
- **ChangeDetector**: Analyzes schemas and produces SchemaDiff object

**3. Generator Layer (FR-003, FR-004)**
- **MigrationGenerator**: Factory for database-specific SQL generators
- **MySQLMigrationGenerator**: MySQL-specific SQL generation
- **PostgreSQLMigrationGenerator**: PostgreSQL-specific SQL generation
- **OracleMigrationGenerator**: Oracle-specific SQL generation
- **DependencyAnalyzer**: Validates statement ordering and dependency constraints

**4. Core Orchestration Layer**
- **TableDiffer**: Main facade class coordinating parsing, comparison, and generation
- **MigrationPlan**: Immutable migration plan with ordered statements and warnings

### Component Design

```
┌─────────────────────────────────────┐
│        TableDiffer (Facade)         │
└──────────────┬──────────────────────┘
               │
    ┌──────────┼──────────┐
    │          │          │
┌───▼───┐  ┌────┐  ┌─────▼─────┐
│Parser │  │    │  │  Generator│
│ Layer │  │Diff│  │  Layer    │
└───────┘  │    │  └───────────┘
           └────┘
```

**Key Classes**:
- `com.aidvps.druid.differ.TableDiffer`: Main public API
- `com.aidvps.druid.differ.internal.parser`: Internal package for parsing logic
- `com.aidvps.druid.differ.internal.model`: Internal package for schema models
- `com.aidvps.druid.differ.internal.generator`: Internal package for SQL generation

### API Design

**Public Interface**:

```java
public final class TableDiffer {
    // Builder for creating TableDiffer instances
    public static class Builder {
        public Builder withDialect(DatabaseDialect dialect);
        public Builder withValidationLevel(ValidationLevel level);
        public TableDiffer build();
    }

    // Generate migration SQL from source to target schema
    public MigrationPlan generateMigration(String sourceSchema, String targetSchema);

    // Generate migration with specific options
    public MigrationPlan generateMigration(String sourceSchema, String targetSchema,
                                           MigrationOptions options);

    // Generate rollback SQL from migration plan
    public String generateRollback(MigrationPlan plan);
}
```

**Domain Models**:

```java
public final class MigrationPlan {
    private final List<String> statements;
    private final List<Warning> warnings;
    private final List<DestructiveOperation> destructiveOperations;

    // Immutable getters
    public List<String> getStatements() { ... }
    public List<Warning> getWarnings() { ... }
    public boolean hasDestructiveOperations() { ... }
}

public enum DatabaseDialect {
    MYSQL, POSTGRESQL, ORACLE
}

public final class MigrationOptions {
    private final boolean wrapInTransaction;
    private final boolean includeComments;
    private final boolean failOnDestructive;

    // Builder pattern for options
}
```

### Dependency Management

**Gradle Configuration**:

```gradle
repositories {
    // Check local Maven repository first for SNAPSHOT dependencies
    mavenLocal()
    // Fall back to central repository
    mavenCentral()
}

dependencies {
    // Druid Parser (1.2.28-SNAPSHOT from local Maven repository)
    implementation 'com.aidvps:druid-parser:1.2.28-SNAPSHOT'

    // JUnit 5 for testing
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.0'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.0'

    // Mockito for unit testing
    testImplementation 'org.mockito:mockito-core:5.7.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.7.0'

    // JMH for performance benchmarks
    jmhImplementation 'org.openjdk.jmh:jmh-core:1.36'
    jmhAnnotationProcessor 'org.openjdk.jmh:jmh-generator-annprocess:1.36'

    // Testcontainers for integration testing
    testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
    testImplementation 'org.testcontainers:mysql:1.19.0'
    testImplementation 'org.testcontainers:postgresql:1.19.0'
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```

**Note**: `mavenLocal()` is essential for accessing SNAPSHOT dependencies like `druid-parser:1.2.28-SNAPSHOT`.

**Java Version**: 8 (minimum requirement)

### Error Handling Strategy

**Exception Hierarchy**:

```
TableDifferException (base)
├── SchemaParsingException (invalid SQL input)
├── SchemaCompatibilityException (incompatible schemas)
└── GenerationException (SQL generation failure)
```

All exceptions include:
- Clear error messages with actionable guidance
- Contextual information (line numbers, column positions)
- Suggestions for resolution

### Performance Optimization

- **Lazy Parsing**: Parse schemas on-demand, cache parsed results
- **Incremental Comparison**: Compare schemas using hash-based equality checks first
- **Statement Ordering**: Pre-compute dependency graph to avoid runtime validation costs
- **Memory Efficiency**: Use immutable models with structural sharing where possible

## Risks and Mitigation

- **Risk**: Druid-parser API changes breaking compatibility → Mitigation: Wrap parser in adapter with version checks, comprehensive integration tests
- **Risk**: Complex dependency ordering causing migration failures → Mitigation: Topological sort algorithm with cycle detection, extensive edge case testing
- **Risk**: Performance degradation on large schemas → Mitigation: Caching, incremental comparison, JMH benchmarks with regression alerts
- **Risk**: Database dialect-specific SQL syntax errors → Mitigation: Comprehensive testing across all supported dialects, dialect-specific test suites

## Acceptance Criteria

- [ ] Parse CREATE TABLE statements from MySQL, PostgreSQL, and Oracle using druid-parser
- [ ] Detect all schema differences with zero false negatives (FR-002)
- [ ] Generate valid migration SQL for MySQL, PostgreSQL, and Oracle (FR-003)
- [ ] Handle multi-table schemas with foreign key relationships (FR-006)
- [ ] Validate dependency ordering and warn about destructive operations (FR-004)
- [ ] Complete schema comparison for 100 tables within 5 seconds (Success Criteria)
- [ ] Generate migration SQL that successfully transforms source to target schema (100% accuracy)
- [ ] Provide clear error messages for all failure scenarios (FR-005)

## Testing Strategy

**Unit Tests** (Target: 90% coverage)
- SchemaParserTest: Test parsing of various CREATE TABLE statements
- SchemaComparatorTest: Test difference detection algorithms
- MigrationGeneratorTest: Test SQL generation for each database dialect
- DependencyAnalyzerTest: Test statement ordering logic
- ErrorHandlingTest: Test exception scenarios and error messages

**Integration Tests**
- MultiDialectTest: Verify behavior across MySQL, PostgreSQL, Oracle
- SchemaEvolutionTest: End-to-end schema migration scenarios
- ForeignKeyTest: Multi-table schema comparison and migration
- PerformanceTest: Verify <5 second performance for 100 tables

**Performance Tests** (JMH)
- SchemaParsingBenchmark: Measure parsing performance
- SchemaComparisonBenchmark: Measure comparison speed
- SQLGenerationBenchmark: Measure SQL generation time
- EndToEndBenchmark: Full pipeline performance

## Documentation Plan

**JavaDoc Coverage**
- TableDiffer class with usage examples
- MigrationPlan and related classes
- All public methods with parameter/return documentation
- DatabaseDialect enum with supported features

**README Updates**
- Quick start guide with code examples
- API documentation with migration examples
- Supported database dialects and features
- Common pitfalls and troubleshooting guide

**Additional Documentation**
- Architecture design document
- Database dialect-specific SQL generation notes
- Migration best practices guide

## Release Plan

- **Version Bump**: 1.0.0 (initial release)
- **Deprecation Notices**: N/A (initial release)
- **Migration Timeline**: N/A (initial release)
- **Breaking Changes**: None (maintain backward compatibility from v1.0.0)

## Implementation Phases

### Phase 0: Research & Foundation
- Research druid-parser AST structure and capabilities
- Define schema model classes and relationships
- Research database-specific SQL dialect differences
- Establish testing framework and benchmarks

### Phase 1: Core Implementation
- Implement SchemaParser and DruidParserAdapter
- Implement core model classes (Schema, Table, Column, SchemaDiff)
- Implement ChangeDetector for schema comparison
- Implement basic MySQL migration generator
- Unit test coverage to 90%

### Phase 2: Enhanced Features
- Implement PostgreSQL and Oracle generators
- Implement DependencyAnalyzer for statement ordering
- Add multi-table schema support with foreign keys
- Integration testing with Testcontainers
- JMH performance benchmarks

### Phase 3: Polish & Documentation
- Error handling refinements and custom exception types
- Documentation updates and code examples
- Performance optimization and memory profiling
- Final validation against all success criteria

## Dependencies and External Integrations

**Required**
- com.aidvps:druid-parser library for SQL parsing
- Java 8+ runtime environment

**Optional**
- Testcontainers for integration testing (dev/test only)
- Maven/Gradle for build and dependency management

**Internal Dependencies**
- None (initial library with no dependencies on other project modules)
