# Tasks: Schema Provider System

**Input**: Design documents from `/specs/001-schema-provider-system/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), data-model.md, contracts/
**Implementation Request**: Multi-module Gradle refactoring with full implementation and unit tests

**Tests**: Test-First Development is NON-NEGOTIABLE per constitution. All tasks include test requirements.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Refactor existing single-module Gradle project to multi-module structure per plan.md

**⚠️ CRITICAL**: This phase MUST be complete before any development can begin

- [ ] T001 Create multi-module Gradle structure with 7 modules in root directory
- [ ] T002 Refactor root build.gradle with common plugins (java, spotless, jacoco)
- [ ] T003 Create settings.gradle with module declarations
- [ ] T004 Create build.gradle for each module with proper dependencies
- [ ] T005 [P] Create package structure for schema-core/src/main/java/com/aidvps/schemakit/core/
- [ ] T006 [P] Create package structure for schema-provider-api/src/main/java/com/aidvps/schemakit/provider/
- [ ] T007 [P] Create package structure for schema-migrator/src/main/java/com/aidvps/schemakit/migrator/
- [ ] T008 Verify all modules compile successfully
- [ ] T009 Run all modules' tests to ensure clean slate

**Checkpoint**: Multi-module Gradle project ready for development

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement core infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Core Schema Model (schema-core module)

- [ ] T010 [P] [US-ALL] Implement Schema class in schema-core/src/main/java/com/aidvps/schemakit/core/Schema.java
- [ ] T011 [P] [US-ALL] Implement Database class in schema-core/src/main/java/com/aidvps/schemakit/core/Database.java
- [ ] T012 [P] [US-ALL] Implement Table class in schema-core/src/main/java/com/aidvps/schemakit/core/Table.java
- [ ] T013 [P] [US-ALL] Implement Column class in schema-core/src/main/java/com/aidvps/schemakit/core/Column.java
- [ ] T014 [P] [US-ALL] Implement DataType class in schema-core/src/main/java/com/aidvps/schemakit/core/DataType.java
- [ ] T015 [P] [US-ALL] Implement Constraint classes in schema-core/src/main/java/com/aidvps/schemakit/core/Constraint.java
- [ ] T016 [P] [US-ALL] Implement Index class in schema-core/src/main/java/com/aidvps/schemakit/core/Index.java
- [ ] T017 [P] [US-ALL] Implement TableProperties class in schema-core/src/main/java/com/aidvps/schemakit/core/TableProperties.java
- [ ] T018 [US-ALL] Implement DatabasePlatform enum in schema-core/src/main/java/com/aidvps/schemakit/core/DatabasePlatform.java
- [ ] T019 [US-ALL] Create builder pattern for all model classes with validation

### Unit Tests for Core Model

- [ ] T020 [P] [US-ALL] Unit tests for Schema class in schema-core/src/test/java/com/aidvps/schemakit/core/SchemaTest.java
- [ ] T021 [P] [US-ALL] Unit tests for Database class in schema-core/src/test/java/com/aidvps/schemakit/core/DatabaseTest.java
- [ ] T022 [P] [US-ALL] Unit tests for Table class in schema-core/src/test/java/com/aidvps/schemakit/core/TableTest.java
- [ ] T023 [P] [US-ALL] Unit tests for Column class in schema-core/src/test/java/com/aidvps/schemakit/core/ColumnTest.java
- [ ] T024 [P] [US-ALL] Unit tests for DataType class in schema-core/src/test/java/com/aidvps/schemakit/core/DataTypeTest.java
- [ ] T025 [P] [US-ALL] Unit tests for Constraint classes in schema-core/src/test/java/com/aidvps/schemakit/core/ConstraintTest.java
- [ ] T026 [P] [US-ALL] Unit tests for Index class in schema-core/src/test/java/com/aidvps/schemakit/core/IndexTest.java
- [ ] T027 [P] [US-ALL] Unit tests for TableProperties class in schema-core/src/test/java/com/aidvps/schemakit/core/TablePropertiesTest.java
- [ ] T028 [P] [US-ALL] Unit tests for DatabasePlatform enum in schema-core/src/test/java/com/aidvps/schemakit/core/DatabasePlatformTest.java
- [ ] T029 [P] [US-ALL] Unit tests for builder validation in schema-core/src/test/java/com/aidvps/schemakit/core/BuilderTest.java

### Provider API (schema-provider-api module)

- [ ] T030 [P] [US-ALL] Implement SchemaProvider interface in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/SchemaProvider.java
- [ ] T031 [P] [US-ALL] Implement SchemaProviderConfig interface in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/SchemaProviderConfig.java
- [ ] T032 [P] [US-ALL] Implement ProviderType enum in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/ProviderType.java
- [ ] T033 [P] [US-ALL] Implement SchemaProviderException in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/SchemaProviderException.java
- [ ] T034 [P] [US-ALL] Implement ConfigValidationException in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/ConfigValidationException.java
- [ ] T035 [P] [US-ALL] Implement SecretProvider interface in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/SecretProvider.java
- [ ] T036 [P] [US-ALL] Implement EnvironmentVariableSecretProvider in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/EnvironmentVariableSecretProvider.java
- [ ] T037 [P] [US-ALL] Implement SchemaProviderFactory in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/SchemaProviderFactory.java

### Provider API Unit Tests

- [ ] T038 [P] [US-ALL] Unit tests for SchemaProvider interface in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/SchemaProviderTest.java
- [ ] T039 [P] [US-ALL] Unit tests for SchemaProviderConfig interface in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/SchemaProviderConfigTest.java
- [ ] T040 [P] [US-ALL] Unit tests for ProviderType enum in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/ProviderTypeTest.java
- [ ] T041 [P] [US-ALL] Unit tests for SchemaProviderFactory in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/SchemaProviderFactoryTest.java
- [ ] T042 [P] [US-ALL] Unit tests for EnvironmentVariableSecretProvider in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/EnvironmentVariableSecretProviderTest.java

### Schema Migrator (schema-migrator module)

- [ ] T043 [P] [US-ALL] Implement SchemaMigrator interface in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/SchemaMigrator.java
- [ ] T044 [P] [US-ALL] Implement MigrationConfig interface in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/MigrationConfig.java
- [ ] T045 [P] [US-ALL] Implement MigrationMode enum in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/MigrationMode.java
- [ ] T046 [P] [US-ALL] Implement MigrationScript class in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/MigrationScript.java
- [ ] T047 [P] [US-ALL] Implement MigrationStatement class in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/MigrationStatement.java
- [ ] T048 [P] [US-ALL] Implement SchemaDiff class in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/SchemaDiff.java
- [ ] T049 [P] [US-ALL] Implement DatabaseDiff class in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/DatabaseDiff.java
- [ ] T050 [P] [US-ALL] Implement TableDiff class in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/TableDiff.java
- [ ] T051 [P] [US-ALL] Implement SchemaChange abstract class in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/SchemaChange.java
- [ ] T052 [P] [US-ALL] Implement MigrationException in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/MigrationException.java

### Schema Migrator Unit Tests

- [ ] T053 [P] [US-ALL] Unit tests for SchemaMigrator interface in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/SchemaMigratorTest.java
- [ ] T054 [P] [US-ALL] Unit tests for MigrationConfig interface in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/MigrationConfigTest.java
- [ ] T055 [P] [US-ALL] Unit tests for MigrationScript class in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/MigrationScriptTest.java
- [ ] T056 [P] [US-ALL] Unit tests for SchemaDiff class in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/SchemaDiffTest.java
- [ ] T057 [P] [US-ALL] Unit tests for DatabaseDiff class in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/DatabaseDiffTest.java
- [ ] T058 [P] [US-ALL] Unit tests for TableDiff class in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/TableDiffTest.java

**Checkpoint**: All foundational components complete and tested - user stories can now begin

---

## Phase 3: User Story 1 - Directory-based Schema Source (Priority: P1) 🎯 MVP

**Goal**: Implement directory-based schema provider supporting .db and .tbl file parsing with migration generation

**Independent Test**: Provide a directory with .db/.tbl files → provider loads schemas → migrator generates SQL

### Tests for User Story 1 (TDD - Write First, Ensure FAIL)

- [ ] T059 [P] [US1] Contract test for directory schema provider in schema-provider-dir/src/test/java/com/aidvps/schemakit/provider/dir/DirectorySchemaProviderContractTest.java
- [ ] T060 [P] [US1] Integration test for directory file parsing in schema-provider-dir/src/test/java/com/aidvps/schemakit/provider/dir/DirectoryFileParsingIntegrationTest.java
- [ ] T061 [P] [US1] Unit test for .db file parser in schema-provider-dir/src/test/java/com/aidvps/schemakit/provider/dir/DatabaseFileParserTest.java
- [ ] T062 [P] [US1] Unit test for .tbl file parser in schema-provider-dir/src/test/java/com/aidvps/schemakit/provider/dir/TableFileParserTest.java
- [ ] T063 [P] [US1] Integration test for directory→migrator workflow in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/DirectoryMigrationIntegrationTest.java

### Directory Provider Implementation

- [ ] T064 [P] [US1] Implement DirectorySchemaProviderConfig interface in schema-provider-dir/src/main/java/com/aidvps/schemakit/provider/dir/DirectorySchemaProviderConfig.java
- [ ] T065 [P] [US1] Implement DirectorySchemaProvider in schema-provider-dir/src/main/java/com/aidvps/schemakit/provider/dir/DirectorySchemaProvider.java
- [ ] T066 [P] [US1] Implement DatabaseFileParser in schema-provider-dir/src/main/java/com/aidvps/schemakit/provider/dir/DatabaseFileParser.java
- [ ] T067 [P] [US1] Implement TableFileParser in schema-provider-dir/src/main/java/com/aidvps/schemakit/provider/dir/TableFileParser.java
- [ ] T068 [P] [US1] Implement DirectoryStructureValidator in schema-provider-dir/src/main/java/com/aidvps/schemakit/provider/dir/DirectoryStructureValidator.java

### Integration with Migrator

- [ ] T069 [US1] Implement DefaultSchemaMigrator in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/DefaultSchemaMigrator.java (depends on T043)
- [ ] T070 [US1] Implement SchemaComparator in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/SchemaComparator.java
- [ ] T071 [US1] Implement SqlGenerator in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/SqlGenerator.java
- [ ] T072 [US1] Implement DependencyAnalyzer in schema-migrator/src/main/java/com/aidvps/schemakit/migrator/DependencyAnalyzer.java

### Integration Tests

- [ ] T073 [US1] End-to-end test: directory→schema→migration in schema-provider-dir/src/test/java/com/aidvps/schemakit/provider/dir/E2EDirectoryToMigrationTest.java
- [ ] T074 [US1] Test schema comparison accuracy (SC-003 validation) in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/SchemaComparisonAccuracyTest.java

**Checkpoint**: User Story 1 complete - directory-based schema retrieval and migration generation working independently

---

## Phase 4: User Story 2 - Live Database Connection Schema Provider (Priority: P2)

**Goal**: Implement live database provider for MySQL, PostgreSQL, MariaDB, SQLite with Testcontainers integration

**Independent Test**: Connect to live database → extract schema → compare with other sources → generate migration

### Tests for User Story 2 (TDD - Write First, Ensure FAIL)

- [ ] T075 [P] [US2] Contract test for database schema provider in schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/DatabaseSchemaProviderContractTest.java
- [ ] T076 [P] [US2] Integration test with MySQL container in schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/MySQLIntegrationTest.java
- [ ] T077 [P] [US2] Integration test with PostgreSQL container in schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/PostgreSQLIntegrationTest.java
- [ ] T078 [P] [US2] Integration test with MariaDB container in schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/MariaDBIntegrationTest.java
- [ ] T079 [P] [US2] Integration test with SQLite integration in schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/SQLiteIntegrationTest.java

### Database Provider Implementation

- [ ] T080 [P] [US2] Implement DatabaseSchemaProviderConfig interface in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/DatabaseSchemaProviderConfig.java
- [ ] T081 [P] [US2] Implement DatabaseSchemaProvider in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/DatabaseSchemaProvider.java
- [ ] T082 [P] [US2] Implement DatabaseMetadataExtractor in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/DatabaseMetadataExtractor.java
- [ ] T083 [P] [US2] Implement DatabaseIntrospector in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/DatabaseIntrospector.java
- [ ] T084 [P] [US2] Implement DialectResolver in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/DialectResolver.java
- [ ] T085 [P] [US2] Implement MySQLIntrospector in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/MySQLIntrospector.java
- [ ] T086 [P] [US2] Implement PostgreSQLIntrospector in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/PostgreSQLIntrospector.java
- [ ] T087 [P] [US2] Implement MariaDBIntrospector in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/MariaDBIntrospector.java
- [ ] T088 [P] [US2] Implement SQLiteIntrospector in schema-provider-db/src/main/java/com/aidvps/schemakit/provider/db/SQLiteIntrospector.java

### Integration Tests

- [ ] T089 [US2] End-to-end test: live database→migration in schema-provider-db/src/test/java/com/aidvps/schemakit/provider/db/LiveDatabaseMigrationE2ETest.java
- [ ] T090 [US2] Test cross-dialect migration (MySQL→PostgreSQL) in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/CrossDialectMigrationTest.java

**Checkpoint**: User Story 2 complete - live database schema extraction working independently with all 4 platforms

---

## Phase 5: User Story 3 - Git Repository Schema Source (Priority: P3)

**Goal**: Implement git repository provider supporting branch/tag/commit references with temporary checkout

**Independent Test**: Clone git repo → checkout branch → load schemas → generate migration

### Tests for User Story 3 (TDD - Write First, Ensure FAIL)

- [ ] T091 [P] [US3] Contract test for git schema provider in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitSchemaProviderContractTest.java
- [ ] T092 [P] [US3] Unit test for git repository cloning in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitRepositoryCloningTest.java
- [ ] T093 [P] [US3] Integration test for branch checkout in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitBranchCheckoutIntegrationTest.java
- [ ] T094 [P] [US3] Integration test for tag reference in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitTagReferenceTest.java
- [ ] T095 [P] [US3] Integration test for commit reference in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitCommitReferenceTest.java

### Git Provider Implementation

- [ ] T096 [P] [US3] Implement GitSchemaProviderConfig interface in schema-provider-git/src/main/java/com/aidvps/schemakit/provider/git/GitSchemaProviderConfig.java
- [ ] T097 [P] [US3] Implement GitSchemaProvider in schema-provider-git/src/main/java/com/aidvps/schemakit/provider/git/GitSchemaProvider.java
- [ ] T098 [P] [US3] Implement GitRepositoryManager in schema-provider-git/src/main/java/com/aidvps/schemakit/provider/git/GitRepositoryManager.java
- [ ] T099 [P] [US3] Implement GitCredentials in schema-provider-git/src/main/java/com/aidvps/schemakit/provider/git/GitCredentials.java
- [ ] T100 [P] [US3] Implement TemporaryRepository in schema-provider-git/src/main/java/com/aidvps/schemakit/provider/git/TemporaryRepository.java
- [ ] T101 [P] [US3] Implement GitReferenceResolver in schema-provider-git/src/main/java/com/aidvps/schemakit/provider/git/GitReferenceResolver.java

### Integration Tests

- [ ] T102 [US3] End-to-end test: git→schema→migration in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitToMigrationE2ETest.java
- [ ] T103 [US3] Test git branch comparison (main vs feature) in schema-provider-git/src/test/java/com/aidvps/schemakit/provider/git/GitBranchComparisonTest.java

**Checkpoint**: User Story 3 complete - git repository schema extraction working independently

---

## Phase 6: User Story 4 - JAR File Embedded Schema Source (Priority: P4)

**Goal**: Implement JAR-embedded provider supporting classpath resource loading

**Independent Test**: Load schema from JAR → extract schemas → generate migration

### Tests for User Story 4 (TDD - Write First, Ensure FAIL)

- [ ] T104 [P] [US4] Contract test for JAR schema provider in schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/JarSchemaProviderContractTest.java
- [ ] T105 [P] [US4] Unit test for JAR resource loading in schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/JarResourceLoaderTest.java
- [ ] T106 [P] [US4] Integration test for embedded schema extraction in schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/EmbeddedSchemaExtractionTest.java
- [ ] T107 [P] [US4] Integration test for multiple databases in JAR in schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/MultipleDatabasesInJarTest.java

### JAR Provider Implementation

- [ ] T108 [P] [US4] Implement JarSchemaProviderConfig interface in schema-provider-jar/src/main/java/com/aidvps/schemakit/provider/jar/JarSchemaProviderConfig.java
- [ ] T109 [P] [US4] Implement JarSchemaProvider in schema-provider-jar/src/main/java/com/aidvps/schemakit/provider/jar/JarSchemaProvider.java
- [ ] T110 [P] [US4] Implement JarResourceExtractor in schema-provider-jar/src/main/java/com/aidvps/schemakit/provider/jar/JarResourceExtractor.java
- [ ] T111 [P] [US4] Implement ClasspathResourceLoader in schema-provider-jar/src/main/java/com/aidvps/schemakit/provider/jar/ClasspathResourceLoader.java
- [ ] T112 [P] [US4] Implement EmbeddedFileReader in schema-provider-jar/src/main/java/com/aidvps/schemakit/provider/jar/EmbeddedFileReader.java

### Integration Tests

- [ ] T113 [US4] End-to-end test: JAR→schema→migration in schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/JarToMigrationE2ETest.java
- [ ] T114 [US4] Test JAR comparison (two versions) in schema-provider-jar/src/test/java/com/aidvps/schemakit/provider/jar/JarVersionComparisonTest.java

**Checkpoint**: User Story 4 complete - JAR-embedded schema extraction working independently

---

## Phase 7: User Story 5 - Custom Schema Provider Implementation (Priority: P5)

**Goal**: Implement custom provider registration and testing framework

**Independent Test**: Register custom provider → implement interface → use in migration generation

### Tests for User Story 5 (TDD - Write First, Ensure FAIL)

- [ ] T115 [P] [US5] Contract test for custom provider registration in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/CustomProviderRegistrationTest.java
- [ ] T116 [P] [US5] Integration test for custom provider usage in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/CustomProviderIntegrationTest.java
- [ ] T117 [P] [US5] Test custom provider isolation in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/CustomProviderIsolationTest.java

### Custom Provider Implementation

- [ ] T118 [P] [US5] Enhance SchemaProviderFactory with registration methods in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/SchemaProviderFactory.java
- [ ] T119 [P] [US5] Create CustomProviderTestKit for testing custom implementations in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/CustomProviderTestKit.java
- [ ] T120 [P] [US5] Document custom provider patterns in docs/CUSTOM_PROVIDER.md
- [ ] T121 [P] [US5] Implement CustomProviderValidator in schema-provider-api/src/main/java/com/aidvps/schemakit/provider/CustomProviderValidator.java

### Integration Tests

- [ ] T122 [US5] End-to-end test: custom provider→migration in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/CustomProviderE2ETest.java
- [ ] T123 [US5] Test mixed source types (custom + standard) in schema-provider-api/src/test/java/com/aidvps/schemakit/provider/MixedSourceTypesTest.java

**Checkpoint**: User Story 5 complete - custom provider framework working independently

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration testing, performance validation, documentation

### Integration Tests Across All Source Types

- [ ] T124 [P] [US-ALL] Integration test: directory→database migration in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/DirectoryToDatabaseMigrationTest.java
- [ ] T125 [P] [US-ALL] Integration test: git→database migration in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/GitToDatabaseMigrationTest.java
- [ ] T126 [P] [US-ALL] Integration test: jar→directory migration in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/JarToDirectoryMigrationTest.java
- [ ] T127 [P] [US-ALL] Integration test: database→git migration in schema-migrator/src/test/java/com/aidvps/schemakit/migrator/DatabaseToGitMigrationTest.java

### Performance Validation (SC-001 and SC-003)

- [ ] T128 [P] [US-ALL] Performance test: 30-second migration generation (SC-001) in tests/performance/MigrationPerformanceTest.java
- [ ] T129 [P] [US-ALL] Performance test: 100% schema accuracy (SC-003) in tests/performance/SchemaAccuracyTest.java
- [ ] T130 [P] [US-ALL] Performance test: large schema handling in tests/performance/LargeSchemaTest.java
- [ ] T131 [P] [US-ALL] Memory usage test for large schemas in tests/performance/MemoryUsageTest.java

### Documentation

- [ ] T132 [P] Update README.md with new multi-module structure
- [ ] T133 [P] Create API documentation in docs/api/
- [ ] T134 [P] Create usage examples in docs/examples/
- [ ] T135 [P] Validate quickstart.md examples work in docs/quickstart.md

### Code Quality

- [ ] T136 [P] Run Spotless formatting on all modules
- [ ] T137 [P] Achieve 80%+ code coverage across all modules
- [ ] T138 [P] Fix all static analysis issues
- [ ] T139 [P] Document all public APIs with JavaDoc

**Checkpoint**: All validation complete - feature ready for production

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - **BLOCKS all user stories**
  - schema-core, schema-provider-api, schema-migrator complete and tested
- **User Stories (Phases 3-7)**: All depend on Foundational phase completion
  - Can proceed in parallel (if staffed) or sequentially in priority order
- **Polish (Phase 8)**: Depends on desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - MVP focus
  - No dependencies on other stories
  - Should be delivered first and tested independently
- **User Story 2 (P2)**: Can start after Foundational
  - Independent testable with Testcontainers
  - May integrate with US1 but not required
- **User Story 3 (P3)**: Can start after Foundational
  - Independent testable with local git repos
  - May integrate with US1/US2 but not required
- **User Story 4 (P4)**: Can start after Foundational
  - Independent testable with embedded JARs
  - May integrate with any story but not required
- **User Story 5 (P5)**: Can start after Foundational
  - Tests custom provider framework
  - Should work with any combination of other sources

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD)
- Core model components first (models → services → integration)
- Each story should be independently functional before moving to next

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational completes, all 5 user stories can start in parallel
- Different user stories can be worked on simultaneously by different team members

---

## Parallel Example: Foundational Phase

```bash
# These can run in parallel (different files, no dependencies):
Task T010: Implement Schema class in schema-core/src/main/java/com/aidvps/schemakit/core/Schema.java
Task T011: Implement Database class in schema-core/src/main/java/com/aidvps/schemakit/core/Database.java
Task T012: Implement Table class in schema-core/src/main/java/com/aidvps/schemakit/core/Table.java
# ... etc for all schema model classes

