# Agent Context: SQL Table Differ Implementation

## Project Context

**Feature**: SQL Table Differ - Automated database schema comparison and migration SQL generation
**Branch**: 1-sql-schema-diff
**Spec Location**: `specs/1-sql-schema-diff/spec.md`
**Plan Location**: `specs/1-sql-schema-diff/plan.md`

## Key Technologies & Dependencies

### Core Library
- **Druid Parser**: `com.aidvps:druid-parser:1.2.28-SNAPSHOT` from `/Users/shunyun/workspace/java/druid/core`
  - **Version**: 1.2.28-SNAPSHOT
  - **Usage**: Parse CREATE TABLE statements into AST
  - **Key Classes**: `SQLCreateTableStatement`, `SQLColumnDefinition`, `SQLConstraint`, `SQLTableElement`
  - **Parser Class**: `SQLCreateTableParser` in `com.alibaba.druid.sql.parser`
  - **Status**: EXISTING LIBRARY - DO NOT CREATE CUSTOM PARSER

### Build System
- **Tool**: Gradle (not Maven)
- **Java Version**: 8 (minimum requirement)
- **Configuration**: sourceCompatibility and targetCompatibility set to VERSION_1_8
- **Test Configurations**: Separate configurations for unit tests, integration tests, and JMH benchmarks
- **Repository Configuration**: Must include `mavenLocal()` for SNAPSHOT dependencies
  ```gradle
  repositories {
      mavenLocal()  // Essential for SNAPSHOT dependencies like druid-parser
      mavenCentral()
  }
  ```

### Target Database Dialects (Priority Order)
1. **MySQL** (Primary)
   - Uses MODIFY COLUMN for type changes
   - AUTO_INCREMENT syntax
   - No constraint names required for drops
2. **PostgreSQL** (Secondary)
   - Uses SERIAL or IDENTITY for auto-increment
   - Requires constraint names for drops
   - Uses TYPE keyword for column changes
3. **Oracle** (Tertiary)
   - VARCHAR2 instead of VARCHAR
   - NUMBER for integers
   - Parentheses required for ALTER TABLE operations

## Architecture Decisions

### Layered Architecture
1. **Parser Layer**: DruidParserAdapter, SchemaExtractor, DatabaseDialectResolver
2. **Model Layer**: Schema, Table, Column, SchemaDiff, ChangeDetector
3. **Generator Layer**: MigrationGenerator, MySQL/PostgreSQL/Oracle generators, DependencyAnalyzer
4. **Orchestration Layer**: TableDiffer (facade), MigrationPlan

### Design Patterns
- **Facade Pattern**: TableDiffer as main public API
- **Abstract Factory Pattern**: Database-specific SQL generators
- **Strategy Pattern**: Database dialect handling
- **Builder Pattern**: MigrationOptions, complex object construction
- **Immutable Objects**: All model classes are immutable for thread safety

### Package Structure
```
com.aidvps.druid.differ
├── TableDiffer.java (public API)
├── DatabaseDialect.java (enum)
├── MigrationOptions.java
└── internal/
    ├── parser/
    │   ├── DruidParserAdapter.java
    │   └── SchemaExtractor.java
    ├── model/
    │   ├── Schema.java
    │   ├── Table.java
    │   ├── Column.java
    │   ├── Constraint.java
    │   ├── SchemaDiff.java
    │   └── MigrationPlan.java
    ├── comparator/
    │   └── ChangeDetector.java
    └── generator/
        ├── MigrationGenerator.java
        ├── MySQLMigrationGenerator.java
        ├── PostgreSQLMigrationGenerator.java
        ├── OracleMigrationGenerator.java
        └── DependencyAnalyzer.java
```

## API Design

### Main Entry Point
```java
public final class TableDiffer {
    public static class Builder {
        public Builder withDialect(DatabaseDialect dialect);
        public Builder withValidationLevel(ValidationLevel level);
        public TableDiffer build();
    }

    public MigrationPlan generateMigration(String sourceSchema, String targetSchema);
    public MigrationPlan generateMigration(String source, String target, MigrationOptions options);
    public String generateRollback(MigrationPlan plan);
}
```

### Core Models
- **Schema**: Complete database structure
- **Table**: Table with columns, constraints, indexes
- **Column**: Column definition with type, constraints, properties
- **Constraint**: PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK
- **SchemaDiff**: Difference representation
- **MigrationPlan**: Ordered SQL statements + warnings + destructive operations

## Key Implementation Details

### Druid-Parser Integration
- **Entry Point**: `SQLCreateTableParser parser = new SQLCreateTableParser(sql)`
- **Parse Method**: `SQLCreateTableStatement statement = parser.parseCreateTable()`
- **Extract Elements**: `statement.getTableElementList()` returns `List<SQLTableElement>`
- **Handle Polymorphism**: Check instanceof for Column vs Constraint vs Index

