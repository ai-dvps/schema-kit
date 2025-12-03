# Implementation Plan: Schema Provider System

**Branch**: `001-schema-provider-system` | **Date**: 2025-12-04 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-schema-provider-system/spec.md`

**Status**: ✅ Implementation Complete

## Summary

A multi-source database schema provider system enabling schema retrieval from 5 source types (directory, live database, git repository, JAR file, custom) with SPI-based architecture for extensibility. Supports MySQL, PostgreSQL, MariaDB, and SQLite dialects with comprehensive migration generation.

## Technical Context

**Language/Version**: Java 8 (source/target compatibility via Gradle toolchain)
**Primary Dependencies**: druid-parser 1.2.28-SNAPSHOT (SQL parsing), Eclipse JGit 5.13.1 (Git operations), HikariCP 5.0.1 (connection pooling)
**Storage**: File system (.db/.tbl files), JDBC (live databases), Git repositories, JAR archives
**Testing**: JUnit 5.10.0, Mockito 4.11.0, Testcontainers 1.19.2 (MySQL, PostgreSQL, MariaDB, SQLite)
**Target Platform**: JVM (cross-platform), library for integration
**Project Type**: Multi-module Gradle library
**Performance Goals**: Schema comparison within 30 seconds, bounded memory by schema size
**Constraints**: Java 8 compatibility, offline-capable (directory/JAR providers)
**Scale/Scope**: Enterprise-grade schema management, 4 database platforms, 5 provider types

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Library-First Architecture | ✅ PASS | 6 standalone modules: schema-core, schema-provider-api, schema-provider-dir/db/git/jar, schema-migrator |
| II. Test-First Development | ✅ PASS | Comprehensive test suite: contract tests, integration tests with Testcontainers, 41+ test files |
| III. Multi-Module Gradle Structure | ✅ PASS | 7-module build with clear separation: api, core, 4 providers, migrator |
| IV. API Design | ✅ PASS | Stable SPI via SchemaProvider interface, SchemaProviderFactory for discovery |
| V. Database Dialect Support | ✅ PASS | DatabaseDialect enum, platform-specific introspectors isolated per module |
| VI. Performance Standards | ✅ PASS | JMH benchmarks configured, lazy loading for schema retrieval |
| VII. CLI and Programmatic Access | ✅ PASS | Programmatic API complete via SchemaProviderFactory |
| Java 8 Compatibility | ✅ PASS | Gradle toolchain enforces Java 8 source/target |
| Testing Requirements | ✅ PASS | Unit, integration, contract tests; Jacoco configured per module |

## Project Structure

### Documentation (this feature)

```text
specs/001-schema-provider-system/
├── plan.md              # This file - implementation plan
├── spec.md              # Feature specification
├── research.md          # Phase 0 - technology research
├── data-model.md        # Phase 1 - entity model
├── quickstart.md        # Phase 1 - usage guide
└── contracts/           # Phase 1 - API contracts
```

### Source Code (repository root)

```text
schema-kit-v2/
├── schema-core/                    # Core schema model and differ engine
│   └── src/main/java/com/aidvps/schemakit/core/
│       ├── model/                  # Schema, Database, Table, Column models
│       ├── differ/                 # Schema comparison engine
│       └── parser/                 # SQL parsing utilities
│
├── schema-provider-api/            # Provider interfaces and SPI
│   └── src/main/java/com/aidvps/schemakit/provider/
│       ├── SchemaProvider.java           # Main provider interface
│       ├── SchemaProviderFactory.java    # SPI-based factory
│       ├── SchemaProviderConfig.java     # Base configuration
│       └── secret/                       # Credential management
│
├── schema-provider-dir/            # Directory-based provider
│   └── src/main/java/com/aidvps/schemakit/provider/dir/
│       ├── DirectorySchemaProvider.java
│       ├── DatabaseFileParser.java
│       └── TableFileParser.java
│
├── schema-provider-db/             # Live database provider
│   └── src/main/java/com/aidvps/schemakit/provider/db/
│       ├── DatabaseSchemaProvider.java
│       ├── DatabaseIntrospector.java
│       └── introspector/           # MySQL, PostgreSQL, MariaDB, SQLite
│
├── schema-provider-git/            # Git repository provider
│   └── src/main/java/com/aidvps/schemakit/provider/git/
│       ├── GitSchemaProvider.java
│       └── GitRepositoryManager.java
│
├── schema-provider-jar/            # JAR-embedded provider
│   └── src/main/java/com/aidvps/schemakit/provider/jar/
│       ├── JarSchemaProvider.java
│       └── JarResourceExtractor.java
│
└── schema-migrator/                # Migration generation
    └── src/main/java/com/aidvps/schemakit/migrator/
        └── MigrationGenerator.java
```

**Structure Decision**: Multi-module Gradle layout with one module per provider type, following constitution principle III. Each provider module depends on schema-provider-api and schema-core.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
