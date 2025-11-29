# Tasks: SQL Table Differ

## Plan Updates (2025-11-29)

**IMPORTANT**: This project uses **Gradle** (not Maven) and requires **Java 8**. The `druid-parser` library is version **1.2.28-SNAPSHOT** and must be accessed via `mavenLocal()` repository.

### Critical Configuration Requirements

```gradle
repositories {
    mavenLocal()  // Essential for SNAPSHOT dependencies
    mavenCentral()
}

dependencies {
    implementation 'com.aidvps:druid-parser:1.2.28-SNAPSHOT'
}
```

**Without `mavenLocal()` configured, the build will fail with "Could not find com.aidvps:druid-parser:1.2.28-SNAPSHOT"**

See [REPOSITORY_CONFIG.md](./REPOSITORY_CONFIG.md) for detailed setup instructions.

---

## Implementation Strategy

### MVP First Approach
- **MVP Scope**: User Story 1 (Development Environment Schema Evolution) with MySQL dialect support
- **Incremental Delivery**: Add PostgreSQL and Oracle support in subsequent phases
- **Parallel Opportunities**: MySQL generator, PostgreSQL generator, and Oracle generator can be developed in parallel after foundational phase

### Independent Test Criteria per User Story
- **US1 (Development)**: Single-table schema comparison and migration generation (MySQL)
- **US2 (Production)**: Multi-table schema with foreign keys and complex constraints
- **US3 (CI/CD)**: Performance validation (<5 seconds for 100 tables)
- **US4 (Drift Detection)**: Comprehensive difference detection with zero false negatives

---

## Phase 1: Setup and Project Initialization

### Project Structure and Build Configuration

- [ ] **T001** Create Gradle project structure following package architecture
  - Location: `build.gradle` and directory structure `src/main/java/com/aidvps/druid/differ/`
  - Tasks:
    - Create `build.gradle` with Gradle configuration and dependencies
    - Set up package structure: internal.parser, internal.model, internal.generator, internal.comparator, exception
    - Create test directory structure matching main package
    - Set up `.editorconfig` and code style configuration
    - Configure Spotless formatter with Google Java Style Guide

- [ ] **T002** [P] Set up Gradle dependencies and repositories in build.gradle
  - Location: `build.gradle`
  - Tasks:
    - Configure repositories: mavenLocal() first, then mavenCentral()
    - Add druid-parser:1.2.28-SNAPSHOT dependency (requires mavenLocal())
    - Add JUnit 5 dependencies (jupiter-api, jupiter-engine)
    - Add Mockito dependencies (core, junit-jupiter)
    - Add JMH dependencies (core, generator-annprocess)
    - Add Testcontainers dependencies
    - Configure Java 8 compatibility (sourceCompatibility/targetCompatibility)
    - Configure test tasks for unit tests, integration tests, and JMH benchmarks
    - Configure Spotless plugin for code formatting
    - Add JaCoCo plugin for code coverage reports

- [ ] **T003** Set up test directory structure and frameworks
  - Location: `src/test/java/com/aidvps/druid/differ/`
  - Tasks:
    - Create test package structure mirroring main code
    - Create test resource directory `src/test/resources/`
    - Add test utilities and base test classes

### Phase 1 Test Criteria
- [ ] Gradle project builds successfully without errors
- [ ] Spotless format check passes
- [ ] Directory structure matches plan.md specification
- [ ] All required dependencies declared in build.gradle
- [ ] mavenLocal() repository configured for SNAPSHOT dependency access
- [ ] druid-parser:1.2.28-SNAPSHOT successfully resolved from local Maven repository

### Additional Setup Tasks

- [ ] **T004** Verify druid-parser SNAPSHOT dependency accessibility
  - Location: `~/.m2/repository/com/aidvps/druid-parser/`
  - Tasks:
    - Check if druid-parser:1.2.28-SNAPSHOT is installed in local Maven repository
    - If not installed: Build and install from /Users/shunyun/workspace/java/druid/core
    - Run `./gradlew dependencies --configuration implementation` to verify resolution
    - Confirm no "Could not find" errors for druid-parser
    - Document installation steps in README for future developers

---

## Phase 2: Foundational Components (Blocking Prerequisites)

### Core Model Classes (Immutable)

