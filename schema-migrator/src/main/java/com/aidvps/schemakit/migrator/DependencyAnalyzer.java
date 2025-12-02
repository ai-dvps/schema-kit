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

package com.aidvps.schemakit.migrator;

import java.util.*;

/** Analyzes schema diffs for dependency ordering and constraints. */
public class DependencyAnalyzer {

    /**
     * Analyze a schema diff to determine proper execution order.
     *
     * @param diff The schema diff to analyze
     * @return DependencyReport with analysis results
     */
    public DependencyReport analyze(SchemaDiff diff) {
        if (diff == null || !diff.hasChanges()) {
            return DependencyReport.builder().build();
        }

        // Collect all changes
        List<SchemaChange> changes = new ArrayList<>(diff.getChanges());

        // Identify dependencies
        Map<SchemaChange, Set<SchemaChange>> dependencies = new HashMap<>();
        Map<String, SchemaChange> tableCreationChanges = new HashMap<>();
        Map<String, SchemaChange> tableDropChanges = new HashMap<>();

        // First pass: identify table creation and drop operations
        for (SchemaChange change : changes) {
            String tableName = extractTableName(change.getEntityPath());

            switch (change.getType()) {
                case TABLE_CREATED:
                    tableCreationChanges.put(tableName, change);
                    break;
                case TABLE_DROPPED:
                    tableDropChanges.put(tableName, change);
                    break;
                default:
                    break;
            }
        }

        // Second pass: analyze dependencies for each change
        for (SchemaChange change : changes) {
            dependencies.put(change, new HashSet<>());

            switch (change.getType()) {
                case COLUMN_ADDED:
                case COLUMN_DROPPED:
                case COLUMN_MODIFIED:
                case INDEX_CREATED:
                case INDEX_DROPPED:
                case CONSTRAINT_ADDED:
                case CONSTRAINT_DROPPED:
                    // These depend on the table existing
                    String tableName = extractTableName(change.getEntityPath());
                    SchemaChange tableCreate = tableCreationChanges.get(tableName);
                    if (tableCreate != null && !tableCreate.equals(change)) {
                        dependencies.get(change).add(tableCreate);
                    }
                    // If table is being dropped, these changes should happen before the drop
                    SchemaChange tableDrop = tableDropChanges.get(tableName);
                    if (tableDrop != null && !tableDrop.equals(change)) {
                        dependencies.get(tableDrop).add(change);
                    }
                    break;

                case TABLE_DROPPED:
                    // Table drop should happen after all dependent objects are dropped
                    // Dependencies will be added by the objects being dropped
                    break;

                default:
                    break;
            }
        }

        // Create dependency graph
        DependencyGraph graph = new DependencyGraph(dependencies);

        // Generate topological sort order
        List<SchemaChange> orderedChanges = graph.topologicalSort();

        // Identify potential issues
        Set<String> warnings = new HashSet<>();
        Set<String> errors = new HashSet<>();

        // Check for circular dependencies
        if (orderedChanges.size() != changes.size()) {
            errors.add("Circular dependency detected in schema changes");
        }

        // Check for drop operations on non-existent tables
        for (SchemaChange change : changes) {
            if (change.getType() == SchemaChange.ChangeType.TABLE_DROPPED) {
                String tableName = extractTableName(change.getEntityPath());
                if (!tableCreationChanges.containsKey(tableName)) {
                    warnings.add("Dropping table that was not explicitly created: " + tableName);
                }
            }
        }

        return DependencyReport.builder()
                .originalChanges(changes)
                .orderedChanges(orderedChanges)
                .dependencies(dependencies)
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    private String extractTableName(String entityPath) {
        // Extract table name from "db.table" or "db.table.column" format
        if (entityPath == null || entityPath.isEmpty()) {
            return "";
        }
        String[] parts = entityPath.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : entityPath;
    }

    /** Represents a dependency graph for schema changes. */
    static class DependencyGraph {
        private final Map<SchemaChange, Set<SchemaChange>> dependencies;

        DependencyGraph(Map<SchemaChange, Set<SchemaChange>> dependencies) {
            this.dependencies = new HashMap<>(dependencies);
        }

        /**
         * Perform topological sort to determine execution order.
         *
         * @return List of changes in dependency order
         */
        List<SchemaChange> topologicalSort() {
            List<SchemaChange> result = new ArrayList<>();
            Set<SchemaChange> visited = new HashSet<>();
            Set<SchemaChange> visiting = new HashSet<>();

            for (SchemaChange change : dependencies.keySet()) {
                if (!visited.contains(change)) {
                    visit(change, visiting, visited, result);
                }
            }

            return result;
        }

        private void visit(
                SchemaChange change,
                Set<SchemaChange> visiting,
                Set<SchemaChange> visited,
                List<SchemaChange> result) {
            if (visiting.contains(change)) {
                // Circular dependency detected
                return;
            }

            if (visited.contains(change)) {
                return;
            }

            visiting.add(change);

            // Visit all dependencies first
            Set<SchemaChange> deps = dependencies.getOrDefault(change, new HashSet<>());
            for (SchemaChange dep : deps) {
                if (!visited.contains(dep)) {
                    visit(dep, visiting, visited, result);
                }
            }

            visiting.remove(change);
            visited.add(change);
            result.add(change);
        }
    }

    /** Report of dependency analysis results. */
    public static class DependencyReport {
        private final List<SchemaChange> originalChanges;
        private final List<SchemaChange> orderedChanges;
        private final Map<SchemaChange, Set<SchemaChange>> dependencies;
        private final Set<String> warnings;
        private final Set<String> errors;

        private DependencyReport(Builder builder) {
            this.originalChanges = builder.originalChanges;
            this.orderedChanges = builder.orderedChanges;
            this.dependencies = builder.dependencies;
            this.warnings = builder.warnings;
            this.errors = builder.errors;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the original list of changes.
         *
         * @return Original changes
         */
        public List<SchemaChange> getOriginalChanges() {
            return originalChanges;
        }

        /**
         * Get the changes in dependency order.
         *
         * @return Ordered changes
         */
        public List<SchemaChange> getOrderedChanges() {
            return orderedChanges;
        }

        /**
         * Get dependencies map.
         *
         * @return Dependencies
         */
        public Map<SchemaChange, Set<SchemaChange>> getDependencies() {
            return dependencies;
        }

        /**
         * Get warnings.
         *
         * @return Warnings
         */
        public Set<String> getWarnings() {
            return warnings;
        }

        /**
         * Get errors.
         *
         * @return Errors
         */
        public Set<String> getErrors() {
            return errors;
        }

        /**
         * Check if analysis has any issues.
         *
         * @return true if issues found
         */
        public boolean hasIssues() {
            return !errors.isEmpty() || !warnings.isEmpty();
        }

        @Override
        public String toString() {
            return "DependencyReport{"
                    + "originalChanges="
                    + originalChanges.size()
                    + ", orderedChanges="
                    + orderedChanges.size()
                    + ", warnings="
                    + warnings.size()
                    + ", errors="
                    + errors.size()
                    + '}';
        }

        /** Builder for DependencyReport. */
        public static class Builder {
            private List<SchemaChange> originalChanges = new ArrayList<>();
            private List<SchemaChange> orderedChanges = new ArrayList<>();
            private Map<SchemaChange, Set<SchemaChange>> dependencies = new HashMap<>();
            private Set<String> warnings = new HashSet<>();
            private Set<String> errors = new HashSet<>();

            private Builder() {}

            /**
             * Set original changes.
             *
             * @param changes Original changes
             * @return This builder
             */
            public Builder originalChanges(List<SchemaChange> changes) {
                this.originalChanges = changes;
                return this;
            }

            /**
             * Set ordered changes.
             *
             * @param changes Ordered changes
             * @return This builder
             */
            public Builder orderedChanges(List<SchemaChange> changes) {
                this.orderedChanges = changes;
                return this;
            }

            /**
             * Set dependencies.
             *
             * @param deps Dependencies map
             * @return This builder
             */
            public Builder dependencies(Map<SchemaChange, Set<SchemaChange>> deps) {
                this.dependencies = deps;
                return this;
            }

            /**
             * Set warnings.
             *
             * @param warnings Warnings set
             * @return This builder
             */
            public Builder warnings(Set<String> warnings) {
                this.warnings = warnings;
                return this;
            }

            /**
             * Set errors.
             *
             * @param errors Errors set
             * @return This builder
             */
            public Builder errors(Set<String> errors) {
                this.errors = errors;
                return this;
            }

            /**
             * Build the report.
             *
             * @return DependencyReport
             */
            public DependencyReport build() {
                return new DependencyReport(this);
            }
        }
    }
}
