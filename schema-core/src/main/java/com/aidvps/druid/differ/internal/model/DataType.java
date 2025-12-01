package com.aidvps.druid.differ.internal.model;

import java.util.*;

/**
 * Represents SQL data types with dialect awareness. Immutable object with builder pattern for
 * construction.
 */
public final class DataType {
    private final String baseType;
    private final List<String> params;
    private final DatabasePlatform dialect;

    private DataType(Builder builder) {
        this.baseType = Objects.requireNonNull(builder.baseType, "Base type cannot be null");
        this.params =
                builder.params != null
                        ? Collections.unmodifiableList(new ArrayList<>(builder.params))
                        : Collections.emptyList();
        this.dialect = builder.dialect;
    }

    /**
     * Get the base type (e.g., VARCHAR, INT).
     *
     * @return Base type
     */
    public String getBaseType() {
        return baseType;
    }

    /**
     * Get the type parameters (e.g., 255, 10,2).
     *
     * @return List of parameters
     */
    public List<String> getParams() {
        return params;
    }

    /**
     * Get the database dialect.
     *
     * @return Database platform
     */
    public DatabasePlatform getDialect() {
        return dialect;
    }

    /**
     * Convert to full type string.
     *
     * @return Full type string (e.g., "VARCHAR(255)")
     */
    public String toString() {
        if (params.isEmpty()) {
            return baseType;
        }
        return baseType + "(" + String.join(", ", params) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataType dataType = (DataType) o;
        return Objects.equals(baseType, dataType.baseType)
                && Objects.equals(params, dataType.params)
                && dialect == dataType.dialect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType, params, dialect);
    }

    /**
     * Create a new builder for DataType.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for DataType class. */
    public static class Builder {
        private String baseType;
        private List<String> params;
        private DatabasePlatform dialect;

        /**
         * Set the base type.
         *
         * @param baseType Base type (e.g., VARCHAR, INT)
         * @return this builder
         */
        public Builder baseType(String baseType) {
            this.baseType = baseType;
            return this;
        }

        /**
         * Add a parameter.
         *
         * @param param Parameter (e.g., "255", "10", "2")
         * @return this builder
         */
        public Builder param(String param) {
            if (params == null) {
                params = new ArrayList<>();
            }
            params.add(param);
            return this;
        }

        /**
         * Set the database dialect.
         *
         * @param dialect Database platform
         * @return this builder
         */
        public Builder dialect(DatabasePlatform dialect) {
            this.dialect = dialect;
            return this;
        }

        /**
         * Build the DataType instance.
         *
         * @return DataType instance
         */
        public DataType build() {
            if (baseType == null) {
                throw new IllegalStateException("Base type must be set");
            }
            return new DataType(this);
        }
    }
}