- [ ] **T005** Implement DatabaseDialect enum in src/main/java/com/aidvps/druid/differ/DatabaseDialect.java
  - Values: MYSQL, POSTGRESQL, ORACLE
  - Include dialect-specific metadata and capabilities

- [ ] **T006** [P] Implement Column class in src/main/java/com/aidvps/druid/differ/internal/model/Column.java
  - Properties: name, dataType, length, precision, scale, nullable, defaultValue, autoIncrement, comment, constraints
  - Immutable design with builder pattern
  - equals/hashCode for comparison

- [ ] **T007** [P] Implement Constraint base class and subtypes in src/main/java/com/aidvps/druid/differ/internal/model/constraint/
  - Location: Constraint.java, PrimaryKey.java, ForeignKey.java, UniqueConstraint.java, CheckConstraint.java
  - Support column-level and table-level constraints
  - Handle dialect-specific constraint features

- [ ] **T008** [P] Implement Table class in src/main/java/com/aidvps/druid/differ/internal/model/Table.java
  - Properties: name, columns, constraints, indexes, comment
  - Immutable design with builder pattern
  - Validation for column and constraint consistency

- [ ] **T009** [P] Implement Schema class in src/main/java/com/aidvps/druid/differ/internal/model/Schema.java
  - Properties: tables (Map), dialect, version, metadata
  - Immutable design with builder pattern
  - Validation for foreign key references

### Exception Hierarchy

- [ ] **T010** Create exception hierarchy in src/main/java/com/aidvps/druid/differ/exception/
  - Location: TableDifferException.java (base), SchemaParsingException.java, SchemaCompatibilityException.java, GenerationException.java
  - Include line/column information for parsing errors
  - Add clear error messages and resolution suggestions

### Parser Layer Foundation


- [ ] **T011** Implement DatabaseDialectResolver in src/main/java/com/aidvps/druid/differ/internal/parser/DatabaseDialectResolver.java
  - Map DatabaseDialect enum to druid-parser DbType
  - Handle dialect-specific parsing configurations


### Phase 2 Test Criteria
- [ ] All model classes compile and pass basic unit tests
- [ ] Immutable properties enforced (final fields, no setters)
- [ ] equals/hashCode implementations correct for all models
- [ ] Exception hierarchy functional with proper error messages

---

## Phase 3: User Story 1 - Development Environment Schema Evolution [US1]

**Goal**: Enable database developers to compare and migrate simple single-table schemas in development environment

**Scope**: MySQL dialect only, single-table schemas, basic column additions/modifications/deletions

### Parser Integration (US1)

- [ ] **T012** [US1] Implement DruidParserAdapter in src/main/java/com/aidvps/druid/differ/internal/parser/DruidParserAdapter.java
  - Wrap SQLCreateTableParser from druid-parser
  - Handle multiple CREATE TABLE statements
  - Map SQLCreateTableStatement AST to internal Schema model
  - Extract table name, columns, and constraints
  - Handle SQLParseException and provide clear error messages

- [ ] **T015** [US1] Implement SchemaExtractor in src/main/java/com/aidvps/druid/differ/internal/parser/SchemaExtractor.java
  - Convert druid-parser AST objects (SQLCreateTableStatement, SQLColumnDefinition, SQLConstraint) to internal models
  - Handle MySQL-specific features (AUTO_INCREMENT, ENGINE clause, etc.)
  - Extract all column properties: type, nullability, default, constraints
  - Extract table-level constraints: PRIMARY KEY, UNIQUE, FOREIGN KEY

### Schema Comparison (US1)

- [ ] **T016** [US1] Implement SchemaDiff and TableDiff in src/main/java/com/aidvps/druid/differ/internal/model/
  - Location: SchemaDiff.java, TableDiff.java, ColumnDiff.java
  - Represent added/removed/modified tables and columns
  - Track semantic changes (type, nullability, default value)
  - Immutable design

- [ ] **T017** [US1] Implement ChangeDetector in src/main/java/com/aidvps/druid/differ/internal/comparator/ChangeDetector.java
  - Compare two Schema objects and produce SchemaDiff
  - Detect added/removed/modified tables
  - For modified tables: detect added/removed/modified columns
  - Compare column properties: type, nullability, default value, constraints
  - Ignore formatting/whitespace differences

