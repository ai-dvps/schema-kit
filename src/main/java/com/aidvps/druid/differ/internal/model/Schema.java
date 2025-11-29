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

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.internal.model.constraint.Constraint;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable representation of a complete database schema.
 *
 * <p>A Schema contains all tables, columns, constraints, and indexes that define a database
 * structure. Once created, a Schema cannot be modified.
 */
public final class Schema {
    private final Map<String, Table> tables;
    private final DatabaseDialect dialect;
    private final String version;
    private final Map<String, Object> metadata;
    private final Instant createdAt;

    private Schema(Builder builder) {
        this.tables = Collections.unmodifiableMap(new java.util.HashMap<>(builder.tables));
        this.dialect = Objects.requireNonNull(builder.dialect, "Database dialect cannot be null");
        this.version = builder.version;
        this.metadata = Collections.unmodifiableMap(new java.util.HashMap<>(builder.metadata));
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    /**
     * Creates a new Builder for Schema.
     *
     * @param dialect the database dialect (required)
     * @return a new Builder instance
     */
    public static Builder builder(DatabaseDialect dialect) {
        return new Builder(dialect);
    }

    /**
     * Returns all tables in the schema.
     *
     * @return an unmodifiable map of table name to Table
     */
    public Map<String, Table> getTables() {
        return tables;
    }

    /**
     * Returns a specific table by name.
     *
     * @param name the table name
     * @return an Optional containing the table if found, or empty
     */
    public Optional<Table> getTable(String name) {
        return Optional.ofNullable(tables.get(name));
    }

    /**
     * Returns the database dialect.
     *
     * @return the dialect (never null)
     */
    public DatabaseDialect getDialect() {
        return dialect;
    }

    /**
     * Returns the schema version.
     *
     * @return an Optional containing the version, or empty if not specified
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns all metadata entries.
     *
     * @return an unmodifiable map of metadata keys to values
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Returns a specific metadata entry.
     *
     * @param key the metadata key
     * @return an Optional containing the value if found, or empty
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /**
     * Returns the timestamp when this schema was created.
     *
     * @return the creation timestamp (never null)
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns all table names in the schema.
     *
     * @return a set of table names
     */
    public Set<String> getTableNames() {
        return tables.keySet();
    }

    /**
     * Returns the number of tables in the schema.
     *
     * @return the table count
     */
    public int getTableCount() {
        return tables.size();
    }

    /**
     * Checks if a table exists in the schema.
     *
     * @param tableName the table name
     * @return true if the table exists, false otherwise
     */
    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName);
    }

    /**
     * Returns all tables in topological order based on foreign key dependencies.
     *
     * @return a list of tables ordered by dependencies
     * @throws IllegalStateException if circular dependencies are detected
     */
    public List<Table> getTablesInDependencyOrder() {
        List<Table> ordered = new java.util.ArrayList<>();
        Set<String> visited = new java.util.HashSet<>();
        Set<String> visiting = new java.util.HashSet<>();

        for (Table table : tables.values()) {
            visitTable(table, ordered, visited, visiting);
        }

        return ordered;
    }

    private void visitTable(
            Table table, List<Table> ordered, Set<String> visited, Set<String> visiting) {
        String tableName = table.getName();

        if (visited.contains(tableName)) {
            return;
        }

        if (visiting.contains(tableName)) {
            throw new IllegalStateException("Circular dependency detected for table: " + tableName);
        }

        visiting.add(tableName);

        for (Constraint constraint : table.getConstraints().values()) {
            if (constraint
                    instanceof com.aidvps.druid.differ.internal.model.constraint.ForeignKey) {
                com.aidvps.druid.differ.internal.model.constraint.ForeignKey fk =
                        (com.aidvps.druid.differ.internal.model.constraint.ForeignKey) constraint;
                Table referencedTable = tables.get(fk.getReferencedTable());
                if (referencedTable != null) {
                    visitTable(referencedTable, ordered, visited, visiting);
                }
            }
        }

        visiting.remove(tableName);
        visited.add(tableName);
        ordered.add(table);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schema schema = (Schema) o;
        return Objects.equals(tables, schema.tables)
                && dialect == schema.dialect
                && Objects.equals(version, schema.version)
                && Objects.equals(metadata, schema.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tables, dialect, version, metadata);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schema: ").append(dialect);
        if (version != null) {
            sb.append(" v").append(version);
        }
        sb.append("\nTables: ").append(tables.size());
        sb.append("\nTable List: ")
                .append(getTableNames().stream().sorted().collect(Collectors.joining(", ")));
        return sb.toString();
    }

    /** Builder for creating Schema instances. */
    public static final class Builder {
        private final java.util.Map<String, Table> tables = new java.util.HashMap<>();
        private final DatabaseDialect dialect;
        private String version;
        private final java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        private Instant createdAt;

        private Builder(DatabaseDialect dialect) {
            this.dialect = dialect;
        }

        /**
         * Adds a table to the schema.
         *
         * @param table the table to add
         * @return this Builder for chaining
         */
        public Builder addTable(Table table) {
            tables.put(table.getName(), table);
            return this;
        }

        /**
         * Sets the schema version.
         *
         * @param version the version string
         * @return this Builder for chaining
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key the metadata key
         * @param value the metadata value
         * @return this Builder for chaining
         */
        public Builder addMetadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this Builder for chaining
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds the Schema instance.
         *
         * @return a new immutable Schema
         */
        public Schema build() {
            validateForeignKeyReferences();
            return new Schema(this);
        }

        private void validateForeignKeyReferences() {
            for (Table table : tables.values()) {
                for (Constraint constraint : table.getConstraints().values()) {
                    if (constraint
                            instanceof
                            com.aidvps.druid.differ.internal.model.constraint.ForeignKey) {
                        com.aidvps.druid.differ.internal.model.constraint.ForeignKey fk =
                                (com.aidvps.druid.differ.internal.model.constraint.ForeignKey)
                                        constraint;
                        if (!tables.containsKey(fk.getReferencedTable())) {
                            throw new IllegalArgumentException(
                                    "Foreign key references non-existent table: "
                                            + fk.getReferencedTable());
                        }
                    }
                }
            }
        }
    }
}
