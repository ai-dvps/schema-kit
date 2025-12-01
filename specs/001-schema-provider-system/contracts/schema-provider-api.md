# Contract: SchemaProvider API

**Module**: schema-provider-api | **Version**: 1.0.0 | **Date**: 2025-12-01

## Overview

The SchemaProvider API defines the contract for all schema source providers. All providers (directory, database, git, JAR, custom) must implement this interface.

## Core Interface

### SchemaProvider

```java
public interface SchemaProvider {
    /**
     * Retrieve a schema from this provider's source.
     *
     * @param config Provider-specific configuration
     * @return Schema instance representing the source schema
     * @throws SchemaProviderException if retrieval fails
     */
    Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException;

    /**
     * Get the type of this provider.
     *
     * @return ProviderType (DIRECTORY, DATABASE, GIT, JAR, CUSTOM)
     */
    ProviderType getType();

    /**
     * Validate configuration without retrieving schema.
     *
     * @param config Configuration to validate
     * @throws SchemaProviderException if configuration is invalid
     */
    default void validateConfig(SchemaProviderConfig config) throws SchemaProviderException {
        // Default implementation checks required fields
        // Override for custom validation
    }
}
```

**Contract**:
- Must be thread-safe (concurrent getSchema calls)
- Must validate configuration in validateConfig and getSchema
- Must throw SchemaProviderException on errors
- Must support cancellation (interrupted threads)

---

## Configuration

### SchemaProviderConfig

Base configuration interface. Providers extend this for type-specific config.

```java
public interface SchemaProviderConfig {
    /**
     * Get configuration as map.
     *
     * @return Immutable configuration map
     */
    Map<String, Object> toMap();

    /**
     * Validate this configuration.
     *
     * @throws ConfigValidationException if invalid
     */
    default void validate() throws ConfigValidationException {
        // Default: no-op
    }
}
```

### DirectorySchemaProviderConfig

Configuration for directory-based provider.

```java
public interface DirectorySchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get the directory path.
     *
     * @return Path to directory containing schema files
     */
    Path getPath();

    /**
     * Whether to validate file structure.
     *
     * @return true to validate, false to skip
     */
    boolean isValidateStructure();

    /**
     * Get file encoding.
     *
     * @return Character encoding (default: UTF-8)
     */
    String getEncoding();

    /**
     * Create builder for DirectorySchemaProviderConfig.
     */
    static Builder builder() { ... }

    interface Builder {
        Builder path(Path path);
        Builder validateStructure(boolean validate);
        Builder encoding(String encoding);
        DirectorySchemaProviderConfig build();
    }
}
```

### DatabaseSchemaProviderConfig

Configuration for live database provider.

```java
public interface DatabaseSchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get the data source.
     *
     * @return JDBC DataSource
     */
    DataSource getDataSource();

    /**
     * Get included databases.
     *
     * @return List of database names to include (null/empty for all)
     */
    List<String> getIncludedDatabases();

    /**
     * Get excluded databases.
     *
     * @return List of database names to exclude
     */
    List<String> getExcludedDatabases();

    /**
     * Get credential provider.
     *
     * @return SecretProvider for credentials
     */
    SecretProvider getCredentialProvider();

    /**
     * Create builder.
     */
    static Builder builder() { ... }

    interface Builder {
        Builder dataSource(DataSource ds);
        Builder includeDatabases(String... dbs);
        Builder excludeDatabases(String... dbs);
        Builder credentialProvider(SecretProvider provider);
        DatabaseSchemaProviderConfig build();
    }
}
```

### GitSchemaProviderConfig

Configuration for git repository provider.

```java
public interface GitSchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get repository URL.
     *
     * @return Git repository URL
     */
    String getRepositoryUrl();

    /**
     * Get branch/tag/commit reference.
     *
     * @return Git reference (branch, tag, or commit hash)
     */
    String getReference();

    /**
     * Get local directory for checkout.
     *
     * @return Path for checkout (temporary if not specified)
     */
    Path getLocalPath();

    /**
     * Get authentication credentials.
     *
     * @return Optional credentials
     */
    Optional<GitCredentials> getCredentials();

    /**
     * Create builder.
     */
    static Builder builder() { ... }

    interface Builder {
        Builder repositoryUrl(String url);
        Builder reference(String ref);
        Builder localPath(Path path);
        Builder credentials(GitCredentials creds);
        GitSchemaProviderConfig build();
    }
}
```

### JarSchemaProviderConfig

Configuration for JAR-embedded provider.

```java
public interface JarSchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get JAR file path or classpath pattern.
     *
     * @return JAR path or classpath resource path
     */
    String getJarPath();

    /**
     * Get base path in JAR for schema directories.
     *
     * @return Base path in JAR (e.g., "schemas/")
     */
    String getBasePath();

    /**
     * Get classloader for resource loading.
     *
     * @return ClassLoader (default: thread context classloader)
     */
    ClassLoader getClassLoader();

    /**
     * Create builder.
     */
    static Builder builder() { ... }

    interface Builder {
        Builder jarPath(String path);
        Builder basePath(String path);
        Builder classLoader(ClassLoader cl);
        JarSchemaProviderConfig build();
    }
}
```

---

## Exceptions

### SchemaProviderException

Base exception for all provider errors.