### MySQL Migration Generator (US1)

- [ ] **T018** [US1] Implement MySQLMigrationGenerator in src/main/java/com/aidvps/druid/differ/internal/generator/MySQLMigrationGenerator.java
  - Generate ALTER TABLE ADD COLUMN statements
  - Generate ALTER TABLE DROP COLUMN statements
  - Generate ALTER TABLE MODIFY COLUMN statements (type changes)
  - Generate CREATE TABLE statements
  - Generate DROP TABLE statements
  - Include explanatory comments in SQL output
  - Handle MySQL-specific syntax (MODIFY vs ALTER COLUMN)

- [ ] **T019** [US1] Implement MigrationPlan in src/main/java/com/aidvps/druid/differ/internal/model/MigrationPlan.java
  - Properties: statements, warnings, destructiveOperations, sourceSchema, targetSchema, databaseDialect
  - Immutable design
  - Helper methods: hasDestructiveOperations(), getStatementCount()

### Core Orchestration (US1)

- [ ] **T020** [US1] Implement TableDiffer facade in src/main/java/com/aidvps/druid/differ/TableDiffer.java
  - Builder pattern with withDialect(DatabaseDialect)
  - generateMigration(String sourceSchema, String targetSchema) method
  - Coordinate: parse source → parse target → compare → generate SQL
  - MySQL dialect validation

- [ ] **T021** [US1] Implement MigrationOptions in src/main/java/com/aidvps/druid/differ/MigrationOptions.java
  - Properties: wrapInTransaction, includeComments, failOnDestructive
  - Builder pattern for construction

### Unit Tests (US1)

- [ ] **T022** [US1] Write SchemaParserTest in src/test/java/com/aidvps/druid/differ/internal/parser/SchemaParserTest.java
  - Test parsing of simple CREATE TABLE statements
  - Test parsing of multiple tables
  - Test error handling for invalid SQL
  - Test MySQL-specific syntax (AUTO_INCREMENT, etc.)

- [ ] **T023** [US1] Write SchemaComparatorTest in src/test/java/com/aidvps/druid/differ/internal/comparator/SchemaComparatorTest.java
  - Test detection of added tables
  - Test detection of removed tables
  - Test detection of added columns
  - Test detection of removed columns
  - Test detection of modified columns (type, nullability)
  - Test comparison ignoring whitespace

- [ ] **T024** [US1] Write MySQLMigrationGeneratorTest in src/test/java/com/aidvps/druid/differ/internal/generator/MySQLMigrationGeneratorTest.java
  - Test ALTER TABLE ADD COLUMN generation
  - Test ALTER TABLE DROP COLUMN generation
  - Test ALTER TABLE MODIFY COLUMN generation
  - Test CREATE TABLE generation
  - Test DROP TABLE generation
  - Test MySQL-specific syntax correctness

- [ ] **T025** [US1] Write TableDifferIntegrationTest in src/test/java/com/aidvps/druid/differ/TableDifferIntegrationTest.java
  - End-to-end test: simple column addition
  - End-to-end test: simple column deletion
  - End-to-end test: column type modification
  - Verify generated SQL executes successfully on test database

### Phase 3 Test Criteria
- [ ] Parse simple CREATE TABLE statements successfully
- [ ] Compare single-table schemas correctly
- [ ] Generate valid MySQL migration SQL
- [ ] Execute generated SQL on test MySQL database successfully
- [ ] Unit test coverage reaches 90%
- [ ] Performance: Complete migration generation for simple schema in <100ms

---

## Phase 4: User Story 2 - Production Schema Upgrade [US2]

**Goal**: Enable DevOps teams to perform complex production schema upgrades with multiple tables and foreign key relationships

**Scope**: Multi-table schemas, foreign keys, constraints, dependency ordering

### Enhanced Parser (US2)

- [ ] **T026** [US2] Enhance SchemaExtractor to handle advanced table features
  - Parse table-level constraints (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK)
  - Parse indexes and their properties
  - Parse table options (engine, tablespace, comment)
  - Parse PostgreSQL inheritance and Oracle-specific features (for future)

### Enhanced Comparison (US2)

