package com.aidvps.schemakit.provider;

import com.aidvps.druid.differ.internal.model.Schema;

/** Core interface for all schema source providers. */
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
        if (config == null) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must not be null");
        }
    }
}
