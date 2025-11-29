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
import com.aidvps.druid.differ.internal.model.DestructiveOperation.Severity;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable migration plan containing generated SQL and metadata.
 *
 * <p>A MigrationPlan represents the complete set of changes needed to migrate from a source schema
 * to a target schema, including the generated SQL statements, warnings, and information about
 * potentially destructive operations.
 */
public final class MigrationPlan {

    private final List<String> statements;
    private final List<Warning> warnings;
    private final List<DestructiveOperation> destructiveOperations;
    private final Schema sourceSchema;
    private final Schema targetSchema;
    private final DatabaseDialect databaseDialect;
    private final Instant createdAt;

    /**
     * Creates a new MigrationPlan.
     *
     * @param statements the SQL statements to execute
     * @param warnings warnings about the migration
     * @param destructiveOperations operations that may cause data loss
     * @param sourceSchema the source schema
     * @param targetSchema the target schema
     * @param databaseDialect the target database dialect
     */
    public MigrationPlan(
            List<String> statements,
            List<Warning> warnings,
            List<DestructiveOperation> destructiveOperations,
            Schema sourceSchema,
            Schema targetSchema,
            DatabaseDialect databaseDialect) {
        this.statements = Collections.unmodifiableList(new java.util.ArrayList<>(statements));
        this.warnings = Collections.unmodifiableList(new java.util.ArrayList<>(warnings));
        this.destructiveOperations =
                Collections.unmodifiableList(new java.util.ArrayList<>(destructiveOperations));
        this.sourceSchema = Objects.requireNonNull(sourceSchema, "Source schema cannot be null");
        this.targetSchema = Objects.requireNonNull(targetSchema, "Target schema cannot be null");
        this.databaseDialect =
                Objects.requireNonNull(databaseDialect, "Database dialect cannot be null");
        this.createdAt = Instant.now();
    }

    /**
     * Returns the SQL statements to execute.
     *
     * @return an unmodifiable list of SQL statements
     */
    public List<String> getStatements() {
        return statements;
    }

    /**
     * Returns warnings about the migration.
     *
     * @return an unmodifiable list of warnings
     */
    public List<Warning> getWarnings() {
        return warnings;
    }

