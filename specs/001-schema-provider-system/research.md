# Research: Schema Provider System Implementation

**Date**: 2025-12-01 | **Feature**: Schema Provider System | **Status**: Complete

## Research Summary

This document consolidates research findings for implementing the schema provider system with multi-source support.

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

## Technology Stack Summary

| Category | Technology | Rationale |
|----------|-----------|-----------|
| Build Tool | Gradle | Already in use, multi-module support |
| Language | Java 8 | Maintain compatibility with existing codebase |
| SQL Parser | druid-parser | Already in dependencies, full DDL support |
| Database Access | JDBC | Standard API, all target DBs supported |
| Git Integration | JGit | Native Java, no native deps |
| Testing | JUnit 5, Mockito, Testcontainers | Standard Java testing stack |
| Credentials | Custom provider + env vars | Flexible, secure |

---

## Risks and Mitigations

1. **SQL Parsing Complexity**: Use proven druid-parser library
2. **Database Dialect Differences**: Abstract dialect logic, extensive testing
3. **Git Repository Size**: Implement size limits and cleanup
4. **Credential Security**: Never log, use secure memory, clear after use
5. **Performance at Scale**: Lazy loading, streaming for large schemas