- [ ] **T027** [US2] Enhance ChangeDetector for multi-table comparison
  - Compare foreign key relationships
  - Compare primary key changes
  - Compare unique constraint changes
  - Compare index changes
  - Detect constraint modifications (added/removed/modified)

### Dependency Analysis (US2)

- [ ] **T028** [US2] Implement DependencyAnalyzer in src/main/java/com/aidvps/druid/differ/internal/generator/DependencyAnalyzer.java
  - Build dependency graph of migration operations
  - Perform topological sort to determine execution order
  - Detect circular dependencies and report errors
  - Apply ordering rules:
    - Drop foreign keys before dropping referenced tables
    - Drop indexes before dropping tables
    - Add tables before adding foreign keys
    - Add columns before adding constraints

### Migration Generator Enhancement (US2)

- [ ] **T029** [US2] Enhance MySQLMigrationGenerator for constraint handling
  - Generate ALTER TABLE ADD CONSTRAINT statements
  - Generate ALTER TABLE DROP CONSTRAINT statements
  - Handle foreign key constraints with ON DELETE/UPDATE actions
  - Handle primary key changes
  - Generate CREATE INDEX statements
  - Generate DROP INDEX statements

### Error Handling Enhancement (US2)

- [ ] **T030** [US2] Enhance error handling and validation
  - Validate foreign key references (referenced table must exist)
  - Validate constraint columns (must exist in table)
  - Detect circular foreign key dependencies
  - Provide clear error messages with line/column information
  - Add warning system for potentially dangerous operations

### Unit Tests (US2)

- [ ] **T031** [US2] Write MultiTableSchemaTest in src/test/java/com/aidvps/druid/differ/internal/comparator/MultiTableSchemaTest.java
  - Test comparison of schemas with foreign keys
  - Test comparison with primary key changes
  - Test comparison with unique constraint changes
  - Test comparison with index changes

- [ ] **T032** [US2] Write DependencyAnalyzerTest in src/test/java/com/aidvps/druid/differ/internal/generator/DependencyAnalyzerTest.java
  - Test correct ordering of simple operations
  - Test ordering with foreign key dependencies
  - Test cycle detection and error reporting
  - Test complex dependency graph

- [ ] **T033** [US2] Write ConstraintHandlingTest in src/test/java/com/aidvps/druid/differ/internal/generator/ConstraintHandlingTest.java
  - Test foreign key constraint generation
  - Test primary key constraint generation
  - Test unique constraint generation
  - Test ON DELETE/UPDATE actions

### Phase 4 Test Criteria
- [ ] Parse and compare multi-table schemas with foreign keys correctly
- [ ] Generate migration SQL with correct dependency ordering
- [ ] Detect and report circular dependencies
- [ ] Validate foreign key and constraint references
- [ ] Execute complex migrations with foreign keys successfully
- [ ] Test coverage maintains 90%

---

## Phase 5: User Story 3 - Automated CI/CD Integration [US3]

**Goal**: Enable automated schema validation in CI/CD pipelines with strict performance requirements

**Scope**: Performance optimization, validation modes, batch processing

### Performance Optimization (US3)

- [ ] **T034** [US3] Implement caching layer for parsed schemas
  - Cache parsed Schema objects by hash
  - Invalidate cache on memory pressure
  - Implement LRU eviction policy
  - Measure cache hit rates

- [ ] **T035** [US3] Optimize ChangeDetector for large schemas
  - Implement hash-based pre-check for quick equality
  - Only deep-compare tables that pass hash check
  - Parallel comparison for multi-table schemas (optional)

- [ ] **T036** [US3] Optimize SQL generation performance
  - Pre-compute statement templates
  - Batch similar operations
  - Minimize string concatenation (use StringBuilder)

### Validation Levels (US3)

- [ ] **T037** [US3] Implement ValidationLevel enum and enforcement
  - Levels: STRICT, STANDARD, LENIENT
  - STRICT: Fail on any potential issue
  - STANDARD: Default behavior
  - LENIENT: Allow some inconsistencies
  - Apply in TableDiffer builder

### Migration Options Enhancement (US3)

- [ ] **T038** [US3] Enhance MigrationOptions with CI/CD features
  - Add transactionalDdl option
  - Add includeRollback option
  - Add dryRun option (validate without generating SQL)
  - Add timeout configuration

### Rollback SQL Generation (US3)

