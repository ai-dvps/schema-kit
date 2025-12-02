# API Documentation

This directory contains generated API documentation for schema-kit-v2.

## Modules

### schema-core

Core schema model classes:
- `Schema` - Root schema entity
- `Database` - Database entity
- `Table` - Table entity
- `Column` - Column entity
- `DataType` - Data type with dialect support
- `Constraint` - Table constraints (PrimaryKey, ForeignKey, Unique, Check)
- `Index` - Database indexes
- `DatabasePlatform` - Platform enum (MYSQL, POSTGRESQL, MARIADB, SQLITE)

### schema-provider-api

Provider interfaces and contracts:
- `SchemaProvider` - Main provider interface
- `SchemaProviderConfig` - Base configuration interface
- `SchemaProviderFactory` - Provider factory with registration
- `ProviderType` - Provider type enum
- `SchemaProviderException` - Base exception
- `ConfigValidationException` - Configuration validation exception
- `SecretProvider` - Credential management interface
- `EnvironmentVariableSecretProvider` - Env var implementation

### schema-provider-dir

Directory-based provider:
- `DirectorySchemaProvider` - Main provider class
- `DirectorySchemaProviderConfig` - Configuration
- `DatabaseFileParser` - .db file parser
- `TableFileParser` - .tbl file parser
- `DirectoryStructureValidator` - Directory structure validator

### schema-provider-db

Live database provider:
- `DatabaseSchemaProvider` - Main provider class
- `DatabaseSchemaProviderConfig` - Configuration
- `DatabaseMetadataExtractor` - Metadata extraction
- `DatabaseIntrospector` - Database introspection
- `DialectResolver` - SQL dialect resolution
- `MySQLIntrospector` - MySQL-specific introspection
- `PostgreSQLIntrospector` - PostgreSQL-specific introspection
- `MariaDBIntrospector` - MariaDB-specific introspection
- `SQLiteIntrospector` - SQLite-specific introspection

### schema-provider-git

Git repository provider:
- `GitSchemaProvider` - Main provider class
- `GitSchemaProviderConfig` - Configuration
- `GitRepositoryManager` - Repository management
- `GitCredentials` - Authentication credentials
- `TemporaryRepository` - Temporary checkout
- `GitReferenceResolver` - Reference resolution

### schema-provider-jar

JAR-embedded provider:
- `JarSchemaProvider` - Main provider class
- `JarSchemaProviderConfig` - Configuration
- `JarResourceExtractor` - Resource extraction
- `ClasspathResourceLoader` - Classpath loading
- `EmbeddedFileReader` - File reading utilities

### schema-migrator

Migration generation:
- `SchemaMigrator` - Main migrator interface
- `DefaultSchemaMigrator` - Default implementation
- `MigrationConfig` - Migration configuration
- `MigrationMode` - Migration mode enum
- `MigrationScript` - Generated migration
- `MigrationStatement` - Individual statements
- `SchemaDiff` - Schema difference
- `DatabaseDiff` - Database-level diff
- `TableDiff` - Table-level diff
- `SchemaChange` - Abstract change representation
- `SchemaComparator` - Schema comparison
- `SqlGenerator` - SQL generation
- `DependencyAnalyzer` - Dependency analysis
- `MigrationException` - Migration exception

### Custom Provider Support

- `CustomProviderTestKit` - Testing framework
- `CustomProviderValidator` - Validation utilities

## Usage Examples

See `/docs/examples/` for complete usage examples.

## Generation

API documentation can be generated using Javadoc:

```bash
./gradlew javadoc
```

Generated documentation will be available in `build/docs/javadoc/`.
