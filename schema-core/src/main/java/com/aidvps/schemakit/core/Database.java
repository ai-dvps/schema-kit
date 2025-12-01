package com.aidvps.schemakit.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a single database (CREATE DATABASE statement). Immutable object with builder pattern
 * for construction.
 */
public final class Database {
    private final String name;
    private final Map<String, Table> tables;
    private final Map<String, String> properties;

    private Database(Builder builder) {
        this.name = validateName(builder.name);
        this.tables =
                builder.tables != null
                        ? Collections.unmodifiableMap(builder.tables)
                        : Collections.emptyMap();
        this.properties =
                builder.properties != null
                        ? Collections.unmodifiableMap(builder.properties)
                        : Collections.emptyMap();
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }
        return name;
    }

    /**
     * Get the database name.
     *
     * @return Database name
     */
    public String getName() {
        return name;
    }

    /**
     * Get a table by name.
     *
     * @param name Table name
     * @return Optional containing the table if found
     */
    public Optional<Table> getTable(String name) {
        return Optional.ofNullable(tables.get(name));
    }

    /**
     * Get all tables in this database.
     *
     * @return Collection of all tables
     */
    public Collection<Table> getTables() {
        return tables.values();
    }

    /**
     * Check if this database contains a table with the given name.
     *
     * @param name Table name
     * @return true if table exists
     */
    public boolean hasTable(String name) {
        return tables.containsKey(name);
    }

    /**
     * Get a database property.
     *
     * @param key Property key
     * @return Optional containing the property value if found
     */
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Get all properties.
     *
     * @return Map of all properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Get all columns across all tables.
     *
     * @return List of all columns
     */
    public List<Column> getAllColumns() {
        return tables.values().stream()
                .flatMap(table -> table.getColumns().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get all indexes across all tables.
     *
     * @return List of all indexes
     */
    public List<Index> getAllIndexes() {
        List<Index> allIndexes = new ArrayList<>();
        for (Table table : tables.values()) {
            allIndexes.addAll(table.getIndexes().values());
        }
        return allIndexes;
    }

    /**
     * Get all constraints across all tables.
     *
     * @return List of all constraints
     */
    public List<Constraint> getAllConstraints() {
        List<Constraint> allConstraints = new ArrayList<>();
        for (Table table : tables.values()) {
            allConstraints.addAll(table.getConstraints().values());
        }
        return allConstraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Database database = (Database) o;
        return Objects.equals(name, database.name)
                && Objects.equals(tables, database.tables)
                && Objects.equals(properties, database.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tables, properties);
    }

    @Override
    public String toString() {
        return "Database{" + "name='" + name + '\'' + ", tables=" + tables.size() + '}';
    }

    /**
     * Create a new builder for Database.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for Database class. */
    public static class Builder {
        private String name;
        private Map<String, Table> tables;
        private Map<String, String> properties;

        /**
         * Set the database name.
         *
         * @param name Database name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add a table to this database.
         *
         * @param name Table name
         * @param table Table instance
         * @return this builder
         */
        public Builder table(String name, Table table) {
            if (tables == null) {
                tables = new HashMap<>();
            }
            if (tables.containsKey(name)) {
                throw new IllegalArgumentException("Table with name '" + name + "' already exists");
            }
            tables.put(name, table);
            return this;
        }

        /**
         * Add a property to this database.
         *
         * @param key Property key
         * @param value Property value
         * @return this builder
         */
        public Builder property(String key, String value) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(key, value);
            return this;
        }

        /**
         * Build the Database instance.
         *
         * @return Database instance
         */
        public Database build() {
            return new Database(this);
        }
    }
}