- [ ] **T039** [US3] Implement rollback generation in TableDiffer
  - generateRollback(MigrationPlan plan) method
  - Generate reverse operations (CREATE ↔ DROP, ADD ↔ DROP COLUMN)
  - Reverse dependency order
  - Handle destructive operation reversals

### Performance Benchmarks (US3)

- [ ] **T040** [US3] Create JMH benchmarks in src/test/java/com/aidvps/druid/differ/benchmark/
  - Location: SchemaParsingBenchmark.java
  - SchemaComparisonBenchmark.java
  - SQLGenerationBenchmark.java
  - EndToEndBenchmark.java
  - Measure memory allocation
  - Set regression threshold at 10%

### Performance Tests (US3)

- [ ] **T041** [US3] Write PerformanceTest in src/test/java/com/aidvps/druid/differ/PerformanceTest.java
  - Test 100-table schema comparison completes in <5 seconds
  - Test 1000-table schema parsing completes in <10 seconds
  - Test memory usage grows linearly
  - Verify no memory leaks in repeated operations

### Phase 5 Test Criteria
- [ ] Complete schema comparison for 100 tables in <5 seconds
- [ ] Generate rollback SQL that successfully reverses migration
- [ ] Validation levels work correctly (STRICT/STANDARD/LENIENT)
- [ ] JMH benchmarks configured and passing
- [ ] Performance regression alerts configured at 10% threshold

---

## Phase 6: User Story 4 - Schema Drift Detection [US4]

**Goal**: Enable DBAs to detect unauthorized schema changes across environments

**Scope**: Comprehensive difference detection, reporting, drift analysis

### Enhanced Difference Detection (US4)

- [ ] **T042** [US4] Enhance ChangeDetector for complete difference coverage
  - Detect trigger changes (if supported in future)
  - Detect character set and collation changes (MySQL)
  - Detect table option changes (engine, tablespace)
  - Detect comment changes
  - Implement semantic equivalence checking for default values

### Schema Hash and Verification (US4)

- [ ] **T043** [US4] Implement schema hashing and verification
  - Compute deterministic hash of schema structure
  - Support hash-based comparison for quick drift detection
  - Include hash in MigrationPlan for verification
  - Support schema signature comparison

### Detailed Reporting (US4)

- [ ] **T044** [US4] Implement detailed change report in MigrationPlan
  - Add change statistics (tables added/removed/modified, columns added/removed/modified)
  - Add severity classification (breaking vs non-breaking changes)
  - Add estimated impact analysis (data loss risk, downtime risk)
  - Generate human-readable change summary

### Warning and Destructive Operation Detection (US4)

- [ ] **T045** [US4] Implement Warning and DestructiveOperation classes
  - Location: src/main/java/com/aidvps/druid/differ/internal/model/Warning.java
  - Location: src/main/java/com/aidvps/druid/differ/internal/model/DestructiveOperation.java
  - Types: DATA_LOSS_RISK, DEPRECATED_SYNTAX, PERFORMANCE_NOTE, COMPATIBILITY_WARNING
  - Severity levels: INFO, WARN, ERROR
  - Track confirmation requirements

### Enhanced Validation (US4)

- [ ] **T046** [US4] Implement comprehensive schema validation
  - Validate table existence for all foreign key references
  - Validate column existence for all constraints
  - Validate data type compatibility
  - Check for potential data loss scenarios
  - Generate validation report with violations

### Comprehensive Testing (US4)

- [ ] **T047** [US4] Write SchemaDriftDetectionTest in src/test/java/com/aidvps/druid/differ/SchemaDriftDetectionTest.java
  - Test detection of all change types
  - Test false negative prevention (completeness)
  - Test false positive prevention (accuracy)
  - Test hash-based comparison speed
  - Test warning generation for destructive operations

### Phase 6 Test Criteria
- [ ] Detect all schema changes with zero false negatives
- [ ] Generate accurate drift reports with impact analysis
- [ ] Warn about all destructive operations before generation
- [ ] Validate schema consistency and report violations
- [ ] Support hash-based quick comparison for large schemas

---

## Phase 7: Database Dialect Expansion [US3, US4]

**Goal**: Add support for PostgreSQL and Oracle to enable broader adoption

### PostgreSQL Migration Generator

