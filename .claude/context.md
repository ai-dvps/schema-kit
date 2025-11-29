# Project Context: SQL Table Differ Feature

## Current Feature: SQL Table Differ

**Status**: In Planning Phase
**Branch**: 1-sql-table-differ
**Version**: 1.1.0 (new feature)

## Technologies Used

### Core Technologies
- **Java**: 8+ (minimum version requirement)
- **Build System**: Gradle
- **Parser**: com.aidvps.druid (shadow package of druid-parser)
- **Testing Framework**: JUnit 5, Mockito
- **Integration Testing**: Testcontainers
- **Performance Benchmarking**: JMH (Java Microbenchmark Harness)

### Database Vendor Support
The SQL differ must generate compatible SQL for:
1. **MySQL** - MySQL 5.7+ and 8.x
2. **PostgreSQL** - PostgreSQL 9.1+
3. **Oracle** - Oracle 11g+ (12c for identity columns)
4. **SQL Server** - SQL Server 2012+
5. **SQLite** - SQLite 3.7.11+

### Key Dependencies (Existing)
- `com.aidvps:druid-parser:1.2.28-SNAPSHOT` (already in build.gradle)
- `org.junit.jupiter:junit-jupiter:5.10.0` (already in build.gradle)

### New Dependencies (To Be Added)
- `org.testcontainers:junit-jupiter` - For integration testing
- `org.testcontainers:mysql` - MySQL test container
- `org.testcontainers:postgresql` - PostgreSQL test container
- `org.openjdk.jmh:jmh-core` - For benchmarking
- `org.openjdk.jmh:jmh-generator-annprocess` - JMH annotation processor

## Architecture Overview

### Package Structure
```
com.aidvps.schemakit.differ
├── SqlTableDiffer.java (public API)
├── SqlDifferException.java (exceptions)
├── TableDiffReport.java (DTO)
└── internal/
    ├── parser/
    │   ├── SchemaParser.java
    │   └── DruidAdapter.java
    ├── comparator/
    │   ├── SchemaComparator.java
    │   ├── DiffElement.java
    │   ├── ColumnDiff.java
    │   └── ConstraintDiff.java
    ├── generator/
    │   ├── SqlGenerator.java
    │   ├── VendorAdapter.java
    │   ├── MySqlAdapter.java
    │   ├── PostgreSqlAdapter.java
    │   ├── OracleAdapter.java
    │   ├── SqlServerAdapter.java
    │   └── SQLiteAdapter.java
    └── model/
        ├── Schema.java
        ├── Column.java
        ├── Constraint.java
        └── Index.java
```

### Three-Phase Pipeline
1. **Parse Phase**: SQL → AST (using druid) → Schema object
2. **Compare Phase**: Schema → DiffElement hierarchy
3. **Generate Phase**: DiffElement → SQL statements (vendor-specific)

## Implementation Phases

### Phase 0: Research & Design
- ✅ Research druid parser capabilities
- ✅ Analyze vendor DDL differences
- ✅ Design comparison algorithms
- ✅ Define exception hierarchy
- ✅ Create data model
- ✅ Define API contracts

### Phase 1: Core Parser Implementation (TBD)
- Implement SchemaParser
- Create model classes
- Add unit tests

### Phase 2: Comparison Engine (TBD)
- Implement SchemaComparator
- Create DiffElement hierarchy
- Write tests

### Phase 3: SQL Generation (TBD)
- Implement SqlGenerator
- Create vendor adapters
- End-to-end testing

### Phase 4: Integration & Testing (TBD)
- Testcontainers setup
- JMH benchmarks
- Performance tuning

### Phase 5: Documentation & Polish (TBD)
- JavaDoc
- README updates
- Code review

## Performance Targets

- **Small Tables** (≤100 columns): <500ms
- **Large Tables** (1000 columns): <2000ms
- **Memory Usage**: Linear with table size, bounded by JVM heap
- **Benchmark Regression**: <10% across releases

## Testing Strategy

### Unit Tests (90% coverage target)
- `SqlTableDifferTest` - Main API tests
- `SchemaParserTest` - Parser integration
- `SchemaComparatorTest` - Comparison logic
- `SqlGeneratorTest` - SQL generation
- `VendorAdapterTest` - Per vendor adapters
- `EdgeCaseTest` - Null handling, error cases

### Integration Tests
- End-to-end comparison tests
- Multi-vendor compatibility
- Real-world schema examples
- Java 8+ version testing

### Performance Tests (JMH)
- Parse operation benchmarks
- Comparison performance tests
- SQL generation benchmarks
- Memory allocation tracking

## Critical Implementation Decisions

1. **Parser Strategy**: Use druid's SQLStatementParser with SQLCreateTableStatement extraction
2. **Comparison**: Semantic comparison (case-insensitive, normalized)
3. **Vendor Support**: Plugin architecture with adapter pattern
4. **Error Handling**: Comprehensive exception hierarchy with detailed messages
5. **Statement Ordering**: Dependency-aware ordering (drop FKs → drop indexes → alter columns → add columns → create indexes → add FKs)

## Key Features

- Parse CREATE TABLE statements from any supported vendor
- Semantic comparison (ignores formatting, handles case-insensitivity)
- Generate vendor-specific ALTER/CREATE/DROP statements
- Handle edge cases (null source → CREATE, null target → DROP)
- Support tables with 1000 columns, 100 indexes
- Comprehensive error handling and validation
- Detailed diff reports for analysis
- Asynchronous API for large schemas

## Constitution Compliance

All implementation must adhere to:
- ✅ Google Java Style Guide (Spotless formatting)
- ✅ SOLID Principles (Single Responsibility, Dependency Inversion)
- ✅ 90% test coverage (JUnit 5, Mockito)
- ✅ Integration tests (Testcontainers)
- ✅ API backward compatibility
- ✅ Comprehensive JavaDoc documentation
- ✅ JMH performance benchmarks
- ✅ Resource management (try-with-resources)

## Reference Documents

- **Specification**: `specs/1-sql-table-differ/spec.md`
- **Implementation Plan**: `specs/1-sql-table-differ/impl-plan/plan.md`
- **Research Findings**: `specs/1-sql-table-differ/impl-plan/research.md`
- **Data Model**: `specs/1-sql-table-differ/impl-plan/data-model.md`
- **API Contract**: `specs/1-sql-table-differ/impl-plan/contracts/api-contract.md`
- **Quick Start**: `specs/1-sql-table-differ/impl-plan/quickstart.md`

## Next Steps

1. Begin Phase 1 implementation (Core Parser)
2. Set up build configuration (add Testcontainers, JMH dependencies)
3. Create initial Java source files
4. Implement SchemaParser with druid integration
5. Write comprehensive unit tests

## Notes

- The com.aidvps.druid package is a shadow/shaded version of the druid parser
- All public APIs must be marked with `@PublicApi` or similar annotation for visibility
- Internal packages must be clearly marked with `internal` suffix
- Performance is critical - benchmark all public methods
- Generated SQL must be executable and safe (no data loss without explicit DROP)
