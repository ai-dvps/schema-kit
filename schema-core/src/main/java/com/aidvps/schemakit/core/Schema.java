package com.aidvps.schemakit.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Root entity representing a complete database schema. Immutable object with builder pattern for
 * construction.
 */
public final class Schema {
    private final Map<String, Database> databases;
    private final DatabasePlatform platform;

    private Schema(Builder builder) {
        this.databases =
                builder.databases != null
                        ? Collections.unmodifiableMap(builder.databases)
                        : Collections.emptyMap();
        this.platform = builder.platform;
    }

    /**
     * Get a database by name.
     *
     * @param name Database name
     * @return Optional containing the database if found
     */
    public Optional<Database> getDatabase(String name) {
        return Optional.ofNullable(databases.get(name));
    }

    /**
     * Get all databases in this schema.
     *
     * @return Collection of all databases
     */
    public Collection<Database> getDatabases() {
        return databases.values();
    }

    /**
     * Check if this schema contains a database with the given name.
     *
     * @param name Database name
     * @return true if database exists
     */
    public boolean hasDatabase(String name) {
        return databases.containsKey(name);
    }

    /**
     * Get the database platform.
     *
     * @return Database platform
     */
    public DatabasePlatform getPlatform() {
        return platform;
    }

    /**
     * Get all tables across all databases.
     *
     * @return List of all tables
     */
    public List<Table> getAllTables() {
        return databases.values().stream()
                .flatMap(db -> db.getTables().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get a table by database and table name.
     *
     * @param databaseName Database name
     * @param tableName Table name
     * @return Optional containing the table if found
     */
    public Optional<Table> getTable(String databaseName, String tableName) {
        return getDatabase(databaseName).flatMap(db -> db.getTable(tableName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schema schema = (Schema) o;
        return Objects.equals(databases, schema.databases) && platform == schema.platform;
    }

    @Override
    public int hashCode() {
        return Objects.hash(databases, platform);
    }

    @Override
    public String toString() {
        return "Schema{" + "databases=" + databases.size() + ", platform=" + platform + '}';
    }

    /**
     * Create a new builder for Schema.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for Schema class. */
    public static class Builder {
        private Map<String, Database> databases;
        private DatabasePlatform platform;

        /**
         * Add a database to this schema.
         *
         * @param name Database name
         * @param database Database instance
         * @return this builder
         */
        public Builder database(String name, Database database) {
            if (databases == null) {
                databases = new HashMap<>();
            }
            databases.put(name, database);
            return this;
        }

        /**
         * Set the database platform.
         *
         * @param platform Database platform
         * @return this builder
         */
        public Builder platform(DatabasePlatform platform) {
            this.platform = platform;
            return this;
        }

        /**
         * Build the Schema instance.
         *
         * @return Schema instance
         * @throws IllegalStateException if platform is not set
         */
        public Schema build() {
            if (platform == null) {
                throw new IllegalStateException("Platform must be set");
            }
            return new Schema(this);
        }
    }
}
