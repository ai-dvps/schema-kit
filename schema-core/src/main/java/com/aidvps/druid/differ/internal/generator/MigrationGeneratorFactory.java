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

package com.aidvps.druid.differ.internal.generator;

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.exception.GenerationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating database-specific migration generators.
 *
 * <p>This class implements the Factory pattern to provide instances of the appropriate
 * MigrationGenerator implementation based on the database dialect.
 *
 * <p>Supported dialects:
 *
 * <ul>
 *   <li>MySQL - uses MySQLMigrationGenerator
 *   <li>PostgreSQL - uses PostgreSQLMigrationGenerator
 *   <li>Oracle - uses OracleMigrationGenerator
 * </ul>
 *
 * <p>The factory maintains a registry of available generators and handles error cases for
 * unsupported dialects.
 */
public class MigrationGeneratorFactory {

    private final Map<DatabaseDialect, MigrationGeneratorFactory.Registration> registry;

    /** Creates a new MigrationGeneratorFactory with the default set of generators registered. */
    public MigrationGeneratorFactory() {
        this.registry = new ConcurrentHashMap<>();
        registerDefaults();
    }

    /**
     * Creates a migration generator for the specified dialect.
     *
     * @param dialect the database dialect
     * @param includeComments whether to include comments in generated SQL
     * @return a MigrationGenerator instance for the specified dialect
     * @throws GenerationException if the dialect is not supported
     */
    public MigrationGenerator createGenerator(DatabaseDialect dialect, boolean includeComments)
            throws GenerationException {
        Registration registration = registry.get(dialect);

        if (registration == null) {
            throw new GenerationException(
                    "Unsupported database dialect: "
                            + dialect
                            + ". Supported dialects: "
                            + getSupportedDialects());
        }

        return registration.factory.apply(includeComments);
    }

    /**
     * Gets a list of supported database dialects.
     *
     * @return an unmodifiable list of supported dialects
     */
    public String getSupportedDialects() {
        return String.join(
                ", ", registry.keySet().stream().map(Enum::name).sorted().toArray(String[]::new));
    }

    /**
     * Checks if a dialect is supported.
     *
     * @param dialect the database dialect to check
     * @return true if supported, false otherwise
     */
    public boolean isDialectSupported(DatabaseDialect dialect) {
        return registry.containsKey(dialect);
    }

    /** Registers the default set of generators. */
    private void registerDefaults() {
        // Register MySQL generator
        register(
                DatabaseDialect.MYSQL,
                includeComments -> new MySQLMigrationGenerator(includeComments));

        // Register PostgreSQL generator
        register(
                DatabaseDialect.POSTGRESQL,
                includeComments -> new PostgreSQLMigrationGenerator(includeComments));

        // Register Oracle generator
        register(
                DatabaseDialect.ORACLE,
                includeComments -> new OracleMigrationGenerator(includeComments));
    }

    /**
     * Registers a generator factory for a specific dialect.
     *
     * @param dialect the database dialect
     * @param factory the generator factory function
     */
    public void register(
            DatabaseDialect dialect,
            java.util.function.Function<Boolean, MigrationGenerator> factory) {
        registry.put(dialect, new Registration(dialect, factory));
    }

    /**
     * Unregisters a generator for a specific dialect.
     *
     * @param dialect the database dialect to unregister
     */
    public void unregister(DatabaseDialect dialect) {
        registry.remove(dialect);
    }

    /** Clears all registered generators. */
    public void clear() {
        registry.clear();
    }

    /**
     * Gets the number of registered generators.
     *
     * @return the number of registered generators
     */
    public int getRegistrationCount() {
        return registry.size();
    }

    /** Internal class to hold generator registration information. */
    private static class Registration {
        private final DatabaseDialect dialect;
        private final java.util.function.Function<Boolean, MigrationGenerator> factory;

        private Registration(
                DatabaseDialect dialect,
                java.util.function.Function<Boolean, MigrationGenerator> factory) {
            this.dialect = dialect;
            this.factory = factory;
        }
    }
}
