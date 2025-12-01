package com.aidvps.schemakit.core;

import java.util.*;

/**
 * Table-level properties (engine, charset, etc.). Immutable object with builder pattern for
 * construction.
 */
public final class TableProperties {
    private final Map<String, String> properties;

    private TableProperties(Builder builder) {
        this.properties =
                builder.properties != null
                        ? Collections.unmodifiableMap(builder.properties)
                        : Collections.emptyMap();
    }

    /**
     * Get a property value.
     *
     * @param key Property key
     * @return Optional containing the property value if found
     */
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Get the engine property (MySQL).
     *
     * @return Optional engine name if found
     */
    public Optional<String> getEngine() {
        return getProperty("engine");
    }

    /**
     * Get the charset property.
     *
     * @return Optional charset if found
     */
    public Optional<String> getCharset() {
        return getProperty("charset");
    }

    /**
     * Get the collation property.
     *
     * @return Optional collation if found
     */
    public Optional<String> getCollation() {
        return getProperty("collation");
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
     * Check if a property is set.
     *
     * @param key Property key
     * @return true if property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableProperties that = (TableProperties) o;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    @Override
    public String toString() {
        if (properties.isEmpty()) {
            return "TableProperties{}";
        }
        return "TableProperties{" + properties + '}';
    }

    /** Builder for TableProperties class. */
    public static class Builder {
        private Map<String, String> properties;

        /**
         * Add a property.
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
         * Set the engine.
         *
         * @param engine Engine name (e.g., InnoDB, MyISAM)
         * @return this builder
         */
        public Builder engine(String engine) {
            return property("engine", engine);
        }

        /**
         * Set the charset.
         *
         * @param charset Charset (e.g., utf8, utf8mb4)
         * @return this builder
         */
        public Builder charset(String charset) {
            return property("charset", charset);
        }

        /**
         * Set the collation.
         *
         * @param collation Collation (e.g., utf8_general_ci)
         * @return this builder
         */
        public Builder collation(String collation) {
            return property("collation", collation);
        }

        /**
         * Build the TableProperties instance.
         *
         * @return TableProperties instance
         */
        public TableProperties build() {
            return new TableProperties(this);
        }
    }
}
