package com.aidvps.schemakit.provider;

import java.util.Map;

/** Base configuration interface. Providers extend this for type-specific config. */
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