### Comparison Algorithm
1. **Phase 1**: Quick hash-based equality check
2. **Phase 2**: Table-level diff (added/removed/modified)
3. **Phase 3**: Column-level diff with semantic awareness
4. **Phase 4**: Constraint and index comparison

### Statement Ordering (Dependency Analysis)
- **Topological Sort**: Ensure safe execution order
- **Priority Rules**:
  1. DROP statements (highest priority)
  2. CREATE statements
  3. ALTER ADD statements (lowest priority)

### Performance Requirements
- **Parsing**: <2 seconds for 100 tables
- **Comparison**: <3 seconds for 100 tables
- **Generation**: <1 second after comparison
- **Total**: <5 seconds for complete pipeline (100 tables)

## Testing Strategy

### Unit Tests (Target: 90% coverage)
- SchemaParserTest: Test druid-parser integration
- SchemaComparatorTest: Test diff detection
- MigrationGeneratorTest: Test SQL generation per dialect
- DependencyAnalyzerTest: Test statement ordering
- ErrorHandlingTest: Test exception scenarios

### Integration Tests
- MultiDialectTest: Test all three database dialects
- SchemaEvolutionTest: End-to-end scenarios
- ForeignKeyTest: Multi-table relationships
- PerformanceTest: Verify <5 second requirement

### Performance Tests (JMH)
- SchemaParsingBenchmark
- SchemaComparisonBenchmark
- SQLGenerationBenchmark
- EndToEndBenchmark

## Constitution Compliance

### Code Quality
- **Google Java Style**: Spotless formatting
- **SOLID Principles**: Single responsibility, dependency inversion
- **Immutability**: All model classes final with final fields

### Testing Requirements
- **Unit Tests**: 90% line coverage with JUnit 5 + Mockito
- **Integration Tests**: Testcontainers for real databases
- **Performance Tests**: JMH benchmarks with regression alerts

### Documentation
- **JavaDoc**: All public APIs with examples
- **README**: Quick start guide + full documentation
- **Code Examples**: Executable and test-covered

### API Stability
- **Semantic Versioning**: MAJOR.MINOR.PATCH
- **Backward Compatibility**: No breaking changes in MINOR/PATCH
- **Internal Packages**: Mark implementation with `internal` suffix

## Risk Mitigation

1. **Druid-parser API Changes**: Wrap in adapter with version checks
2. **Complex Dependencies**: Topological sort with cycle detection
3. **Performance on Large Schemas**: Caching + incremental comparison
4. **Dialect-Specific Bugs**: Comprehensive testing per dialect

## Acceptance Criteria (from spec)

- [x] Parse CREATE TABLE using druid-parser
- [x] Support MySQL, PostgreSQL, Oracle (priority order)
- [x] Detect all schema differences (zero false negatives)
- [x] Generate valid migration SQL with dependency ordering
- [x] Handle multi-table schemas with foreign keys
- [x] <5 seconds for 100 tables
- [x] Clear error messages for all failures
- [x] Warn about destructive operations

## Next Phase Preparation

**Phase 2 Tasks** (from plan.md):
1. Implement DruidParserAdapter and SchemaExtractor
2. Implement core model classes (Schema, Table, Column, SchemaDiff)
3. Implement ChangeDetector for schema comparison
4. Implement MySQLMigrationGenerator first
5. Add unit tests to reach 90% coverage
6. Implement DependencyAnalyzer
7. Add PostgreSQL and Oracle generators
8. Integration testing with Testcontainers
9. JMH performance benchmarks
10. Error handling and documentation refinements

## Generated Artifacts

### Phase 0: Research
- ✅ `specs/1-sql-schema-diff/research.md` - Comprehensive druid-parser integration guide, database dialect differences, comparison algorithm design

### Phase 1: Design & Contracts
- ✅ `specs/1-sql-schema-diff/data-model.md` - Complete data model with entity relationships and validation rules
- ✅ `specs/1-sql-schema-diff/contracts/openapi.yaml` - OpenAPI 3.0 specification for migration API
- ✅ `specs/1-sql-schema-diff/quickstart.md` - Quick start guide with code examples
- ✅ `specs/1-sql-schema-diff/agent-context.md` - This context file

### Ready for Implementation
All planning artifacts are complete. The implementation can now proceed with Phase 2 (development tasks).

## Key Decisions Summary

| Component | Decision | Rationale |
|-----------|----------|-----------|
| SQL Parser | Druid-parser library | Mature, comprehensive, multi-dialect |
| Comparison | Object equivalence + semantic checks | Detect meaningful differences |
| Generation | Abstract factory pattern | Clean dialect separation |
| Ordering | Topological sort | Dependency-safe execution |
| Testing | Testcontainers | Real database validation |
| Performance | <5 seconds target | Success criteria requirement |
| First Dialect | MySQL | Simplest syntax, validation |
