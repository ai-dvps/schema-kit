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

import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable representation of a database table.
 *
 * <p>A Table contains columns, constraints, and indexes that define its structure and
 * relationships. Once created, a Table cannot be modified.
 */
public final class Table {
    private final String name;
    private final List<Column> columns;
    private final Map<String, Constraint> constraints;
    private final List<Index> indexes;
    private final String comment;
    private final Map<String, String> options;

    private Table(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Table name cannot be null");
        this.columns = Collections.unmodifiableList(new java.util.ArrayList<>(builder.columns));
        this.constraints =
                Collections.unmodifiableMap(new java.util.HashMap<>(builder.constraints));
        this.indexes = Collections.unmodifiableList(new java.util.ArrayList<>(builder.indexes));
        this.comment = builder.comment;
        this.options = Collections.unmodifiableMap(new java.util.HashMap<>(builder.options));

        validate();
    }

    /**
     * Creates a new Builder for Table.
     *
     * @param name the table name (required)
     * @return a new Builder instance
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Returns the table name.
     *
     * @return the table name (never null)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns all columns in the table.
     *
     * @return an unmodifiable list of columns
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Returns a specific column by name.
     *
     * @param name the column name
     * @return an Optional containing the column if found, or empty
     */
    public Optional<Column> getColumn(String name) {
        return columns.stream().filter(c -> c.getName().equals(name)).findFirst();
    }

    /**
     * Returns all constraints in the table.
     *
     * @return an unmodifiable map of constraint name to Constraint
     */
    public Map<String, Constraint> getConstraints() {
        return constraints;
    }

    /**
     * Returns a specific constraint by name.
     *
     * @param name the constraint name
     * @return an Optional containing the constraint if found, or empty
     */
    public Optional<Constraint> getConstraint(String name) {
        return Optional.ofNullable(constraints.get(name));
    }

    /**
     * Returns all indexes in the table.
     *
     * @return an unmodifiable list of indexes
     */
    public List<Index> getIndexes() {
        return indexes;
    }

    /**
     * Returns a specific index by name.
     *
     * @param name the index name
     * @return an Optional containing the index if found, or empty
     */
    public Optional<Index> getIndex(String name) {
        return indexes.stream().filter(i -> i.getName().equals(name)).findFirst();
    }

    /**
     * Returns the table comment.
     *
     * @return an Optional containing the comment, or empty if not specified
     */
    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    /**
     * Returns all table options.
     *
     * @return an unmodifiable map of option names to values
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Returns a specific table option.
     *
     * @param key the option key
     * @return an Optional containing the option value if found, or empty
     */
    public Optional<String> getOption(String key) {
        return Optional.ofNullable(options.get(key));
    }

    /**
     * Returns column names only.
     *
     * @return a list of column names
     */
    public List<String> getColumnNames() {
        return columns.stream().map(Column::getName).collect(Collectors.toList());
    }

    private void validate() {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }

        List<String> columnNames = getColumnNames();
        if (columnNames.size() != columnNames.stream().distinct().count()) {
            throw new IllegalArgumentException("Column names must be unique within a table");
        }

        for (Constraint constraint : constraints.values()) {
            if (constraint
                    instanceof com.aidvps.druid.differ.internal.model.constraint.PrimaryKey) {
                com.aidvps.druid.differ.internal.model.constraint.PrimaryKey pk =
                        (com.aidvps.druid.differ.internal.model.constraint.PrimaryKey) constraint;
                for (String col : pk.getColumns()) {
                    if (!columnNames.contains(col)) {
                        throw new IllegalArgumentException(
                                "Primary key references non-existent column: " + col);
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        // Only compare name to avoid deep recursion with nested collections
        return Objects.equals(name, table.name);
    }

    @Override
    public int hashCode() {
        // Only use name for hashing
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Table: ").append(name);
        if (comment != null) {
            sb.append(" Comment: ").append(comment);
        }
        sb.append("\nColumns: ").append(columns.size());
        sb.append("\nConstraints: ").append(constraints.size());
        sb.append("\nIndexes: ").append(indexes.size());
        return sb.toString();
    }

    /** Builder for creating Table instances. */
    public static final class Builder {
        private final String name;
        private final java.util.List<Column> columns = new java.util.ArrayList<>();
        private final java.util.Map<String, Constraint> constraints = new java.util.HashMap<>();
        private final java.util.List<Index> indexes = new java.util.ArrayList<>();
        private String comment;
        private final java.util.Map<String, String> options = new java.util.HashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Adds a column to the table.
         *
         * @param column the column to add
         * @return this Builder for chaining
         */
        public Builder addColumn(Column column) {
            columns.add(column);
            return this;
        }

        /**
         * Adds multiple columns to the table.
         *
         * @param columns the columns to add
         * @return this Builder for chaining
         */
        public Builder addColumns(List<Column> columns) {
            this.columns.addAll(columns);
            return this;
        }

        /**
         * Adds a constraint to the table.
         *
         * @param name the constraint name (required for retrieval)
         * @param constraint the constraint to add
         * @return this Builder for chaining
         */
        public Builder addConstraint(String name, Constraint constraint) {
            constraints.put(name, constraint);
            return this;
        }

        /**
         * Adds an index to the table.
         *
         * @param index the index to add
         * @return this Builder for chaining
         */
        public Builder addIndex(Index index) {
            indexes.add(index);
            return this;
        }

        /**
         * Sets the table comment.
         *
         * @param comment the comment text
         * @return this Builder for chaining
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Adds a table option.
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
         * Builds the Table instance.
         *
         * @return a new immutable Table
         */
        public Table build() {
            return new Table(this);
        }
    }
}