    /**
     * Returns destructive operations that may cause data loss.
     *
     * @return an unmodifiable list of destructive operations
     */
    public List<DestructiveOperation> getDestructiveOperations() {
        return destructiveOperations;
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
     * Returns the database dialect.
     *
     * @return the database dialect (never null)
     */
    public DatabaseDialect getDatabaseDialect() {
        return databaseDialect;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return the creation timestamp (never null)
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the number of statements.
     *
     * @return the statement count
     */
    public int getStatementCount() {
        return statements.size();
    }

    /**
     * Returns whether the plan contains any destructive operations.
     *
     * @return true if there are destructive operations, false otherwise
     */
    public boolean hasDestructiveOperations() {
        return !destructiveOperations.isEmpty();
    }

    /**
     * Returns whether the plan contains any warnings.
     *
     * @return true if there are warnings, false otherwise
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns whether the plan is empty (no changes).
     *
     * @return true if there are no statements, false otherwise
     */
    public boolean isEmpty() {
        return statements.isEmpty();
    }

    /**
     * Returns all SQL statements joined into a single string.
     *
     * @return the SQL statements joined by newlines
     */
    public String getSql() {
        return String.join("\n", statements);
    }

    /**
     * Returns a summary of the migration plan.
     *
     * @return a human-readable summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Migration Plan Summary\n");
        sb.append("======================\n");
        sb.append("Source Schema: ").append(sourceSchema.getDialect());
        sourceSchema.getVersion().ifPresent(v -> sb.append(" v").append(v));
        sb.append("\n");
        sb.append("Target Schema: ").append(targetSchema.getDialect());
        targetSchema.getVersion().ifPresent(v -> sb.append(" v").append(v));
        sb.append("\n");
        sb.append("Database Dialect: ").append(databaseDialect).append("\n");
        sb.append("Statements: ").append(statements.size()).append("\n");
        sb.append("Warnings: ").append(warnings.size()).append("\n");
        sb.append("Destructive Operations: ").append(destructiveOperations.size()).append("\n");

        if (!destructiveOperations.isEmpty()) {
            sb.append("\nDestructive Operations Detected:\n");
            for (DestructiveOperation op : destructiveOperations) {
                sb.append("  - ")
                        .append(op.getOperationType())
                        .append(": ")
                        .append(op.getTargetObject());
                if (op.getSeverity() == Severity.HIGH) {
                    sb.append(" (HIGH RISK)");
                }
                sb.append("\n");
            }
        }

        if (!warnings.isEmpty()) {
            sb.append("\nWarnings:\n");
            for (Warning warning : warnings) {
                sb.append("  - [")
                        .append(warning.getSeverity())
                        .append("] ")
                        .append(warning.getMessage())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationPlan that = (MigrationPlan) o;
        return statements.equals(that.statements)
                && warnings.equals(that.warnings)
                && destructiveOperations.equals(that.destructiveOperations)
                && sourceSchema.equals(that.sourceSchema)
                && targetSchema.equals(that.targetSchema)
                && databaseDialect == that.databaseDialect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                statements,
                warnings,
                destructiveOperations,
                sourceSchema,
                targetSchema,
                databaseDialect);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    /** Builder for creating MigrationPlan instances. */
    public static class Builder {
        private final java.util.List<String> statements = new java.util.ArrayList<>();
        private final java.util.List<Warning> warnings = new java.util.ArrayList<>();
        private final java.util.List<DestructiveOperation> destructiveOperations =
                new java.util.ArrayList<>();
        private Schema sourceSchema;
        private Schema targetSchema;
        private DatabaseDialect databaseDialect;

        /**
         * Sets the source schema.
         *
         * @param sourceSchema the source schema
         * @return this Builder for chaining
         */
        public Builder sourceSchema(Schema sourceSchema) {
            this.sourceSchema = sourceSchema;
            return this;
        }

        /**
         * Sets the target schema.
         *
         * @param targetSchema the target schema
         * @return this Builder for chaining
         */
        public Builder targetSchema(Schema targetSchema) {
            this.targetSchema = targetSchema;
            return this;
        }

        /**
         * Sets the database dialect.
         *
         * @param databaseDialect the database dialect
         * @return this Builder for chaining
         */
        public Builder databaseDialect(DatabaseDialect databaseDialect) {
            this.databaseDialect = databaseDialect;
            return this;
        }

        /**
         * Adds a SQL statement.
         *
         * @param statement the SQL statement
         * @return this Builder for chaining
         */
        public Builder addStatement(String statement) {
            statements.add(statement);
            return this;
        }

        /**
         * Adds multiple SQL statements.
         *
         * @param statements the SQL statements
         * @return this Builder for chaining
         */
        public Builder addStatements(List<String> statements) {
            this.statements.addAll(statements);
            return this;
        }

        /**
         * Adds a warning.
         *
         * @param warning the warning
         * @return this Builder for chaining
         */
        public Builder addWarning(Warning warning) {
            warnings.add(warning);
            return this;
        }

        /**
         * Adds multiple warnings.
         *
         * @param warnings the warnings
         * @return this Builder for chaining
         */
        public Builder addWarnings(List<Warning> warnings) {
            this.warnings.addAll(warnings);
            return this;
        }

        /**
         * Adds a destructive operation.
         *
         * @param operation the destructive operation
         * @return this Builder for chaining
         */
        public Builder addDestructiveOperation(DestructiveOperation operation) {
            destructiveOperations.add(operation);
            return this;
        }

        /**
         * Adds multiple destructive operations.
         *
         * @param operations the destructive operations
         * @return this Builder for chaining
         */
        public Builder addDestructiveOperations(List<DestructiveOperation> operations) {
            this.destructiveOperations.addAll(operations);
            return this;
        }

        /**
         * Builds the MigrationPlan.
         *
         * @return a new MigrationPlan
         */
        public MigrationPlan build() {
            return new MigrationPlan(
                    statements,
                    warnings,
                    destructiveOperations,
                    sourceSchema,
                    targetSchema,
                    databaseDialect);
        }
    }
}
