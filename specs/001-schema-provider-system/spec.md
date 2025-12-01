# Feature Specification: Schema Provider System with Multi-Source Support

**Feature Branch**: `[001-schema-provider-system]`
**Created**: 2025-12-01
**Status**: Draft
**Input**: User description: "we are going to solve the problem where to get the source schemas and target schemas for a given database. I think we can difine a schema provider, whicn can provide the source schemas and target schemas for: 1. a given living database, for example, a living mysql database which schema will be retreived from the database by a query. 2. a directory, for example, src/main/resources/schemas 3. a git branch/tag of a git repository 4. a directory inside a jar file of the runtime classpath 5. it can be customized by the end user to provide the source schemas and target schemas from a different source. After that, the migrations will be generated at database level rather than single table level. In order to distinguish schemas at table level, we intruduce  new file extensions: 1. .db, for database which contains the create database statement; 2. .tbl, for table which contains the create table statement; The file name is same as the table name or the database. The files are orgnized in the directory structure, the directory name is same as the database name, under the directory, there is only one .db and multiple .tbl files."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Directory-based Schema Source (Priority: P1)

As a developer, I want to provide source and target database schemas from a local directory so that I can generate migrations between different versions of my database schema stored in my project files.

**Why this priority**: This is the most common use case for developers working with version-controlled schema definitions in their projects. It enables offline schema comparison and migration generation without requiring live database connections.

**Independent Test**: Can be fully tested by providing a directory path containing .db and .tbl files in the expected structure, then verifying that schemas are successfully loaded and compared, generating database-level migration scripts.

**Acceptance Scenarios**:

1. **Given** a directory containing properly structured database schema files (.db and .tbl files organized in database-named directories), **When** a user configures a directory-based schema provider with this path, **Then** the system MUST successfully load all database and table definitions from the files.

2. **Given** a directory with multiple database subdirectories, each containing one .db file and multiple .tbl files, **When** the schema provider processes the directory, **Then** it MUST create a complete schema model representing all databases and their tables.

3. **Given** a directory with valid schema files for both source and target, **When** the user requests migration generation, **Then** the system MUST generate database-level migration scripts that cover all differences between the two schemas.

---

### User Story 2 - Live Database Connection Schema Provider (Priority: P2)

As a database administrator, I want to retrieve schemas directly from a live database instance (such as MySQL) so that I can compare it against another schema version and generate accurate migration scripts.

**Why this priority**: This enables real-world schema comparison between production/staging environments and desired states, which is critical for deployment and change management workflows.

**Independent Test**: Can be fully tested by connecting to a live database instance, retrieving its schema, and verifying that all databases, tables, columns, indexes, and constraints are accurately captured in the schema model.

**Acceptance Scenarios**:

1. **Given** a live database connection with proper credentials and permissions, **When** a user configures a database schema provider with connection details, **Then** the system MUST successfully connect and retrieve the complete database schema without requiring manual queries.

2. **Given** a connected live database containing multiple databases and tables, **When** the schema provider fetches the schema, **Then** it MUST accurately capture all database objects including tables, columns, constraints, indexes, and relationships.

3. **Given** schema information retrieved from a live database and schema information from another source, **When** the user generates migrations, **Then** the migration scripts MUST be valid and applicable to the target database platform.

---

### User Story 3 - Git Repository Schema Source (Priority: P3)

As a DevOps engineer, I want to retrieve database schemas from a specific git branch or tag in a repository so that I can generate migrations based on historical or future schema versions tracked in version control.

**Why this priority**: This enables schema comparison and migration generation based on version-controlled schema definitions, supporting advanced workflows like reviewing schema changes before deployment and managing schema evolution across branches.

**Independent Test**: Can be fully tested by specifying a git repository URL with a branch or tag, then verifying that schemas are successfully checked out from that version and loaded into the schema provider.

**Acceptance Scenarios**:

1. **Given** a valid git repository URL containing schema files and a specific branch or tag reference, **When** a user configures a git-based schema provider, **Then** the system MUST checkout the specified version and load schema definitions.

2. **Given** a git repository with schema files organized in the expected directory structure, **When** the schema provider retrieves schemas from a specific commit, **Then** it MUST load the exact schema state as it existed at that commit.

3. **Given** schemas retrieved from two different git references (e.g., main branch vs feature branch), **When** the user generates migrations, **Then** the system MUST generate accurate migration scripts reflecting only the differences between those two versions.

---

### User Story 4 - JAR File Embedded Schema Source (Priority: P4)

As a developer, I want to retrieve database schemas from a directory inside a JAR file on the runtime classpath so that I can package schemas with my application and use them for migration generation without external file dependencies.

**Why this priority**: This enables self-contained schema definitions packaged with applications, useful for distributed deployments, testing, and scenarios where schema files need to be bundled with the application.

**Independent Test**: Can be fully tested by including schema files in a JAR file, placing it on the classpath, then configuring the JAR-based schema provider and verifying schemas are successfully extracted and loaded.

**Acceptance Scenarios**:

1. **Given** a JAR file containing schema directories and files (.db and .tbl) in the expected structure, **When** the JAR is added to the runtime classpath and configured as a schema source, **Then** the system MUST successfully read and parse the embedded schema files.

2. **Given** a JAR file with multiple database directories, **When** the schema provider accesses it, **Then** it MUST correctly extract and load all database definitions from within the JAR.

