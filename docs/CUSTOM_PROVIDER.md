# Custom Schema Provider Implementation Guide

**Version**: 1.0.0 | **Last Updated**: 2025-12-02

This guide explains how to implement custom schema providers for the schema-kit-v2 system.

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Provider Contract](#provider-contract)
4. [Implementation Patterns](#implementation-patterns)
5. [Configuration](#configuration)
6. [Testing](#testing)
7. [Error Handling](#error-handling)
8. [Best Practices](#best-practices)
9. [Examples](#examples)
10. [Troubleshooting](#troubleshooting)

## Overview

Custom schema providers allow you to integrate schema sources beyond the built-in providers (directory, database, git, JAR). Any source that can provide database schema information can be integrated as a custom provider.

### When to Use Custom Providers

- **Cloud database services**: DynamoDB, CosmosDB, Firestore
- **ORM frameworks**: Hibernate, Entity Framework
- **API-based sources**: REST APIs, GraphQL endpoints
- **Configuration files**: YAML, JSON, XML formats
- **Legacy systems**: Mainframe, NoSQL databases
- **Custom formats**: Proprietary schema definitions

## Quick Start

### 1. Create a Provider Class

```java
public class MyCustomProvider implements SchemaProvider {
    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        MyCustomConfig myConfig = (MyCustomConfig) config;

        // Extract schema from your source
        return buildSchemaFromSource(myConfig);
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }
}
```

### 2. Create Configuration Interface

```java
public interface MyCustomConfig extends SchemaProviderConfig {
    String getConnectionString();
    String getApiKey();

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private String connectionString;
        private String apiKey;

        public Builder connectionString(String connectionString) {
            this.connectionString = connectionString;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public MyCustomConfig build() {
            return new MyCustomConfig() {
                @Override
                public String getConnectionString() {
                    return connectionString;
                }

                @Override
                public String getApiKey() {
                    return apiKey;
                }

                @Override
                public Map<String, Object> toMap() {
                    Map<String, Object> map = new HashMap<>();
                    map.put("connectionString", connectionString);
                    map.put("apiKey", apiKey);
                    return map;
                }

                @Override
                public void validate() throws ConfigValidationException {
                    if (connectionString == null || connectionString.trim().isEmpty()) {
                        throw new ConfigValidationException("Connection string is required");
                    }
                    if (apiKey == null || apiKey.trim().isEmpty()) {
                        throw new ConfigValidationException("API key is required");
                    }
                }
            };
        }
    }
}
```

### 3. Register and Use

```java
// Register your custom provider
MyCustomProvider provider = new MyCustomProvider();
SchemaProviderFactory.registerProvider(ProviderType.CUSTOM, provider);

// Use the provider
SchemaProvider registeredProvider = SchemaProviderFactory.createProvider(ProviderType.CUSTOM);

MyCustomConfig config = MyCustomConfig.builder()
    .connectionString("my-source://connection")
    .apiKey("my-api-key")
    .build();

Schema schema = registeredProvider.getSchema(config);
```

## Provider Contract

All custom providers must implement the `SchemaProvider` interface:

### Required Methods

#### `Schema getSchema(SchemaProviderConfig config)`

**Purpose**: Retrieve schema from the source

**Parameters**:
- `config`: Provider-specific configuration

**Returns**: `Schema` instance representing the source schema

**Throws**:
- `SchemaProviderException`: If retrieval fails
- `IllegalArgumentException`: If config is wrong type

**Contract**:
- Must validate configuration before processing
- Must be thread-safe (can be called concurrently)
- Must complete within 30 seconds (SC-001)
- Must return valid, complete schema
- Must throw appropriate exceptions for errors

#### `ProviderType getType()`

**Purpose**: Identify the provider type

**Returns**: `ProviderType.CUSTOM` for all custom providers

### Optional Methods

#### `void validateConfig(SchemaProviderConfig config)`

**Purpose**: Pre-validate configuration without retrieving schema

**Default**: Checks required fields

**Override**: Add custom validation logic

**Throws**: `SchemaProviderException` if invalid

## Implementation Patterns

### Pattern 1: API-Based Provider

For providers that fetch schema via API:

```java
public class ApiBasedProvider implements SchemaProvider {
    private final HttpClient httpClient;

    public ApiBasedProvider() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        ApiConfig apiConfig = (ApiConfig) config;

        try {
            // Call API endpoint
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiConfig.getEndpoint()))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "API request failed with status: " + response.statusCode());
            }

            // Parse response
            return parseApiResponse(response.body(), apiConfig);

        } catch (Exception e) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.NETWORK_ERROR,
                "Failed to fetch schema from API", e);
        }
    }

    private Schema parseApiResponse(String json, ApiConfig config) {
        // Parse JSON and build schema
        // Return Schema instance
    }
}
```

**Best Practices**:
- Use connection pooling for HTTP clients
- Implement retry logic with exponential backoff
- Cache responses to avoid repeated API calls
- Handle rate limiting gracefully

### Pattern 2: File-Based Provider

For providers that read from custom file formats:

```java
public class CustomFileProvider implements SchemaProvider {
    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        CustomFileConfig fileConfig = (CustomFileConfig) config;
        Path path = fileConfig.getPath();

        if (!Files.exists(path)) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.SOURCE_NOT_FOUND,
                "File not found: " + path);
        }

        try {
            String content = Files.readString(path, fileConfig.getEncoding());
            return parseFileContent(content, fileConfig);
        } catch (IOException e) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                "Failed to read file", e);
        }
    }

    private Schema parseFileContent(String content, CustomFileConfig config) {
        // Parse custom file format
        // Return Schema instance
    }
}
```

**Best Practices**:
- Support multiple encodings (UTF-8, ISO-8859-1, etc.)
- Validate file structure before parsing
- Handle large files with streaming
- Support compression (gzip, zip)

### Pattern 3: Database Connection Provider

For providers that connect to databases:

```java
public class CustomDbProvider implements SchemaProvider {
    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        CustomDbConfig dbConfig = (CustomDbConfig) config;

        try (Connection conn = dbConfig.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Extract schema information
            return extractSchema(metaData, dbConfig);

        } catch (SQLException e) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                "Database connection failed", e);
        }
    }

    private Schema extractSchema(DatabaseMetaData metaData, CustomDbConfig config) throws SQLException {
        // Query database metadata
        // Build schema model
        // Return Schema instance
    }
}
```

**Best Practices**:
- Always close connections (use try-with-resources)
- Use connection pools for performance
- Handle transaction isolation
- Support multiple database dialects

### Pattern 4: Caching Provider

For providers that benefit from caching:

```java
public class CachedProvider implements SchemaProvider {
    private final SchemaProvider delegate;
    private final Cache<String, Schema> cache;

    public CachedProvider(SchemaProvider delegate, Cache<String, Schema> cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        String cacheKey = generateCacheKey(config);

        return cache.get(cacheKey, () -> {
            try {
                return delegate.getSchema(config);
            } catch (Exception e) {
                throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.UNKNOWN_ERROR,
                    "Delegate provider failed", e);
            }
        });
    }

    private String generateCacheKey(SchemaProviderConfig config) {
        return Integer.toString(config.toMap().hashCode());
    }
}
```

**Best Practices**:
- Use cache invalidation strategies
- Set appropriate TTL (time-to-live)
- Handle cache misses gracefully
- Monitor cache hit rates

## Configuration

### Configuration Interface Pattern

```java
public interface MyConfig extends SchemaProviderConfig {
    // Getters for configuration properties

    // Static builder method
    static Builder builder() {
        return new Builder();
    }

    // Builder interface
    interface Builder {
        // Fluent setter methods
        Builder propertyName(Type value);

        // Build method
        MyConfig build();
    }
}
```

### Validation Pattern

```java
@Override
public void validate() throws ConfigValidationException {
    List<String> errors = new ArrayList<>();

    if (property1 == null) {
        errors.add("Property1 is required");
    }

    if (property2 != null && property2 < 0) {
        errors.add("Property2 must be non-negative");
    }

    if (!errors.isEmpty()) {
        throw new ConfigValidationException(
            "Configuration validation failed: " + String.join(", ", errors));
    }
}
```

### Required Configuration Properties

Every configuration should include:

1. **Connection information**: URL, endpoint, path
2. **Credentials**: API keys, passwords (use SecretProvider)
3. **Options**: Encoding, timeout, retry settings

### Optional Configuration Properties

- **Caching**: Enable/disable, TTL settings
- **Filtering**: Include/exclude patterns
- **Metadata**: Custom tags, labels

## Testing

### Unit Testing

Use `CustomProviderTestKit` for contract testing:

```java
@Test
void testMyCustomProvider() throws Exception {
    MyConfig config = MyConfig.builder()
        .connectionString("test://connection")
        .apiKey("test-key")
        .build();

    MyCustomProvider provider = new MyCustomProvider();

    CustomProviderTestKit.testProvider(provider)
        .withValidConfig(config)
        .withInvalidConfig(invalidConfig)
        .runAllTests();
}
```

### Integration Testing

Test with real data sources:

```java
@Test
void testWithRealSource() throws Exception {
    MyProvider provider = new MyProvider();
    SchemaProviderFactory.registerProvider(ProviderType.CUSTOM, provider);

    MyConfig config = MyConfig.builder()
        .connectionString(System.getenv("TEST_DB_URL"))
        .build();

    Schema schema = provider.getSchema(config);

    assertNotNull(schema);
    assertFalse(schema.getDatabases().isEmpty());
}
```

### Test Coverage

Ensure tests cover:

- [ ] Valid configuration
- [ ] Invalid configuration
- [ ] Null configuration
- [ ] Connection failures
- [ ] Schema parsing
- [ ] Error handling
- [ ] Thread safety
- [ ] Resource cleanup

## Error Handling

### Exception Hierarchy

```
SchemaProviderException (base)
├── ConfigValidationException (configuration errors)
└── Other subclasses (optional)
```

### Error Codes

Use appropriate error codes:

- `CONFIG_INVALID`: Configuration is malformed or missing required fields
- `SOURCE_NOT_FOUND`: Source (file, endpoint, database) doesn't exist
- `SOURCE_INACCESSIBLE`: Source exists but can't be accessed
- `PARSE_ERROR`: Source is accessible but can't be parsed
- `VALIDATION_ERROR`: Schema validation failed
- `NETWORK_ERROR`: Network-related errors (API providers)
- `AUTHENTICATION_FAILED`: Credentials invalid or expired
- `TIMEOUT`: Operation exceeded time limit
- `UNKNOWN_ERROR`: Unexpected errors

### Error Handling Best Practices

1. **Be specific**: Use appropriate error codes
2. **Include context**: Add meaningful messages
3. **Log errors**: Include details for debugging
4. **Handle retries**: Implement retry logic where appropriate
5. **Fail fast**: Validate early and throw immediately

```java
@Override
public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
    // Validate config first
    if (config == null) {
        throw new SchemaProviderException(
            SchemaProviderException.ErrorCode.CONFIG_INVALID,
            "Configuration cannot be null");
    }

    if (!(config instanceof MyConfig)) {
        throw new SchemaProviderException(
            SchemaProviderException.ErrorCode.CONFIG_INVALID,
            "Expected MyConfig, got " + config.getClass().getName());
    }

    MyConfig myConfig = (MyConfig) config;

    try {
        // Try to get schema
        return doGetSchema(myConfig);
    } catch (SQLException e) {
        throw new SchemaProviderException(
            SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
            "Database connection failed", e);
    } catch (ParseException e) {
        throw new SchemaProviderException(
            SchemaProviderException.ErrorCode.PARSE_ERROR,
            "Failed to parse schema", e);
    } catch (Exception e) {
        throw new SchemaProviderException(
            SchemaProviderException.ErrorCode.UNKNOWN_ERROR,
            "Unexpected error", e);
    }
}
```

## Best Practices

### 1. Thread Safety

Providers must be thread-safe:

```java
// GOOD: Stateless or properly synchronized
public class ThreadSafeProvider implements SchemaProvider {
    // No mutable state
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Schema getSchema(SchemaProviderConfig config) {
        // Each call creates new connection/objects
        return buildSchema(config);
    }
}

// BAD: Shared mutable state without synchronization
public class UnsafeProvider implements SchemaProvider {
    private Connection sharedConnection; // Not thread-safe!

    public Schema getSchema(SchemaProviderConfig config) {
        return sharedConnection.query(...); // Race condition!
    }
}
```

### 2. Resource Management

Always clean up resources:

```java
public class ResourceManagedProvider implements SchemaProvider {
    @Override
    public Schema getSchema(SchemaProviderConfig config) {
        Connection conn = null;
        try {
            conn = createConnection(config);
            return extractSchema(conn);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Log but don't throw
                }
            }
        }
    }
}
```

Better: Use try-with-resources:

```java
@Override
public Schema getSchema(SchemaProviderConfig config) {
    try (Connection conn = createConnection(config)) {
        return extractSchema(conn);
    } catch (SQLException e) {
        throw new SchemaProviderException(...);
    }
}
```

### 3. Performance

- **Cache schemas** when source doesn't change frequently
- **Use connection pools** for database providers
- **Implement streaming** for large schemas
- **Set timeouts** to prevent hanging
- **Lazy initialization** of expensive resources

### 4. Security

- **Never log sensitive data** (passwords, API keys)
- **Use SecretProvider** for credentials
- **Validate input** to prevent injection attacks
- **Use secure connections** (HTTPS, TLS)
- **Follow principle of least privilege** for database access

### 5. Configurability

- **Provide sensible defaults** for optional properties
- **Support environment variables** via SecretProvider
- **Document all configuration options**
- **Validate configuration early** (in builder or validate method)

## Examples

### Example 1: DynamoDB Provider

```java
public class DynamoDbProvider implements SchemaProvider {
    private final AmazonDynamoDB dynamoDb;

    public DynamoDbProvider(AmazonDynamoDB dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        DynamoDbConfig dbConfig = (DynamoDbConfig) config;

        List<String> tableNames = listTables(dbConfig);

        Map<String, Table> tables = new HashMap<>();
        for (String tableName : tableNames) {
            TableInfo tableInfo = getTableInfo(tableName, dbConfig);
            tables.put(tableName, tableInfo);
        }

        return Schema.builder()
            .platform(DatabasePlatform.CUSTOM)
            .database(Database.builder()
                .name(dbConfig.getDatabaseName())
                .tables(tables)
                .build())
            .build();
    }

    private List<String> listTables(DynamoDbConfig config) {
        // Implementation for listing DynamoDB tables
    }

    private Table getTableInfo(String tableName, DynamoDbConfig config) {
        // Implementation for getting table schema
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }
}
```

### Example 2: REST API Provider

```java
public class RestApiProvider implements SchemaProvider {
    private final RestTemplate restTemplate;

    public RestApiProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        ApiConfig apiConfig = (ApiConfig) config;

        String url = apiConfig.getBaseUrl() + "/schema";

        try {
            SchemaDto response = restTemplate.getForObject(url, SchemaDto.class);
            return convertToSchema(response, apiConfig);
        } catch (RestClientException e) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.NETWORK_ERROR,
                "Failed to fetch schema from API", e);
        }
    }

    private Schema convertToSchema(SchemaDto dto, ApiConfig config) {
        // Convert DTO to Schema model
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }
}
```

### Example 3: YAML Configuration Provider

```java
public class YamlConfigProvider implements SchemaProvider {
    private final ObjectMapper yamlMapper;

    public YamlConfigProvider() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        YamlConfig yamlConfig = (YamlConfig) config;

        try {
            Map<String, Object> yaml = yamlMapper.readValue(
                new File(yamlConfig.getPath()),
                Map.class
            );

            return parseYamlSchema(yaml, yamlConfig);
        } catch (IOException e) {
            throw new SchemaProviderException(
                SchemaProviderException.ErrorCode.PARSE_ERROR,
                "Failed to parse YAML file", e);
        }
    }

    private Schema parseYamlSchema(Map<String, Object> yaml, YamlConfig config) {
        // Parse YAML structure and build schema
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CUSTOM;
    }
}
```

## Troubleshooting

### Common Issues

#### 1. "Provider type not supported" Error

**Cause**: Provider not registered before use

**Solution**:
```java
SchemaProviderFactory.registerProvider(ProviderType.CUSTOM, myProvider);
SchemaProvider provider = SchemaProviderFactory.createProvider(ProviderType.CUSTOM);
```

#### 2. ClassCastException for Config

**Cause**: Incorrect config type cast

**Solution**:
```java
@Override
public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
    if (!(config instanceof MyConfig)) {
        throw new SchemaProviderException(
            SchemaProviderException.ErrorCode.CONFIG_INVALID,
            "Expected MyConfig");
    }
    MyConfig myConfig = (MyConfig) config;
    // ...
}
```

#### 3. Provider Slow Performance

**Cause**: No caching or inefficient operations

**Solution**:
- Implement caching layer
- Use connection pooling
- Add timeouts
- Stream large results

#### 4. Thread Safety Issues

**Cause**: Shared mutable state

**Solution**:
```java
// Use thread-local or make stateless
private final ThreadLocal<Connection> connection = new ThreadLocal<>();

// Or synchronize access
private synchronized void updateSharedState() {
    // ...
}
```

### Debugging Tips

1. **Enable logging**:
   ```java
   private static final Logger logger = LoggerFactory.getLogger(MyProvider.class);

   try {
       // operation
   } catch (Exception e) {
       logger.error("Operation failed", e);
       throw e;
   }
   ```

2. **Validate configuration early**:
   ```java
   @Override
   public void validateConfig(SchemaProviderConfig config) throws SchemaProviderException {
       if (config == null) {
           throw new SchemaProviderException(...);
       }
       // validate...
   }
   ```

3. **Test with CustomProviderTestKit**:
   ```java
   CustomProviderTestKit.testProvider(myProvider)
       .withValidConfig(validConfig)
       .runAllTests();
   ```

## Additional Resources

- **SchemaProvider API Documentation**: See `schema-provider-api.md`
- **Data Model Guide**: See `data-model.md`
- **Testing Guide**: See `testing.md`
- **Migration Guide**: See `migration.md`

## Support

For issues and questions:

1. Check existing [GitHub Issues](https://github.com/your-org/schema-kit-v2/issues)
2. Review this documentation
3. Contact the development team

## License

This documentation is part of the schema-kit-v2 project. See the project license for details.