```java
public class SchemaProviderException extends Exception {
    private final ErrorCode errorCode;
    private final String providerType;

    public enum ErrorCode {
        CONFIG_INVALID,
        SOURCE_NOT_FOUND,
        SOURCE_INACCESSIBLE,
        PARSE_ERROR,
        VALIDATION_ERROR,
        NETWORK_ERROR,
        AUTHENTICATION_FAILED,
        QUOTA_EXCEEDED,
        TIMEOUT,
        UNKNOWN_ERROR
    }

    // Constructors
    public SchemaProviderException(ErrorCode code, String message)
    public SchemaProviderException(ErrorCode code, String message, Throwable cause)
}
```

### ConfigValidationException

Thrown when configuration is invalid.

```java
public class ConfigValidationException extends SchemaProviderException {
    private final List<ValidationError> errors;

    public static class ValidationError {
        private final String field;
        private final String message;
        private final Object invalidValue;
    }
}
```

---

## Provider Type

### ProviderType

Enum identifying provider type.

```java
public enum ProviderType {
    DIRECTORY("Directory-based file provider"),
    DATABASE("Live database connection provider"),
    GIT("Git repository provider"),
    JAR("JAR-embedded file provider"),
    CUSTOM("Custom provider");

    private final String description;

    // Methods
    public String getDescription() { ... }
}
```

---

## Secret Management

### SecretProvider

Interface for credential retrieval.

```java
public interface SecretProvider {
    /**
     * Get a secret value.
     *
     * @param key Secret key
     * @return Secret value
     * @throws SecretNotFoundException if key doesn't exist
     */
    String getSecret(String key) throws SecretNotFoundException;

    /**
     * Check if secret exists.
     *
     * @param key Secret key
     * @return true if exists
     */
    boolean hasSecret(String key);
}
```

### EnvironmentVariableSecretProvider

Built-in implementation using environment variables.

```java
public class EnvironmentVariableSecretProvider implements SecretProvider {
    public EnvironmentVariableSecretProvider() {
        // Uses System.getenv()
    }

    @Override
    public String getSecret(String key) {
        return System.getenv(key);
    }

    @Override
    public boolean hasSecret(String key) {
        return System.getenv().containsKey(key);
    }
}
```

---

## Lifecycle

### SchemaProviderFactory

Factory for creating providers.

```java
public final class SchemaProviderFactory {
    /**
     * Create provider by type.
     *
     * @param type Provider type
     * @return Provider instance
     * @throws IllegalArgumentException if type not supported
     */
    public static SchemaProvider createProvider(ProviderType type) {
        // Returns appropriate provider implementation
    }

    /**
     * Register custom provider.
     *
     * @param type Provider type
     * @param provider Provider implementation
     */
    public static void registerProvider(ProviderType type, SchemaProvider provider) {
        // Custom provider registration
    }
}
```

---

## Usage Examples

### Directory Provider

```java
SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.DIRECTORY);

DirectorySchemaProviderConfig config = DirectorySchemaProviderConfig.builder()
    .path(Paths.get("/path/to/schemas"))
    .validateStructure(true)
    .encoding("UTF-8")
    .build();

Schema schema = provider.getSchema(config);
```

### Database Provider

```java
SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.DATABASE);

DatabaseSchemaProviderConfig config = DatabaseSchemaProviderConfig.builder()
    .dataSource(myDataSource)
    .includeDatabases("prod", "staging")
    .credentialProvider(new EnvironmentVariableSecretProvider())
    .build();

Schema schema = provider.getSchema(config);
```

### Git Provider

```java
SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.GIT);

GitSchemaProviderConfig config = GitSchemaProviderConfig.builder()
    .repositoryUrl("https://github.com/user/repo.git")
    .reference("main")
    .credentials(GitCredentials.builder()
        .username("user")
        .password("token")
        .build())
    .build();

Schema schema = provider.getSchema(config);
```

### Custom Provider

```java
public class MyCustomProvider implements SchemaProvider {
    @Override
    public Schema getSchema(SchemaProviderConfig config) {
        // Custom implementation
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }
}

// Register and use
SchemaProviderFactory.registerProvider(ProviderType.CUSTOM, new MyCustomProvider());
SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.CUSTOM);
```

---

## Contract Testing

All providers must pass these contract tests:

1. **Configuration Validation**
   - Invalid configuration throws ConfigValidationException
   - Valid configuration passes validation

2. **Schema Retrieval**
   - Successful retrieval returns valid Schema
   - Failed retrieval throws SchemaProviderException with proper error code

3. **Thread Safety**
   - Concurrent calls to getSchema are safe
   - No race conditions in configuration validation

4. **Error Handling**
   - All exceptions are SchemaProviderException or subclass
   - Error codes are accurate
   - Messages are user-friendly

5. **Resource Management**
   - Cleanup after schema retrieval
   - Temporary files removed (git provider)
   - Connections closed (database provider)

6. **Performance**
   - getSchema completes within 30 seconds for typical schemas
   - No memory leaks
   - Proper streaming for large schemas

---

## Versioning

**Breaking Changes**: Increment major version, maintain compatibility for one major version
**Backward Compatibility**: Older configurations must work with newer implementations
**Deprecation**: Deprecated methods marked @Deprecated, removed after 2 major versions

**Version History**:
- 1.0.0: Initial API