3. **Given** schemas loaded from a JAR file and schemas from another source, **When** the user generates migrations, **Then** the migration scripts MUST correctly reflect differences between the embedded schemas and the comparison source.

---

### User Story 5 - Custom Schema Provider Implementation (Priority: P5)

As a software architect, I want to implement a custom schema provider for a non-standard source (such as cloud database service API or custom storage format) so that I can integrate schema comparison and migration generation into specialized workflows.

**Why this priority**: This provides extensibility for unique use cases and integration scenarios, ensuring the schema provider system can adapt to custom requirements without core system modifications.

**Independent Test**: Can be fully tested by implementing a custom provider following the defined interface, registering it with the system, and verifying it successfully loads schemas from the custom source.

**Acceptance Scenarios**:

1. **Given** a user has implemented a custom schema provider following the standard interface, **When** they register it with the schema provider system, **Then** the system MUST accept and utilize the custom provider for schema retrieval.

2. **Given** a properly implemented custom provider configured with necessary parameters, **When** the system requests schemas, **Then** it MUST return schema data in the standard format expected by the migration generator.

3. **Given** schemas from a custom provider and schemas from a standard provider, **When** the user generates migrations, **Then** the migration scripts MUST be valid and correctly handle differences between the two schema sources.

---

### Edge Cases

- What happens when the schema source is unavailable or inaccessible (network issues, missing files, permissions)?
- How does the system handle corrupted schema files or invalid SQL syntax?
- What occurs when schemas from different sources have incompatible database platforms?
- How does the system handle circular dependencies or foreign key relationships across databases?
- What happens when multiple versions of the same database schema are detected?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a standardized schema provider interface that can be implemented by multiple source types.
- **FR-002**: System MUST support directory-based schema sources containing .db and .tbl files organized in database-named directories.
- **FR-003**: System MUST support live database connections as schema sources, automatically querying database metadata.
- **FR-004**: System MUST support git repository sources, allowing schema retrieval from specific branches or tags.
- **FR-005**: System MUST support JAR file embedded directories as schema sources via runtime classpath access.
- **FR-006**: System MUST support custom user-implemented schema providers through a plugin architecture.
- **FR-006.1**: System MUST provide a programmatic API for configuring schema providers at runtime.
- **FR-007**: Schema files MUST use .db extension for database-level CREATE DATABASE statements with full DDL support.
- **FR-008**: Schema files MUST use .tbl extension for table-level CREATE TABLE statements including complete column definitions, constraints, indexes, and foreign keys.
- **FR-009**: Schema directory structure MUST be organized as: database-name directory containing exactly one .db file and zero or more .tbl files.
- **FR-010**: File names within a database directory MUST match the database name (for .db files) or table names (for .tbl files).
- **FR-011**: System MUST generate database-level migrations that encompass all schema differences rather than single-table operations.
- **FR-012**: System MUST compare complete database schemas and generate comprehensive migration scripts covering all differences.
- **FR-012.1**: System MUST support migration generation between ANY combination of source and target types (directory, live database, git repository, JAR file, custom provider).
- **FR-013**: System MUST handle schema sources from supported database platforms (MySQL, PostgreSQL, MariaDB, SQLite) correctly with proper dialect awareness.
- **FR-014**: System MUST support credential management through environment variables and external secret manager integrations.
- **FR-015**: System MUST provide clear error messages when schema sources are inaccessible or invalid.

### Key Entities

- **Schema Provider**: An interface/class that defines the contract for retrieving schemas from various sources, including configuration requirements and schema retrieval methods.
- **Schema Source**: The origin of database schema definitions, which can be a directory path, database connection, git reference, JAR file path, or custom implementation.
- **Schema Model**: An in-memory representation of database structure including databases, tables, columns, constraints, indexes, and relationships.
- **Migration Script**: Generated database-level scripts (DDL/DML) that transform one schema version to another, covering all necessary changes.
- **Schema File**: A .db or .tbl file containing SQL statements for database or table creation, with file names matching their respective database or table names.

## Clarifications

### Session 2025-12-01

- Q: Which database platforms should be officially supported and tested out-of-the-box? → A: MySQL, PostgreSQL, MariaDB, SQLite (4 major open-source platforms)
- Q: What specific SQL content and structure should be supported in .db and .tbl files? → A: Full CREATE statements with complete DDL (columns, constraints, indexes, foreign keys, etc.)
- Q: How should users configure schema providers? → A: Programmatic API - code-based configuration for maximum flexibility
- Q: Should users be able to generate migrations between ANY source type combination? → A: YES - any source can migrate to any target (full matrix support)
- Q: How should the system handle authentication credentials for live databases and git repositories? → A: Both environment variables and external secret manager support

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can retrieve schemas from any supported source type and generate valid migration scripts within 30 seconds of configuration.
- **SC-002**: Schema provider system supports at least 5 different source types (directory, live database, git, JAR, custom) without requiring system recompilation.
- **SC-003**: Database-level migration generation correctly handles all schema differences including new tables, modified tables, dropped tables, and database-level changes with 100% accuracy.
- **SC-004**: Schema file parsing supports the .db and .tbl file extensions with proper directory structure validation, achieving 100% correct schema loading for valid structures.
- **SC-005**: System generates migration scripts that are executable on the target database platform without manual modification in 95% of standard schema evolution scenarios.
- **SC-006**: Users can configure and use schema providers without requiring documentation beyond basic configuration examples, achieving successful schema retrieval on first attempt.
