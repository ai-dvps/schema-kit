package com.aidvps.schemakit.provider;

import java.util.HashMap;
import java.util.Map;

/** Factory for creating providers. */
public final class SchemaProviderFactory {
    private static final Map<ProviderType, SchemaProvider> providers = new HashMap<>();

    static {
        // Register built-in providers (to be implemented in Phase 3-6)
        // providers.put(ProviderType.DIRECTORY, new DirectorySchemaProvider());
        // providers.put(ProviderType.DATABASE, new DatabaseSchemaProvider());
        // providers.put(ProviderType.GIT, new GitSchemaProvider());
        // providers.put(ProviderType.JAR, new JarSchemaProvider());
    }

    private SchemaProviderFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Create provider by type.
     *
     * @param type Provider type
     * @return Provider instance
     * @throws IllegalArgumentException if type not supported
     */
    public static SchemaProvider createProvider(ProviderType type) {
        if (type == null) {
            throw new IllegalArgumentException("Provider type must not be null");
        }

        SchemaProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "Provider type not supported: "
                            + type
                            + ". Register a provider first using registerProvider().");
        }

        return provider;
    }

    /**
     * Register custom provider.
     *
     * @param type Provider type
     * @param provider Provider implementation
     */
    public static void registerProvider(ProviderType type, SchemaProvider provider) {
        if (type == null) {
            throw new IllegalArgumentException("Provider type must not be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }

        providers.put(type, provider);
    }

    /**
     * Check if a provider type is registered.
     *
     * @param type Provider type
     * @return true if registered
     */
    public static boolean isProviderRegistered(ProviderType type) {
        return providers.containsKey(type);
    }
}
