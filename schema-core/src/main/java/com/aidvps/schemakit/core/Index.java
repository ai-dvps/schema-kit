package com.aidvps.schemakit.core;

import java.util.*;

/** Represents a database index. Immutable object with builder pattern for construction. */
public final class Index {
    private final String name;
    private final List<String> columns;
    private final IndexType type;
    private final boolean unique;

    private Index(Builder builder) {
        this.name = validateName(builder.name);
        this.columns =
                builder.columns != null
                        ? Collections.unmodifiableList(new ArrayList<>(builder.columns))
                        : Collections.emptyList();
        this.type = builder.type != null ? builder.type : IndexType.BTREE;
        this.unique = builder.unique;
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name cannot be null or empty");
        }
        return name;
    }

    /**
     * Get the index name.
     *
     * @return Index name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the indexed columns (in order).
     *
     * @return List of column names
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Get the index type.
     *
     * @return Index type
     */
    public IndexType getType() {
        return type;
    }

    /**
     * Check if this is a unique index.
     *
     * @return true if unique
     */
    public boolean isUnique() {
        return unique;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return unique == index.unique
                && Objects.equals(name, index.name)
                && Objects.equals(columns, index.columns)
                && type == index.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, type, unique);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Index '").append(name).append("'");
        if (!columns.isEmpty()) {
            sb.append(" (").append(String.join(", ", columns)).append(")");
        }
        if (unique) {
            sb.append(" UNIQUE");
        }
        if (type != IndexType.BTREE) {
            sb.append(" [").append(type).append("]");
        }
        return sb.toString();
    }

    /** Builder for Index class. */
    public static class Builder {
        private String name;
        private List<String> columns;
        private IndexType type;
        private boolean unique = false;

        /**
         * Set the index name.
         *
         * @param name Index name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add a column to the index.
         *
         * @param column Column name
         * @return this builder
         */
        public Builder column(String column) {
            if (columns == null) {
                columns = new ArrayList<>();
            }
            columns.add(column);
            return this;
        }

        /**
         * Set the index type.
         *
         * @param type Index type
         * @return this builder
         */
        public Builder type(IndexType type) {
            this.type = type;
            return this;
        }

        /**
         * Set whether the index is unique.
         *
         * @param unique true if unique
         * @return this builder
         */
        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        /**
         * Build the Index instance.
         *
         * @return Index instance
         */
        public Index build() {
            if (name == null) {
                throw new IllegalStateException("Index name must be set");
            }
            if (columns == null || columns.isEmpty()) {
                throw new IllegalStateException("At least one column must be set");
            }
            return new Index(this);
        }
    }
}

/** Index type enum. */
enum IndexType {
    BTREE("BTREE"),
    HASH("HASH"),
    FULLTEXT("FULLTEXT"),
    SPATIAL("SPATIAL");

    private final String typeName;

    IndexType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
