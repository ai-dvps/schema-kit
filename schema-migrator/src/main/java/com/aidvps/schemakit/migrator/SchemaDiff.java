package com.aidvps.schemakit.migrator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Complete diff between two schemas. */
public final class SchemaDiff {
    private final Set<DatabaseDiff> databaseDiffs;
    private final Set<SchemaChange> changes;

    private SchemaDiff(Builder builder) {
        this.databaseDiffs =
                builder.databaseDiffs != null
                        ? new HashSet<>(builder.databaseDiffs)
                        : new HashSet<>();
        this.changes = builder.changes != null ? new HashSet<>(builder.changes) : new HashSet<>();
    }

    /**
     * Create a new builder.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get diff for specific database.
     *
     * @param databaseName The database name
     * @return Optional containing the diff if found
     */
    public Optional<DatabaseDiff> getDatabaseDiff(String databaseName) {
        return databaseDiffs.stream()
                .filter(diff -> diff.getDatabaseName().equals(databaseName))
                .findFirst();
    }

    /**
     * Check if any changes detected.
     *
     * @return true if changes detected
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Get all database diffs.
     *
     * @return The set of database diffs
     */
    public Set<DatabaseDiff> getDatabaseDiffs() {
        return Collections.unmodifiableSet(databaseDiffs);
    }

    /**
     * Get all changes.
     *
     * @return The set of changes
     */
    public Set<SchemaChange> getChanges() {
        return Collections.unmodifiableSet(changes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaDiff schemaDiff = (SchemaDiff) o;
        return Objects.equals(databaseDiffs, schemaDiff.databaseDiffs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseDiffs);
    }

    @Override
    public String toString() {
        return "SchemaDiff{"
                + "databaseDiffs="
                + databaseDiffs.size()
                + ", changes="
                + changes.size()
                + '}';
    }

    /** Builder for SchemaDiff. */
    public static class Builder {
        private Set<DatabaseDiff> databaseDiffs;
        private Set<SchemaChange> changes;

        private Builder() {}

        /**
         * Add a database diff.
         *
         * @param databaseDiff The database diff
         * @return This builder
         */
        public Builder databaseDiff(DatabaseDiff databaseDiff) {
            if (databaseDiffs == null) {
                databaseDiffs = new HashSet<>();
            }
            databaseDiffs.add(databaseDiff);
            return this;
        }

        /**
         * Add a schema change.
         *
         * @param change The schema change
         * @return This builder
         */
        public Builder change(SchemaChange change) {
            if (changes == null) {
                changes = new HashSet<>();
            }
            changes.add(change);
            return this;
        }

        /**
         * Build the SchemaDiff instance.
         *
         * @return The diff instance
         */
        public SchemaDiff build() {
            return new SchemaDiff(this);
        }
    }
}