# Then all unit tests in parallel:
Task T020: Unit tests for Schema class in schema-core/src/test/java/com/aidvps/schemakit/core/SchemaTest.java
Task T021: Unit tests for Database class in schema-core/src/test/java/com/aidvps/schemakit/core/DatabaseTest.java
# ... etc for all schema model tests
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Recommended for initial delivery:**

1. Complete Phase 1: Setup (Multi-module structure)
2. Complete Phase 2: Foundational (schema-core, schema-provider-api, schema-migrator)
3. Complete Phase 3: User Story 1 (Directory-based provider)
4. **STOP and VALIDATE**: Test User Story 1 independently
   - SC-001: 30-second migration generation
   - SC-004: 100% correct schema loading
5. Deploy/demo if ready

**Why User Story 1 first?**
- Most common use case (P1 priority)
- No external dependencies (local files only)
- Easiest to test and debug
- Validates core architecture works

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4 → Test independently → Deploy/Demo
6. Add User Story 5 → Test independently → Deploy/Demo
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. **Sprint 1**: Team completes Setup + Foundational together
2. **Sprint 2**: Parallel development
   - Developer A: User Story 1 (Directory)
   - Developer B: User Story 2 (Database)
   - Developer C: User Story 3 (Git)
3. **Sprint 3**: Continue parallel
   - Developer A: User Story 4 (JAR) or polish
   - Developer B: User Story 5 (Custom) or polish
   - Developer C: Integration tests, performance validation

