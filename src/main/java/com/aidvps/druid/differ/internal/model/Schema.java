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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     * Computes a deterministic hash of the schema structure.
     *
     * <p>This hash is based on the complete schema structure including all tables, columns,
     * constraints, and indexes. It can be used for quick drift detection and comparison.
     *
     * <p>The hash is computed using SHA-256 and is guaranteed to be deterministic - the same schema
     * will always produce the same hash.
     *
     * @return a hexadecimal string representation of the schema hash
     * @throws RuntimeException if the hash algorithm is not available (should never happen with
     *     SHA-256)
     */
    public String computeHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateHashWithSchema(digest);
            byte[] hash = digest.digest();
            return convertToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Computes a hash for this schema and compares it with another schema's hash.
     *
     * @param other the other schema to compare
     * @return true if the schemas have the same hash (are structurally identical)
     */
    public boolean hasSameHash(Schema other) {
        if (other == null) {
            return false;
        }
        return this.computeHash().equals(other.computeHash());
    }

    /**
     * Updates the hash digest with the schema structure.
     *
     * @param digest the message digest to update
     */
    private void updateHashWithSchema(MessageDigest digest) {
        // Add dialect
        digest.update(dialect.name().getBytes(StandardCharsets.UTF_8));

        // Add version if present
        if (version != null) {
            digest.update("version:".getBytes(StandardCharsets.UTF_8));
            digest.update(version.getBytes(StandardCharsets.UTF_8));
        }

        // Add tables in sorted order for deterministic hash
        List<String> sortedTableNames =
                tables.keySet().stream().sorted().collect(Collectors.toList());

        for (String tableName : sortedTableNames) {
            Table table = tables.get(tableName);
            updateHashWithTable(digest, table);
        }
    }

    /**
     * Updates the hash digest with a table structure.
     *
     * @param digest the message digest to update
     * @param table the table to hash
     */
    private void updateHashWithTable(MessageDigest digest, Table table) {
        // Add table name
        digest.update("table:".getBytes(StandardCharsets.UTF_8));
        digest.update(table.getName().getBytes(StandardCharsets.UTF_8));

        // Add comment if present
        String comment = table.getComment().orElse(null);
        if (comment != null) {
            digest.update("comment:".getBytes(StandardCharsets.UTF_8));
            digest.update(comment.getBytes(StandardCharsets.UTF_8));
        }

        // Add options in sorted order
        List<String> sortedOptionKeys =
                table.getOptions().keySet().stream().sorted().collect(Collectors.toList());
        for (String key : sortedOptionKeys) {
            digest.update("option:".getBytes(StandardCharsets.UTF_8));
            digest.update(key.getBytes(StandardCharsets.UTF_8));
            digest.update("=".getBytes(StandardCharsets.UTF_8));
            digest.update(table.getOptions().get(key).getBytes(StandardCharsets.UTF_8));
        }

        // Add columns in definition order
        for (Column column : table.getColumns()) {
            updateHashWithColumn(digest, column);
        }

        // Add constraints in sorted order
        List<String> sortedConstraintNames =
                table.getConstraints().keySet().stream().sorted().collect(Collectors.toList());
        for (String constraintName : sortedConstraintNames) {
            Constraint constraint = table.getConstraints().get(constraintName);
            updateHashWithConstraint(digest, constraint);
        }

        // Add indexes in definition order
        for (Index index : table.getIndexes()) {
            updateHashWithIndex(digest, index);
        }
    }

    /**
     * Updates the hash digest with a column structure.
     *
     * @param digest the message digest to update
     * @param column the column to hash
     */
    private void updateHashWithColumn(MessageDigest digest, Column column) {
        digest.update("column:".getBytes(StandardCharsets.UTF_8));
        digest.update(column.getName().getBytes(StandardCharsets.UTF_8));
        digest.update(":".getBytes(StandardCharsets.UTF_8));
        digest.update(column.getDataType().getBytes(StandardCharsets.UTF_8));

        if (column.getLength().isPresent()) {
            digest.update("(".getBytes(StandardCharsets.UTF_8));
            digest.update(column.getLength().get().toString().getBytes(StandardCharsets.UTF_8));
            digest.update(")".getBytes(StandardCharsets.UTF_8));
        }

        if (column.getPrecision().isPresent()) {
            digest.update("(".getBytes(StandardCharsets.UTF_8));
            digest.update(column.getPrecision().get().toString().getBytes(StandardCharsets.UTF_8));
            if (column.getScale().isPresent()) {
                digest.update(",".getBytes(StandardCharsets.UTF_8));
                digest.update(column.getScale().get().toString().getBytes(StandardCharsets.UTF_8));
            }
            digest.update(")".getBytes(StandardCharsets.UTF_8));
        }

        if (!column.isNullable()) {
            digest.update(" NOT NULL".getBytes(StandardCharsets.UTF_8));
        }

        if (column.isAutoIncrement()) {
            digest.update(" AUTO_INCREMENT".getBytes(StandardCharsets.UTF_8));
        }

        String defaultValue = column.getDefaultValue().orElse(null);
        if (defaultValue != null) {
            digest.update(" DEFAULT ".getBytes(StandardCharsets.UTF_8));
            digest.update(defaultValue.getBytes(StandardCharsets.UTF_8));
        }

        String columnComment = column.getComment().orElse(null);
        if (columnComment != null) {
            digest.update(" COMMENT ".getBytes(StandardCharsets.UTF_8));
            digest.update(columnComment.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Updates the hash digest with a constraint structure.
     *
     * @param digest the message digest to update
     * @param constraint the constraint to hash
     */
    private void updateHashWithConstraint(MessageDigest digest, Constraint constraint) {
        digest.update("constraint:".getBytes(StandardCharsets.UTF_8));
        digest.update(constraint.getType().name().getBytes(StandardCharsets.UTF_8));

        // Add constraint-specific details based on type
        switch (constraint.getType()) {
            case PRIMARY_KEY:
                com.aidvps.druid.differ.internal.model.constraint.PrimaryKey pk =
                        (com.aidvps.druid.differ.internal.model.constraint.PrimaryKey) constraint;
                digest.update("(".getBytes(StandardCharsets.UTF_8));
                digest.update(String.join(",", pk.getColumns()).getBytes(StandardCharsets.UTF_8));
                digest.update(")".getBytes(StandardCharsets.UTF_8));
                break;

            case UNIQUE:
                com.aidvps.druid.differ.internal.model.constraint.UniqueConstraint uc =
                        (com.aidvps.druid.differ.internal.model.constraint.UniqueConstraint)
                                constraint;
                digest.update("(".getBytes(StandardCharsets.UTF_8));
                digest.update(String.join(",", uc.getColumns()).getBytes(StandardCharsets.UTF_8));
                digest.update(")".getBytes(StandardCharsets.UTF_8));
                break;

            case FOREIGN_KEY:
                com.aidvps.druid.differ.internal.model.constraint.ForeignKey fk =
                        (com.aidvps.druid.differ.internal.model.constraint.ForeignKey) constraint;
                digest.update("(".getBytes(StandardCharsets.UTF_8));
                digest.update(String.join(",", fk.getColumns()).getBytes(StandardCharsets.UTF_8));
                digest.update(") REFERENCES ".getBytes(StandardCharsets.UTF_8));
                digest.update(fk.getReferencedTable().getBytes(StandardCharsets.UTF_8));
                digest.update("(".getBytes(StandardCharsets.UTF_8));
                digest.update(
                        String.join(",", fk.getReferencedColumns())
                                .getBytes(StandardCharsets.UTF_8));
                digest.update(")".getBytes(StandardCharsets.UTF_8));
                break;

            case CHECK:
                com.aidvps.druid.differ.internal.model.constraint.CheckConstraint cc =
                        (com.aidvps.druid.differ.internal.model.constraint.CheckConstraint)
                                constraint;
                digest.update("(".getBytes(StandardCharsets.UTF_8));
                digest.update(cc.getExpression().getBytes(StandardCharsets.UTF_8));
                digest.update(")".getBytes(StandardCharsets.UTF_8));
                break;
        }
    }

    /**
     * Updates the hash digest with an index structure.
     *
     * @param digest the message digest to update
     * @param index the index to hash
     */
    private void updateHashWithIndex(MessageDigest digest, Index index) {
        digest.update("index:".getBytes(StandardCharsets.UTF_8));

        if (index.getName().isPresent()) {
            digest.update(index.getName().get().getBytes(StandardCharsets.UTF_8));
        }

        digest.update("(".getBytes(StandardCharsets.UTF_8));
        digest.update(String.join(",", index.getColumns()).getBytes(StandardCharsets.UTF_8));
        digest.update(")".getBytes(StandardCharsets.UTF_8));

        if (index.isUnique()) {
            digest.update(" UNIQUE".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array
     * @return the hexadecimal string
     */
    private String convertToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
        // Only compare dialect and table names to avoid infinite recursion
        // Table contents are compared through TableDiff, not direct Schema equality
        return dialect == schema.dialect
                && Objects.equals(version, schema.version)
                && tables.keySet().equals(schema.tables.keySet());
    }

    @Override
    public int hashCode() {
        // Only use dialect and table names for hashing
        return Objects.hash(dialect, version, tables.keySet());
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
            // Note: Foreign key validation is not performed during parsing to allow
            // forward references (tables can be defined in any order)
            return new Schema(this);
        }

        /**
         * Validates foreign key references.
         *
         * @throws IllegalArgumentException if a foreign key references a non-existent table
         */
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
