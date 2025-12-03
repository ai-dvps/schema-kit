/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aidvps.schemakit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating SchemaProviders with automatic discovery via SPI (Service Provider Interface).
 *
 * <p>This factory supports:
 * - Automatic discovery of providers via META-INF/services
 * - Manual registration of custom providers
 * - Provider lookup by unique identifier
 * - Validation of unique provider identifiers
 */
public final class SchemaProviderFactory {

    // Provider registry using provider ID as key
    private static final Map<String, SchemaProvider> providersById =
            new ConcurrentHashMap<>();

    // Provider metadata registry
    private static final Map<String, SchemaProviderInfo> providerInfoRegistry =
            new ConcurrentHashMap<>();

    private static boolean initialized = false;

    static {
        // Initialize via SPI discovery
        initialize();
    }

    /**
     * Initialize the factory by discovering providers via SPI.
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        List<SchemaProvider> discoveredProviders = new ArrayList<>();

        // Discover providers via ServiceLoader
        ServiceLoader<SchemaProvider> loader = ServiceLoader.load(SchemaProvider.class);
        for (SchemaProvider provider : loader) {
            discoveredProviders.add(provider);
        }

        // Auto-register discovered providers
        for (SchemaProvider provider : discoveredProviders) {
            try {
                autoRegisterProvider(provider);
            } catch (Exception e) {
                // Log warning but continue with other providers
                System.err.println(
                        "Warning: Failed to auto-register provider: "
                                + provider.getClass().getName()
                                + " - "
                                + e.getMessage());
            }
        }

        // Validate unique identifiers
        validateUniqueIdentifiers();

        initialized = true;
    }

    /**
     * Auto-register a provider discovered via SPI.
     *
     * @param provider Provider to register
     */
    private static void autoRegisterProvider(SchemaProvider provider) {
        String providerId = provider.getProviderId();

        // Register by ID
        providersById.put(providerId, provider);

        // Register metadata
        providerInfoRegistry.put(providerId, new SchemaProviderInfo(providerId, provider));
    }

    /**
     * Validate that all provider IDs are unique.
     *
     * @throws IllegalStateException if duplicate provider IDs are found
     */
    private static void validateUniqueIdentifiers() {
        List<String> duplicateIds = new ArrayList<>();
        Map<String, Integer> idCount = new HashMap<>();

        // Count occurrences of each provider ID
        for (String id : providerInfoRegistry.keySet()) {
            idCount.put(id, idCount.getOrDefault(id, 0) + 1);
        }

        // Find duplicates
        for (Map.Entry<String, Integer> entry : idCount.entrySet()) {
            if (entry.getValue() > 1) {
                duplicateIds.add(entry.getKey());
            }
        }

        if (!duplicateIds.isEmpty()) {
            throw new IllegalStateException(
                    "Duplicate provider IDs detected: "
                            + duplicateIds
                            + ". Each provider must have a unique identifier.");
        }
    }

    private SchemaProviderFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Create provider by unique provider ID (preferred method).
     *
     * @param providerId Unique provider identifier
     * @return Provider instance
     * @throws IllegalArgumentException if provider ID is not found
     */
    public static SchemaProvider createProvider(String providerId) {
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID must not be null or empty");
        }

        SchemaProvider provider = providersById.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "Provider not found with ID: "
                            + providerId
                            + ". Available IDs: "
                            + getRegisteredProviderIds());
        }

        return provider;
    }


    /**
     * Register a custom provider.
     *
     * @param providerId Unique provider identifier
     * @param provider Provider instance
     * @throws IllegalArgumentException if provider ID or provider is invalid
     */
    public static void registerProvider(String providerId, SchemaProvider provider) {
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID must not be null or empty");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }

        // Check for duplicate ID
        if (providersById.containsKey(providerId)) {
            throw new IllegalArgumentException(
                    "Provider ID already registered: " + providerId);
        }

        // Register by ID
        providersById.put(providerId, provider);

        // Register metadata
        providerInfoRegistry.put(providerId, new SchemaProviderInfo(providerId, provider));

        // Re-validate unique identifiers
        try {
            validateUniqueIdentifiers();
        } catch (IllegalStateException e) {
            // Rollback registration if validation fails
            providersById.remove(providerId);
            providerInfoRegistry.remove(providerId);
            throw e;
        }
    }

    /**
     * Check if a provider ID is registered.
     *
     * @param providerId Provider ID
     * @return true if registered
     */
    public static boolean isProviderRegistered(String providerId) {
        return providerId != null && providersById.containsKey(providerId);
    }

    /**
     * Get all registered provider information.
     *
     * @return Collection of provider info
     */
    public static Collection<SchemaProviderInfo> getRegisteredProviders() {
        return Collections.unmodifiableCollection(providerInfoRegistry.values());
    }

    /**
     * Get all registered provider IDs.
     *
     * @return Set of provider IDs
     */
    public static Collection<String> getRegisteredProviderIds() {
        return Collections.unmodifiableSet(providersById.keySet());
    }

    /**
     * Re-initialize the factory (useful for testing).
     *
     * @throws IllegalStateException if re-initialization fails
     */
    public static synchronized void reinitialize() {
        initialized = false;
        providersById.clear();
        providerInfoRegistry.clear();
        initialize();
    }
}