- [ ] **T048** [P] [US2] Implement PostgreSQLMigrationGenerator in src/main/java/com/aidvps/druid/differ/internal/generator/PostgreSQLMigrationGenerator.java
  - Generate ALTER TABLE ALTER COLUMN TYPE (not MODIFY)
  - Handle SERIAL and IDENTITY for auto-increment
  - Require constraint names for drops
  - Handle PostgreSQL-specific types (SERIAL, UUID, JSONB)
  - Generate USING clause for type conversions

### Oracle Migration Generator

- [ ] **T049** [P] [US2] Implement OracleMigrationGenerator in src/main/java/com/aidvps/druid/differ/internal/generator/OracleMigrationGenerator.java
  - Use VARCHAR2 instead of VARCHAR
  - Use NUMBER instead of INT/BIGINT
  - Parentheses required for multi-column ALTER TABLE
  - Handle CLOB for large text
  - Constraint names required for all drops

### MigrationGenerator Factory

- [ ] **T050** [US2] Implement MigrationGeneratorFactory in src/main/java/com/aidvps/druid/differ/internal/generator/MigrationGeneratorFactory.java
  - Factory pattern to create appropriate generator by dialect
  - Registry of available generators
  - Error handling for unsupported dialects

### Multi-Dialect Testing

- [ ] **T051** [P] [US2] Write PostgreSQLMigrationGeneratorTest in src/test/java/com/aidvps/druid/differ/internal/generator/PostgreSQLMigrationGeneratorTest.java
  - Test PostgreSQL-specific syntax correctness
  - Test SERIAL and IDENTITY handling
  - Test ALTER COLUMN TYPE generation
  - Test constraint name requirements

- [ ] **T052** [P] [US2] Write OracleMigrationGeneratorTest in src/test/java/com/aidvps/druid/differ/internal/generator/OracleMigrationGeneratorTest.java
  - Test Oracle-specific syntax (VARCHAR2, NUMBER)
  - Test parentheses in ALTER TABLE
  - Test constraint name requirements
  - Test CLOB handling

- [ ] **T053** [P] [US2] Write MultiDialectIntegrationTest in src/test/java/com/aidvps/druid/differ/MultiDialectIntegrationTest.java
  - Test same schema migration across all three dialects
  - Validate dialect-specific SQL correctness
  - Test with Testcontainers: MySQL, PostgreSQL containers

### Phase 7 Test Criteria
- [ ] Generate valid PostgreSQL migration SQL
- [ ] Generate valid Oracle migration SQL
- [ ] Execute migrations successfully on test databases
- [ ] All dialect-specific features tested

---

## Phase 8: Polish & Cross-Cutting Concerns

### Code Quality and Constitution Compliance

- [ ] **T054** Final code review for SOLID principles compliance
  - Review all classes for single responsibility
  - Verify dependency inversion usage
  - Check for code duplication and extract abstractions
  - Location: All source files

- [ ] **T055** Final Spotless formatting and style check
  - Run spotless:format on all source files
  - Run spotless:check to verify compliance
  - Fix any style violations
  - Location: All Java source files

### Documentation

- [ ] **T056** [US1] Add comprehensive JavaDoc to TableDiffer and public APIs
  - Location: src/main/java/com/aidvps/druid/differ/TableDiffer.java
  - Include usage examples in JavaDoc
  - Document all public methods and parameters

- [ ] **T057** [US1] Add JavaDoc to all model classes
  - Location: All classes in internal.model package
  - Document properties, constraints, and relationships

- [ ] **T058** [US1] Update README.md with quick start guide
  - Include Gradle dependency information
  - Add code examples for basic usage
  - Document supported database dialects
  - Include troubleshooting guide

### Integration Testing Enhancement

- [ ] **T059** [US3] Implement Testcontainers integration tests
  - Set up MySQL container (mysql:8.0)
  - Set up PostgreSQL container (postgres:15)
  - Execute generated migrations on real databases
  - Verify schema matches expected state after migration
  - Location: src/test/java/com/aidvps/druid/differ/integration/

### Performance Validation

