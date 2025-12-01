package com.aidvps.schemakit.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a single table (CREATE TABLE statement). Immutable object with builder pattern for
 * construction.
 */
public final class Table {
    private final String name;
    private final List<Column> columns;
    private final Map<String, Index> indexes;
    private final Map<String, Constraint> constraints;
    private final TableProperties properties;

    private Table(Builder builder) {
        this.name = validateName(builder.name);
        this.columns =
                builder.columns != null
                        ? Collections.unmodifiableList(new ArrayList<>(builder.columns))
                        : Collections.emptyList();
        this.indexes =
                builder.indexes != null
                        ? Collections.unmodifiableMap(builder.indexes)
                        : Collections.emptyMap();
        this.constraints =
                builder.constraints != null
                        ? Collections.unmodifiableMap(builder.constraints)
                        : Collections.emptyMap();
        this.properties =
                builder.properties != null
                        ? builder.properties
                        : new TableProperties.Builder().build();

        validateTable();
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        return name;
    }

    private void validateTable() {
        // Validate column uniqueness
        Set<String> columnNames = new HashSet<>();
        for (Column column : columns) {
            if (!columnNames.add(column.getName())) {
                throw new IllegalArgumentException("Duplicate column name: " + column.getName());
            }
        }

        // Validate index references
        for (Index index : indexes.values()) {
            for (String columnName : index.getColumns()) {
                if (!columnNames.contains(columnName)) {
                    throw new IllegalArgumentException(
                            "Index '"
                                    + index.getName()
                                    + "' references non-existent column: "
                                    + columnName);
                }
            }
        }

        // Validate constraint references
        for (Constraint constraint : constraints.values()) {
            if (constraint instanceof Constraint.PrimaryKey) {
                Constraint.PrimaryKey pk = (Constraint.PrimaryKey) constraint;
                for (String columnName : pk.getColumns()) {
                    if (!columnNames.contains(columnName)) {
                        throw new IllegalArgumentException(
                                "Primary key references non-existent column: " + columnName);
                    }
                }
            } else if (constraint instanceof Constraint.ForeignKey) {
                Constraint.ForeignKey fk = (Constraint.ForeignKey) constraint;
                for (String columnName : fk.getColumns()) {
                    if (!columnNames.contains(columnName)) {
                        throw new IllegalArgumentException(
                                "Foreign key references non-existent column: " + columnName);
                    }
                }
            }
        }
    }

    /**
     * Get the table name.
     *
     * @return Table name
     */
    public String getName() {
        return name;
    }

    /**
     * Get a column by name.
     *
     * @param name Column name
     * @return Optional containing the column if found
     */
    public Optional<Column> getColumn(String name) {
        return columns.stream().filter(col -> col.getName().equals(name)).findFirst();
    }

    /**
     * Get all columns in this table.
     *
     * @return List of all columns (order matters)
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Get an index by name.
     *
     * @param name Index name
     * @return Optional containing the index if found
     */
    public Optional<Index> getIndex(String name) {
        return Optional.ofNullable(indexes.get(name));
    }

    /**
     * Get all indexes in this table.
     *
     * @return Collection of all indexes
     */
    public Map<String, Index> getIndexes() {
        return indexes;
    }

    /**
     * Get a constraint by name.
     *
     * @param name Constraint name
     * @return Optional containing the constraint if found
     */
    public Optional<Constraint> getConstraint(String name) {
        return Optional.ofNullable(constraints.get(name));
    }

    /**
     * Get all constraints in this table.
     *
     * @return Collection of all constraints
     */
    public Map<String, Constraint> getConstraints() {
        return constraints;
    }

    /**
     * Get table properties.
     *
     * @return Table properties
     */
    public TableProperties getProperties() {
        return properties;
    }

    /**
     * Check if this table has a primary key.
     *
     * @return true if primary key exists
     */
    public boolean hasPrimaryKey() {
        return constraints.values().stream().anyMatch(c -> c instanceof Constraint.PrimaryKey);
    }

    /**
     * Get the primary key constraint if it exists.
     *
     * @return Optional containing the primary key if found
     */
    public Optional<Constraint.PrimaryKey> getPrimaryKey() {
        return constraints.values().stream()
                .filter(c -> c instanceof Constraint.PrimaryKey)
                .map(c -> (Constraint.PrimaryKey) c)
                .findFirst();
    }

    /**
     * Get all foreign key constraints.
     *
     * @return List of foreign key constraints
     */
    public List<Constraint.ForeignKey> getForeignKeys() {
        return constraints.values().stream()
                .filter(c -> c instanceof Constraint.ForeignKey)
                .map(c -> (Constraint.ForeignKey) c)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return Objects.equals(name, table.name)
                && Objects.equals(columns, table.columns)
                && Objects.equals(indexes, table.indexes)
                && Objects.equals(constraints, table.constraints)
                && Objects.equals(properties, table.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, indexes, constraints, properties);
    }

    @Override
    public String toString() {
        return "Table{"
                + "name='"
                + name
                + '\''
                + ", columns="
                + columns.size()
                + ", indexes="
                + indexes.size()
                + ", constraints="
                + constraints.size()
                + '}';
    }

    /**
     * Create a new builder for Table.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for Table class. */
    public static class Builder {
        private String name;
        private List<Column> columns;
        private Map<String, Index> indexes;
        private Map<String, Constraint> constraints;
        private TableProperties properties;

        /**
         * Set the table name.
         *
         * @param name Table name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add a column to this table.
         *
         * @param column Column instance
         * @return this builder
         */
        public Builder column(Column column) {
            if (columns == null) {
                columns = new ArrayList<>();
            }
            columns.add(column);
            return this;
        }

        /**
         * Add an index to this table.
         *
         * @param name Index name
         * @param index Index instance
         * @return this builder
         */
        public Builder index(String name, Index index) {
            if (indexes == null) {
                indexes = new HashMap<>();
            }
            indexes.put(name, index);
            return this;
        }

        /**
         * Add a constraint to this table.
         *
         * @param name Constraint name
         * @param constraint Constraint instance
         * @return this builder
         */
        public Builder constraint(String name, Constraint constraint) {
            if (constraints == null) {
                constraints = new HashMap<>();
            }
            constraints.put(name, constraint);
            return this;
        }

        /**
         * Set table properties.
         *
         * @param properties Table properties
         * @return this builder
         */
        public Builder properties(TableProperties properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Build the Table instance.
         *
         * @return Table instance
         */
        public Table build() {
            return new Table(this);
        }
    }
}
