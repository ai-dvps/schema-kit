# Implementation Plan: Schema Provider System

**Branch**: `[001-schema-provider-system]` | **Date**: 2025-12-01 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-schema-provider-system/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement a multi-source schema provider system supporting 5 source types (directory, live database, git, JAR, custom) with database-level migration generation. Refactor existing single-module project into multi-module Gradle structure. Support MySQL, PostgreSQL, MariaDB, and SQLite platforms.

## Technical Context

**Language/Version**: Java 8
**Primary Dependencies**: Gradle, JUnit 5, Mockito, Testcontainers (MySQL, PostgreSQL, MariaDB, SQLite)
**Storage**: File-based (.db/.tbl) and database metadata
**Testing**: JUnit 5, Mockito for unit tests, Testcontainers for integration tests
**Target Platform**: JVM (multi-platform)
**Project Type**: Multi-module Java library
**Performance Goals**: 30-second migration generation (SC-001), 100% schema accuracy (SC-003)
**Constraints**: Java 8 compatibility, programmatic API configuration, environment variable/secret manager support
**Scale/Scope**: Support 5 source types, 4 database platforms

## Constitution Check

**GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.**

✅ **Library-First Architecture**: Schema provider will be its own module with clear interfaces
✅ **Test-First Development**: TDD cycle will be followed for all 5 user stories
✅ **Multi-Module Gradle Structure**: Refactor current single-module to multi-module (existing, enhanced)
✅ **API Design**: Programmatic API with stable interfaces for all provider types
✅ **Database Dialect Support**: Supports MySQL, PostgreSQL, MariaDB, SQLite with dialect awareness
✅ **Performance Standards**: SC-001 (30 seconds) and SC-003 (100% accuracy) targets defined
✅ **CLI and Programmatic Access**: Programmatic API as primary, CLI wrapper later

## Project Structure

### Documentation (this feature)

```text
specs/001-schema-provider-system/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Multi-module Gradle structure
root (schema-kit-v2)
├── settings.gradle                    # Declares all modules
├── build.gradle                       # Root build with common config
│
├── schema-core/                       # Core schema model and interfaces
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/core/
│       └── test/java/com/aidvps/schemakit/core/
│
├── schema-provider-api/               # Schema provider interfaces
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/provider/
│       └── test/java/com/aidvps/schemakit/provider/
│
├── schema-provider-dir/               # Directory-based provider (P1)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/provider/dir/
│       └── test/java/com/aidvps/schemakit/provider/dir/
│
├── schema-provider-db/                # Live database provider (P2)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/provider/db/
│       └── test/java/com/aidvps/schemakit/provider/db/
│
├── schema-provider-git/               # Git repository provider (P3)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/provider/git/
│       └── test/java/com/aidvps/schemakit/provider/git/
│
├── schema-provider-jar/               # JAR embedded provider (P4)
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/provider/jar/
│       └── test/java/com/aidvps/schemakit/provider/jar/
│
├── schema-migrator/                   # Migration generation engine
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/aidvps/schemakit/migrator/
│       └── test/java/com/aidvps/schemakit/migrator/
│
└── schema-cli/                        # CLI wrapper (future)
    └── build.gradle (future)
```

**Structure Decision**: Multi-module Gradle project with 7 modules:
1. **schema-core**: Core schema model (Database, Table, Column, Constraint, etc.)
2. **schema-provider-api**: Interfaces and contracts for all providers
3. **schema-provider-dir**: P1 - Directory-based file provider
4. **schema-provider-db**: P2 - Live database connection provider
5. **schema-provider-git**: P3 - Git repository provider
6. **schema-provider-jar**: P4 - JAR embedded provider
7. **schema-migrator**: Database-level migration generation and comparison

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. Multi-module structure aligns with Constitution III.

## Phases

### Phase 0: Research & Planning ✅ COMPLETE
- [x] Research Gradle multi-module best practices
- [x] Research SQL parser options for .db/.tbl files
- [x] Research database metadata extraction for MySQL, PostgreSQL, MariaDB, SQLite
- [x] Research JGit integration patterns
- [x] Research JAR resource loading patterns
- [x] Research credential management integration (env vars, secret managers)
**Output**: research.md created with all findings consolidated

### Phase 1: Design & Contracts ✅ COMPLETE
- [x] Design schema-core data model (Database, Table, Column, Constraint, Index, ForeignKey)
- [x] Design SchemaProvider interface with provider lifecycle
- [x] Design configuration API for all provider types
- [x] Design migration generation algorithm (database-level)
- [x] Design error handling and validation patterns
- [x] Create OpenAPI/interface contracts
- [x] Create quickstart guide
**Outputs**: data-model.md, contracts/, quickstart.md, agent context updated

## Post-Design Constitution Re-Check

✅ **Library-First Architecture**: Confirmed - all modules are independent libraries with clear interfaces
✅ **Test-First Development**: Confirmed - TDD cycle defined, unit/integration tests planned
✅ **Multi-Module Gradle Structure**: Confirmed - 7-module structure defined with minimal coupling
✅ **API Design**: Confirmed - stable interfaces with builder pattern, versioned
✅ **Database Dialect Support**: Confirmed - MySQL, PostgreSQL, MariaDB, SQLite with proper abstraction
✅ **Performance Standards**: Confirmed - 30-second target, 100% accuracy in research.md
✅ **CLI and Programmatic Access**: Confirmed - programmatic API primary, CLI wrapper future

### Phase 2: Implementation Tasks
- [ ] Refactor project to multi-module Gradle structure
- [ ] Implement schema-core module
- [ ] Implement schema-provider-api module
- [ ] Implement schema-provider-dir (P1)
- [ ] Implement schema-provider-db (P2)
- [ ] Implement schema-provider-git (P3)
- [ ] Implement schema-provider-jar (P4)
- [ ] Implement custom provider support
- [ ] Implement schema-migrator module
- [ ] Generate unit tests for all modules
- [ ] Generate integration tests with Testcontainers
- [ ] Performance validation against SC-001 (30 seconds)
- [ ] Accuracy validation against SC-003 (100%)