- [ ] **T060** [US3] Run comprehensive JMH benchmarks
  - Execute all benchmark tests
  - Verify performance meets targets:
    - Parsing: <2s for 100 tables
    - Comparison: <3s for 100 tables
    - Generation: <1s after comparison
    - Total: <5s end-to-end
  - Capture baseline metrics for regression testing
  - Location: src/test/java/com/aidvps/druid/differ/benchmark/

### Final Testing and Coverage

- [ ] **T061** [US1] Achieve 90% line coverage
  - Run JaCoCo or similar coverage tool
  - Identify uncovered lines
  - Add tests for edge cases
  - Verify coverage report shows 90%+

- [ ] **T062** [US1] Final integration test run
  - Run all unit tests
  - Run all integration tests
  - Run all performance tests
  - Verify all tests pass

### Release Preparation

- [ ] **T063** Update CHANGELOG.md with feature additions
  - Document all functional requirements implemented
  - Document database dialect support
  - Document performance achievements
  - Location: CHANGELOG.md

- [ ] **T064** Finalize version and release notes
  - Version: 1.0.0 (initial release)
  - Document supported features
  - Document known limitations
  - Location: RELEASE_NOTES.md

### Phase 8 Test Criteria
- [ ] All unit tests pass (100% success rate)
- [ ] All integration tests pass on MySQL and PostgreSQL
- [ ] Line coverage >= 90%
- [ ] Performance meets all targets
- [ ] JavaDoc complete for all public APIs
- [ ] README updated with examples
- [ ] No Spotless formatting violations

---

## Task Dependencies

```mermaid
graph TD
    A[Phase 1: Setup] --> B[Phase 2: Foundational]
    B --> C[Phase 3: US1 - Development]
    B --> D[Phase 4: US2 - Production]
    B --> E[Phase 5: US3 - CI/CD]
    B --> F[Phase 6: US4 - Drift Detection]
    C --> G[Phase 7: Dialect Expansion]
    D --> G
    E --> G
    F --> G
    G --> H[Phase 8: Polish]
```

### Critical Path
Setup → Foundational → US1 (MVP) → Dialect Expansion → Polish → Release

### Parallel Execution Opportunities
- **005-008**: Model classes can be developed in parallel
- **T046-T047**: PostgreSQL and Oracle generators can be developed in parallel after US2
- **T049-T050**: Multi-dialect tests can be developed in parallel
- **T054-T056**: Documentation tasks can be done in parallel with implementation

---

## Definition of Done

- [ ] All user stories (US1-US4) implemented and tested independently
- [ ] MySQL, PostgreSQL, and Oracle dialect support complete
- [ ] Performance requirements met: <5 seconds for 100 tables
- [ ] Test coverage >= 90% line coverage
- [ ] All integration tests pass with real databases (Testcontainers)
- [ ] JMH benchmarks configured and passing
- [ ] JavaDoc complete for all public APIs
- [ ] README with quick start guide and examples
- [ ] Spotless formatting passes
- [ ] No critical or high-severity code quality issues
- [ ] CHANGELOG.md updated
- [ ] Version 1.0.0 ready for release

---

## Summary

**Total Tasks**: 62
- Setup: 4 tasks (includes SNAPSHOT dependency verification)
- Foundational: 7 tasks (core models, exceptions, parser foundation)
- User Story 1 (Development): 13 tasks (MySQL single-table schema comparison)
- User Story 2 (Production): 8 tasks (multi-table, foreign keys, dependencies)
- User Story 3 (CI/CD): 8 tasks (performance optimization, validation, rollback)
- User Story 4 (Drift Detection): 6 tasks (comprehensive diff detection, reporting)
- Dialect Expansion: 6 tasks (PostgreSQL and Oracle support)
- Polish: 10 tasks (documentation, testing, release preparation)

**MVP Scope**: Tasks T001-T024 (Phase 1-3) - Single-table MySQL support with basic migration
**Full Feature**: All tasks T001-T064 - Multi-dialect, multi-table, production-ready

**Parallel Opportunities**: 15 tasks marked with [P] can be executed in parallel
**User Story Dependencies**: US2-US4 depend on foundational phase only, can proceed independently after Phase 2

**Updated Configuration**:
- Build System: Gradle (not Maven)
- Java Version: 8 (minimum requirement)
- Parser: druid-parser:1.2.28-SNAPSHOT
- Repository: mavenLocal() required for SNAPSHOT access
