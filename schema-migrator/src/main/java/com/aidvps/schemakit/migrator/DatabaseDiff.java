package com.aidvps.schemakit.migrator;

import com.aidvps.druid.differ.internal.model.Database;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Diff for a specific database. */
public final class DatabaseDiff {
    private final String databaseName;
    private final Database source;
    private final Database target;
    private final Set<TableDiff> tableDiffs;
    private final boolean isNew;
    private final boolean isDeleted;

    private DatabaseDiff(Builder builder) {
        this.databaseName = builder.databaseName;
        this.source = builder.source;
        this.target = builder.target;
        this.tableDiffs =
                builder.tableDiffs != null ? new HashSet<>(builder.tableDiffs) : new HashSet<>();
        this.isNew = builder.isNew;
        this.isDeleted = builder.isDeleted;
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
     * Get the database name.
     *
     * @return The database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Get the source database.
     *
     * @return The source database
     */
    public Database getSource() {
        return source;
    }

    /**
     * Get the target database.
     *
     * @return The target database
     */
    public Database getTarget() {
        return target;
    }

    /**
     * Get all table diffs.
     *
     * @return The set of table diffs
     */
    public Set<TableDiff> getTableDiffs() {
        return Collections.unmodifiableSet(tableDiffs);
    }

    /**
     * Check if the database is new.
     *
     * @return true if new
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Check if the database is deleted.
     *
     * @return true if deleted
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Check if the database is modified.
     *
     * @return true if modified
     */
    public boolean isModified() {
        return !isNew && !isDeleted && !tableDiffs.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseDiff that = (DatabaseDiff) o;
        return Objects.equals(databaseName, that.databaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(databaseName);
    }

    @Override
    public String toString() {
        return "DatabaseDiff{"
                + "databaseName='"
                + databaseName
                + '\''
                + ", isNew="
                + isNew
                + ", isDeleted="
                + isDeleted
                + ", tableDiffs="
                + tableDiffs.size()
                + '}';
    }

    /** Builder for DatabaseDiff. */
    public static class Builder {
        private String databaseName;
        private Database source;
        private Database target;
        private Set<TableDiff> tableDiffs;
        private boolean isNew;
        private boolean isDeleted;

        private Builder() {}

        /**
         * Set the database name.
         *
         * @param databaseName The database name
         * @return This builder
         */
        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        /**
         * Set the source database.
         *
         * @param source The source database
         * @return This builder
         */
        public Builder source(Database source) {
            this.source = source;
            return this;
        }

        /**
         * Set the target database.
         *
         * @param target The target database
         * @return This builder
         */
        public Builder target(Database target) {
            this.target = target;
            return this;
        }

        /**
         * Add a table diff.
         *
         * @param tableDiff The table diff
         * @return This builder
         */
        public Builder tableDiff(TableDiff tableDiff) {
            if (tableDiffs == null) {
                tableDiffs = new HashSet<>();
            }
            tableDiffs.add(tableDiff);
            return this;
        }

        /**
         * Set whether this database is new.
         *
         * @param isNew true if new
         * @return This builder
         */
        public Builder isNew(boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        /**
         * Set whether this database is deleted.
         *
         * @param isDeleted true if deleted
         * @return This builder
         */
        public Builder isDeleted(boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        /**
         * Build the DatabaseDiff instance.
         *
         * @return The diff instance
         */
        public DatabaseDiff build() {
            if (databaseName == null) {
                throw new IllegalStateException("Database name must be set");
            }
            return new DatabaseDiff(this);
        }
    }
}
