# Feature Specification: SQL Table Differ

## Executive Summary

The SQL Table Differ enables automated comparison of database table structures across different schema versions and generates the necessary migration SQL to transform one schema into another. This capability is critical for maintaining database version consistency across development, testing, and production environments, reducing manual effort in schema migration, and ensuring reliable database versioning practices.

By analyzing CREATE TABLE statements from both source and target schemas using the com.aidvps:druid-parser library, the component generates database-specific migration SQL including ALTER, CREATE, and DROP statements. This enables automated, repeatable, and consistent database migrations that can be integrated into CI/CD pipelines.

**Technical Integration Note**: Schema parsing will utilize the existing com.aidvps:druid-parser library (located at /Users/shunyun/workspace/java/druid/core) which provides robust SQL CREATE TABLE statement parsing capabilities. No custom parser implementation is required.

## Clarifications

### Session 2025-11-29
- Q: Should we create a custom SQL parser or use an existing library? → A: Use existing com.aidvps:druid-parser library from /Users/shunyun/workspace/java/druid/core
- Q: Which database dialects should be prioritized for migration SQL generation? → A: MySQL, PostgreSQL, and Oracle (in that priority order)

## User Scenarios & Testing

### Scenario 1: Development Environment Schema Evolution
**Actor**: Database Developer
**Context**: Developer modifies a local database schema during feature development and needs to generate migration scripts for team members

**Flow**:
1. Developer defines source schema (current version)
2. Developer defines target schema (desired version)
3. System analyzes both schemas
4. System generates migration SQL with ALTER, CREATE, and DROP statements
5. Developer reviews and applies migration to local database

**Testing**: Verify migration SQL correctly handles column additions, modifications, type changes, and table deletions

### Scenario 2: Production Schema Upgrade
**Actor**: DevOps Engineer
**Context**: Engineering team needs to upgrade production database schema with zero downtime

**Flow**:
1. DevOps engineer extracts production schema (source)
2. DevOps engineer prepares target schema (new version)
3. System generates comprehensive migration SQL
4. Team reviews generated SQL for safety
5. Migration is applied during maintenance window

**Testing**: Validate generated SQL handles complex scenarios including foreign keys, indexes, constraints, and data type migrations

### Scenario 3: Automated CI/CD Integration
**Actor**: Build System
**Context**: CI/CD pipeline automatically validates schema changes against production

**Flow**:
1. Pipeline extracts schema from feature branch (source)
2. Pipeline extracts schema from production (target)
3. System generates migration SQL automatically
4. Pipeline applies migration to test database
5. Automated tests validate schema changes

**Testing**: Ensure performance meets pipeline requirements (<5 seconds for typical schema comparisons)

### Scenario 4: Schema Drift Detection
**Actor**: Database Administrator
**Context**: DBA monitors multiple database environments to detect unauthorized schema changes

**Flow**:
1. DBA extracts schemas from staging and production
2. System compares schemas
3. System generates migration SQL showing differences
4. DBA reviews changes and validates they are authorized
5. Documentation is updated with approved changes

**Testing**: Verify comparison accuracy and comprehensive change detection

## Functional Requirements

### FR-001: Schema Parsing
**Priority**: HIGH
**Requirement**: System must successfully parse CREATE TABLE statements from both source and target schemas using the druid-parser library
**Acceptance Criteria**:
- Leverage druid-parser's SQLCreateTableParser and SQLStatementParser to parse CREATE TABLE statements
- Extract table metadata including table name, columns, data types, and all table elements
- Parse column definitions with data types, nullability, default values, auto-increment, and column-level constraints (PRIMARY KEY, UNIQUE, NOT NULL, CHECK, FOREIGN KEY)
- Parse table-level constraints (PRIMARY KEY, UNIQUE, CHECK, FOREIGN KEY) and indexes
- Support multiple database dialects with priority: MySQL (primary), PostgreSQL (secondary), Oracle (tertiary), with extensible architecture for additional dialects
- Parse advanced table features: engine, tablespace, partitioning, inheritance (PostgreSQL), temporary tables, LIKE clauses
- Handle schemas with multiple tables in a single definition
- Report parsing errors with clear, actionable messages leveraging druid-parser's SQLParseException
- Map parsed AST objects (SQLCreateTableStatement, SQLColumnDefinition, SQLConstraint) to internal schema representation

### FR-002: Schema Comparison
**Priority**: HIGH
**Requirement**: System must semantically compare two database schemas and identify all differences
**Acceptance Criteria**:
- Detect table additions, modifications, and deletions
- Detect column additions, modifications (type, constraints), and deletions
- Detect changes to primary keys, foreign keys, and unique constraints
- Detect changes to indexes and triggers
- Ignore insignificant whitespace and formatting differences
- Handle schema order independence (tables can be defined in any order)

### FR-003: Migration SQL Generation
**Priority**: HIGH
**Requirement**: System must generate database-specific SQL statements to transform source schema into target schema with priority support for MySQL, PostgreSQL, and Oracle
**Acceptance Criteria**:
- Generate ALTER TABLE statements for column modifications and additions
- Generate CREATE TABLE statements for new tables
- Generate DROP TABLE statements for removed tables
- Generate ALTER TABLE ... DROP COLUMN for removed columns
- Generate statements for constraint and index changes
- Order statements to satisfy dependency constraints (drop before create, alter before adding constraints)
- Support transaction-wrapped migrations where appropriate

