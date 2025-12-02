package com.aidvps.schemakit.provider;

import com.aidvps.druid.differ.DatabaseDialect;
import java.util.Map;

/** Base configuration interface. Providers extend this for type-specific config. */
public interface SchemaProviderConfig {
    /**
     * Get the database dialect for schema generation.
     *
     * @return Database dialect, may be null if not specified
     */
    default DatabaseDialect getDialect() {
        return null;
    }

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
