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

package com.aidvps.druid.differ.internal.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of a database index.
 *
 * <p>An Index provides a fast lookup mechanism for data in a table, typically implemented using
 * B-tree or hash structures.
 */
public final class Index {
    private final String name;
    private final List<String> columns;
    private final boolean unique;
    private final String type;
    private final Map<String, String> options;

    /**
     * Creates a new Index.
     *
     * @param name the index name (may be null for auto-generated names)
     * @param columns the columns in the index (must not be empty)
     * @param unique whether the index enforces uniqueness
     * @param type the index type (e.g., "BTREE", "HASH")
     * @param options additional index options
     */
    public Index(
            String name,
            List<String> columns,
            boolean unique,
            String type,
            Map<String, String> options) {
        Objects.requireNonNull(columns, "Index columns cannot be null");
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Index must have at least one column");
        }
        this.name = name;
        this.columns = Collections.unmodifiableList(columns);
        this.unique = unique;
        this.type = type;
        this.options =
                options != null ? Collections.unmodifiableMap(options) : Collections.emptyMap();
    }

    /**
     * Returns the index name.
     *
     * @return an Optional containing the name, or empty if auto-generated
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the columns in the index.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Returns whether the index enforces uniqueness.
     *
     * @return true if unique, false otherwise
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Returns the index type.
     *
     * @return an Optional containing the type, or empty if not specified
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns all index options.
     *
     * @return an unmodifiable map of option keys to values
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Returns a specific index option.
     *
     * @param key the option key
     * @return an Optional containing the value if found, or empty
     */
    public Optional<String> getOption(String key) {
        return Optional.ofNullable(options.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return unique == index.unique
                && Objects.equals(name, index.name)
                && columns.equals(index.columns)
                && Objects.equals(type, index.type)
                && Objects.equals(options, index.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, unique, type, options);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name).append(' ');
        }
        if (unique) {
            sb.append("UNIQUE ");
        }
        if (type != null) {
            sb.append(type).append(' ');
        }
        sb.append("INDEX (");
        sb.append(String.join(", ", columns));
        sb.append(')');
        return sb.toString();
    }

    /** Builder for creating Index instances. */
    public static class Builder {
        private String name;
        private final java.util.List<String> columns = new java.util.ArrayList<>();
        private boolean unique = false;
        private String type;
        private final java.util.Map<String, String> options = new java.util.HashMap<>();

        /**
         * Sets the index name.
         *
         * @param name the index name
         * @return this Builder for chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds a column to the index.
         *
         * @param column the column name
         * @return this Builder for chaining
         */
        public Builder addColumn(String column) {
            columns.add(column);
            return this;
        }

        /**
         * Sets whether the index is unique.
         *
         * @param unique true for unique index
         * @return this Builder for chaining
         */
        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        /**
         * Sets the index type.
         *
         * @param type the index type (e.g., "BTREE", "HASH")
         * @return this Builder for chaining
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Adds an index option.
         *
         * @param key the option key
         * @param value the option value
         * @return this Builder for chaining
         */
        public Builder addOption(String key, String value) {
            options.put(key, value);
            return this;
        }

        /**
         * Builds the Index.
         *
         * @return a new Index
         */
        public Index build() {
            return new Index(name, columns, unique, type, options);
        }
    }
}