### FR-004: Dependency Handling
**Priority**: MEDIUM
**Requirement**: System must correctly handle dependencies between schema objects
**Acceptance Criteria**:
- Drop foreign keys before dropping referenced tables
- Drop indexes before dropping tables
- Add constraints only after related tables and columns exist
- Warn about potential data loss from destructive operations (DROP TABLE, DROP COLUMN)
- Handle circular dependencies gracefully

### FR-005: Error Handling
**Priority**: HIGH
**Requirement**: System must handle edge cases and errors gracefully
**Acceptance Criteria**:
- Validate schema consistency before generating migration
- Provide clear error messages for invalid or incompatible schemas
- Handle empty or null schemas appropriately
- Warn about potentially destructive operations before generation
- Support rollback scenarios for migration failures

### FR-006: Multi-Table Support
**Priority**: HIGH
**Requirement**: System must handle schemas with multiple related tables
**Acceptance Criteria**:
- Compare entire schema collections, not just single tables
- Maintain foreign key relationships across table changes
- Generate coherent migration plans for multi-table schemas
- Validate referential integrity throughout migration process

## Success Criteria

- **Accuracy**: Generated migration SQL correctly transforms source schema to target schema 100% of the time for valid inputs
- **Performance**: Schema comparison and migration generation completes within 5 seconds for schemas with up to 100 tables
- **Reliability**: System handles edge cases without crashes, providing appropriate error messages 100% of the time
- **Completeness**: All schema differences are detected and appropriate SQL is generated, with zero false negatives
- **Safety**: System warns users about destructive operations before generating DROP statements
- **Compatibility**: Generated SQL is valid for the target database dialect (prioritizing MySQL, PostgreSQL, Oracle) without manual modifications

## Key Entities

### Schema
- **Definition**: Complete database structure including all tables, columns, constraints, indexes, and relationships
- **Properties**: Table definitions, column definitions, data types, constraints, indexes, foreign keys, triggers
- **Lifecycle**: Created from CREATE TABLE statements, validated, compared, then used to generate migrations

### Table
- **Definition**: Named collection of columns defining a database entity
- **Properties**: Name, columns, primary key, unique constraints, foreign keys, indexes
- **Relationships**: May reference other tables via foreign keys

### Column
- **Definition**: Individual data field within a table
- **Properties**: Name, data type, nullable, default value, auto-increment, unique constraint
- **Constraints**: Primary key membership, foreign key references

### Migration SQL
- **Definition**: Sequence of SQL statements that transform one schema to another
- **Types**: ALTER TABLE, CREATE TABLE, DROP TABLE, ALTER TABLE ... ADD COLUMN, ALTER TABLE ... DROP COLUMN
- **Ordering**: Must respect dependency constraints for successful execution

### Schema Diff
- **Definition**: Structured representation of differences between two schemas
- **Components**: Added tables, removed tables, modified tables, added columns, removed columns, modified columns, constraint changes
- **Purpose**: Intermediate representation used to generate migration SQL

## Non-Functional Requirements

### Performance
- Schema parsing: Complete within 2 seconds for schemas with 100 tables
- Schema comparison: Complete within 3 seconds for typical production schemas (20-50 tables)
- Migration generation: Complete within 1 second after comparison

### Scalability
- Support schemas with up to 1,000 tables
- Memory usage remains reasonable for large schemas (linear growth, no exponential blow-up)
- CPU usage scales reasonably with schema size

### Reliability
- 99.9% uptime for schema comparison operations
- Graceful handling of malformed SQL inputs
- Consistent results for identical inputs (deterministic output)

### Usability
- Clear, actionable error messages for all failure scenarios
- Informative warnings about potentially destructive operations
- Generated SQL includes comments explaining purpose of changes

### Security
- No execution of input SQL (parse-only operation)
- Sanitized output preventing SQL injection in generated statements
- No sensitive data logging or persistence

## Assumptions

- Input schemas are syntactically valid SQL CREATE TABLE statements
- Users have appropriate database permissions to execute generated migration SQL
- Schema evolution follows standard SQL conventions without vendor-specific extensions
- Migration execution will be reviewed by qualified database administrators
- Generated migrations will be tested in non-production environments before production deployment
- Users understand the difference between migration directions (forward vs rollback)

## Risks and Mitigation

**Risk**: Generated migration SQL may cause data loss for DROP operations
**Mitigation**: Require explicit confirmation for destructive operations and provide clear warnings

**Risk**: Schema comparison may miss subtle semantic differences
**Mitigation**: Comprehensive testing suite covering edge cases and validation against known schemas

**Risk**: Dependency ordering errors may cause migration failures
**Mitigation**: Dependency analysis algorithm with topological sorting and validation

**Risk**: Large schemas may cause performance issues
**Mitigation**: Performance benchmarks and optimization for schema comparison algorithm
