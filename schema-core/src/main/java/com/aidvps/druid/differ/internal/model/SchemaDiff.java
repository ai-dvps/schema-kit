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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents differences between two database schemas.
 *
 * <p>A SchemaDiff captures all changes needed to transform a source schema into a target schema,
 * including added tables, removed tables, and modified tables with their specific changes.
 */
public final class SchemaDiff {

    private final Schema sourceSchema;
    private final Schema targetSchema;
    private final Map<String, Table> addedTables;
    private final Map<String, Table> removedTables;
    private final Map<String, TableDiff> modifiedTables;

    /**
     * Creates a new SchemaDiff.
     *
     * @param sourceSchema the source schema (never null)
     * @param targetSchema the target schema (never null)
     * @param addedTables map of table name to Table that exists only in target
     * @param removedTables map of table name to Table that exists only in source
     * @param modifiedTables map of table name to TableDiff with changes
     */
    public SchemaDiff(
            Schema sourceSchema,
            Schema targetSchema,
            Map<String, Table> addedTables,
            Map<String, Table> removedTables,
            Map<String, TableDiff> modifiedTables) {
        this.sourceSchema = Objects.requireNonNull(sourceSchema, "Source schema cannot be null");
        this.targetSchema = Objects.requireNonNull(targetSchema, "Target schema cannot be null");
        this.addedTables = Collections.unmodifiableMap(new java.util.HashMap<>(addedTables));
        this.removedTables = Collections.unmodifiableMap(new java.util.HashMap<>(removedTables));
        this.modifiedTables = Collections.unmodifiableMap(new java.util.HashMap<>(modifiedTables));
    }

    /**
     * Returns the source schema.
     *
     * @return the source schema (never null)
     */
    public Schema getSourceSchema() {
        return sourceSchema;
    }

    /**
     * Returns the target schema.
     *
     * @return the target schema (never null)
     */
    public Schema getTargetSchema() {
        return targetSchema;
    }

    /**
     * Returns tables that exist only in the target schema.
     *
     * @return an unmodifiable map of table name to Table
     */
    public Map<String, Table> getAddedTables() {
        return addedTables;
    }

    /**
     * Returns a specific added table.
     *
     * @param tableName the table name
     * @return an Optional containing the table if it's an added table, or empty
     */
    public Optional<Table> getAddedTable(String tableName) {
        return Optional.ofNullable(addedTables.get(tableName));
    }

    /**
     * Returns tables that exist only in the source schema.
     *
     * @return an unmodifiable map of table name to Table
     */
    public Map<String, Table> getRemovedTables() {
        return removedTables;
    }

    /**
     * Returns a specific removed table.
     *
     * @param tableName the table name
     * @return an Optional containing the table if it's a removed table, or empty
     */
    public Optional<Table> getRemovedTable(String tableName) {
        return Optional.ofNullable(removedTables.get(tableName));
    }

    /**
     * Returns tables that exist in both schemas but have different structures.
     *
     * @return an unmodifiable map of table name to TableDiff
     */
    public Map<String, TableDiff> getModifiedTables() {
        return modifiedTables;
    }

    /**
     * Returns changes for a specific modified table.
     *
     * @param tableName the table name
     * @return an Optional containing the TableDiff if the table is modified, or empty
     */
    public Optional<TableDiff> getModifiedTable(String tableName) {
        return Optional.ofNullable(modifiedTables.get(tableName));
    }

    /**
     * Returns whether the schemas are identical.
     *
     * @return true if there are no differences, false otherwise
     */
    public boolean isEmpty() {
        return addedTables.isEmpty() && removedTables.isEmpty() && modifiedTables.isEmpty();
    }

    /**
     * Returns the total number of tables with changes.
     *
     * @return the count of changed tables
     */
    public int getChangeCount() {
        return addedTables.size() + removedTables.size() + modifiedTables.size();
    }

    /**
     * Returns a summary of all changes.
     *
     * @return a human-readable summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Schema Diff Summary:\n");

        if (!addedTables.isEmpty()) {
            sb.append("  Added Tables (").append(addedTables.size()).append("):\n");
            addedTables.keySet().stream()
                    .sorted()
                    .forEach(table -> sb.append("    + ").append(table).append("\n"));
        }

        if (!removedTables.isEmpty()) {
            sb.append("  Removed Tables (").append(removedTables.size()).append("):\n");
            removedTables.keySet().stream()
                    .sorted()
                    .forEach(table -> sb.append("    - ").append(table).append("\n"));
        }

        if (!modifiedTables.isEmpty()) {
            sb.append("  Modified Tables (").append(modifiedTables.size()).append("):\n");
            modifiedTables.keySet().stream()
                    .sorted()
                    .forEach(table -> sb.append("    ~ ").append(table).append("\n"));
        }

        if (isEmpty()) {
            sb.append("  No differences found");
        }

        return sb.toString();
    }

    /**
     * Creates an empty SchemaDiff indicating no changes.
     *
     * @param schema the schema that is the same in both source and target
     * @return an empty SchemaDiff
     */
    public static SchemaDiff empty(Schema schema) {
        return new SchemaDiff(
                schema,
                schema,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaDiff that = (SchemaDiff) o;
        return sourceSchema.equals(that.sourceSchema)
                && targetSchema.equals(that.targetSchema)
                && addedTables.equals(that.addedTables)
                && removedTables.equals(that.removedTables)
                && modifiedTables.equals(that.modifiedTables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceSchema, targetSchema, addedTables, removedTables, modifiedTables);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /** Builder for creating SchemaDiff instances. */
    public static class Builder {
        private final Schema sourceSchema;
        private final Schema targetSchema;
        private final java.util.Map<String, Table> addedTables = new java.util.HashMap<>();
        private final java.util.Map<String, Table> removedTables = new java.util.HashMap<>();
        private final java.util.Map<String, TableDiff> modifiedTables = new java.util.HashMap<>();

        /**
         * Creates a new Builder.
         *
         * @param sourceSchema the source schema
         * @param targetSchema the target schema
         */
        public Builder(Schema sourceSchema, Schema targetSchema) {
            this.sourceSchema = sourceSchema;
            this.targetSchema = targetSchema;
        }

        /**
         * Adds a table that exists only in the target schema.
         *
         * @param tableName the table name
         * @param table the table definition
         * @return this Builder for chaining
         */
        public Builder addAddedTable(String tableName, Table table) {
            addedTables.put(tableName, table);
            return this;
        }

        /**
         * Adds a table that exists only in the source schema.
         *
         * @param tableName the table name
         * @param table the table definition
         * @return this Builder for chaining
         */
        public Builder addRemovedTable(String tableName, Table table) {
            removedTables.put(tableName, table);
            return this;
        }

        /**
         * Adds a table that exists in both schemas but has different structures.
         *
         * @param tableName the table name
         * @param tableDiff the table differences
         * @return this Builder for chaining
         */
        public Builder addModifiedTable(String tableName, TableDiff tableDiff) {
            modifiedTables.put(tableName, tableDiff);
            return this;
        }

        /**
         * Builds the SchemaDiff.
         *
         * @return a new SchemaDiff
         */
        public SchemaDiff build() {
            return new SchemaDiff(
                    sourceSchema, targetSchema, addedTables, removedTables, modifiedTables);
        }
    }
}