---

## Success Criteria Validation

### SC-001: Performance
- **Test**: T128 - 30-second migration generation
- **Measurement**: Time from configuration to migration script generation
- **Target**: <30 seconds for typical schemas

### SC-002: Source Type Support
- **Test**: All contract tests pass for 5 provider types
- **Verification**: No recompilation needed to add providers

### SC-003: Accuracy
- **Test**: T129 - 100% schema accuracy
- **Measurement**: Correct detection of all schema differences
- **Target**: 100% accuracy in schema comparison

### SC-004: File Structure Support
- **Test**: T060, T061, T062 - .db/.tbl parsing
- **Verification**: 100% correct loading for valid structures
- **Target**: SC-004 (100% correct schema loading)

### SC-005: Executable Migrations
- **Test**: All migration E2E tests
- **Verification**: Generated SQL executes without manual modification
- **Target**: 95% success rate for standard scenarios

### SC-006: Ease of Use
- **Test**: T135 - quickstart.md validation
- **Verification**: Users succeed with basic configuration examples
- **Target**: First-attempt success

---

## Notes

- [P] tasks = parallelizable (different files, no dependencies)
- [US-ALL] = needed by all user stories (foundational)
- [US1-US5] = specific to user story
- Each user story independently testable
- Verify tests fail before implementing (TDD)
- Commit after each task or logical group
- Stop at checkpoints to validate story independently
- Run `./gradlew test` frequently to verify progress
