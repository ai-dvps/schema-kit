package com.aidvps.schemakit.migrator;

import com.aidvps.schemakit.core.Column;
import com.aidvps.schemakit.core.Constraint;
import com.aidvps.schemakit.core.Index;
import com.aidvps.schemakit.core.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Diff for a specific table. */
public final class TableDiff {
    private final String tableName;
    private final Table source;
    private final Table target;
    private final Set<ColumnDiff> columnDiffs;
    private final Set<IndexDiff> indexDiffs;
    private final Set<ConstraintDiff> constraintDiffs;
    private final boolean isNew;
    private final boolean isDeleted;
    private final boolean isModified;

    private TableDiff(Builder builder) {
        this.tableName = builder.tableName;
        this.source = builder.source;
        this.target = builder.target;
        this.columnDiffs =
                builder.columnDiffs != null ? new HashSet<>(builder.columnDiffs) : new HashSet<>();
        this.indexDiffs =
                builder.indexDiffs != null ? new HashSet<>(builder.indexDiffs) : new HashSet<>();
        this.constraintDiffs =
                builder.constraintDiffs != null
                        ? new HashSet<>(builder.constraintDiffs)
                        : new HashSet<>();
        this.isNew = builder.isNew;
        this.isDeleted = builder.isDeleted;
        this.isModified = builder.isModified;
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
     * Get the table name.
     *
     * @return The table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Get the source table.
     *
     * @return The source table
     */
    public Table getSource() {
        return source;
    }

    /**
     * Get the target table.
     *
     * @return The target table
     */
    public Table getTarget() {
        return target;
    }

    /**
     * Get all column diffs.
     *
     * @return The set of column diffs
     */
    public Set<ColumnDiff> getColumnDiffs() {
        return Collections.unmodifiableSet(columnDiffs);
    }

    /**
     * Get all index diffs.
     *
     * @return The set of index diffs
     */
    public Set<IndexDiff> getIndexDiffs() {
        return Collections.unmodifiableSet(indexDiffs);
    }

    /**
     * Get all constraint diffs.
     *
     * @return The set of constraint diffs
     */
    public Set<ConstraintDiff> getConstraintDiffs() {
        return Collections.unmodifiableSet(constraintDiffs);
    }

    /**
     * Check if the table is new.
     *
     * @return true if new
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Check if the table is deleted.
     *
     * @return true if deleted
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Check if the table is modified.
     *
     * @return true if modified
     */
    public boolean isModified() {
        return isModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableDiff tableDiff = (TableDiff) o;
        return Objects.equals(tableName, tableDiff.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName);
    }

    @Override
    public String toString() {
        return "TableDiff{"
                + "tableName='"
                + tableName
                + '\''
                + ", isNew="
                + isNew
                + ", isDeleted="
                + isDeleted
                + ", isModified="
                + isModified
                + ", columnDiffs="
                + columnDiffs.size()
                + '}';
    }

    /** Column diff. */
    public static class ColumnDiff {
        private final String columnName;
        private final Column source;
        private final Column target;
        private final boolean isNew;
        private final boolean isDeleted;
        private final boolean isModified;

        public ColumnDiff(
                String columnName,
                Column source,
                Column target,
                boolean isNew,
                boolean isDeleted,
                boolean isModified) {
            this.columnName = columnName;
            this.source = source;
            this.target = target;
            this.isNew = isNew;
            this.isDeleted = isDeleted;
            this.isModified = isModified;
        }

        public String getColumnName() {
            return columnName;
        }

        public Column getSource() {
            return source;
        }

        public Column getTarget() {
            return target;
        }

        public boolean isNew() {
            return isNew;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public boolean isModified() {
            return isModified;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnDiff that = (ColumnDiff) o;
            return Objects.equals(columnName, that.columnName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(columnName);
        }

        @Override
        public String toString() {
            return "ColumnDiff{"
                    + "columnName='"
                    + columnName
                    + '\''
                    + ", isNew="
                    + isNew
                    + ", isDeleted="
                    + isDeleted
                    + ", isModified="
                    + isModified
                    + '}';
        }
    }

    /** Index diff. */
    public static class IndexDiff {
        private final String indexName;
        private final Index source;
        private final Index target;
        private final boolean isNew;
        private final boolean isDeleted;
        private final boolean isModified;

        public IndexDiff(
                String indexName,
                Index source,
                Index target,
                boolean isNew,
                boolean isDeleted,
                boolean isModified) {
            this.indexName = indexName;
            this.source = source;
            this.target = target;
            this.isNew = isNew;
            this.isDeleted = isDeleted;
            this.isModified = isModified;
        }

        public String getIndexName() {
            return indexName;
        }

        public Index getSource() {
            return source;
        }

        public Index getTarget() {
            return target;
        }

        public boolean isNew() {
            return isNew;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public boolean isModified() {
            return isModified;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IndexDiff indexDiff = (IndexDiff) o;
            return Objects.equals(indexName, indexDiff.indexName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indexName);
        }

        @Override
        public String toString() {
            return "IndexDiff{"
                    + "indexName='"
                    + indexName
                    + '\''
                    + ", isNew="
                    + isNew
                    + ", isDeleted="
                    + isDeleted
                    + ", isModified="
                    + isModified
                    + '}';
        }
    }

    /** Constraint diff. */
    public static class ConstraintDiff {
        private final String constraintName;
        private final Constraint source;
        private final Constraint target;
        private final boolean isNew;
        private final boolean isDeleted;
        private final boolean isModified;

        public ConstraintDiff(
                String constraintName,
                Constraint source,
                Constraint target,
                boolean isNew,
                boolean isDeleted,
                boolean isModified) {
            this.constraintName = constraintName;
            this.source = source;
            this.target = target;
            this.isNew = isNew;
            this.isDeleted = isDeleted;
            this.isModified = isModified;
        }

        public String getConstraintName() {
            return constraintName;
        }

        public Constraint getSource() {
            return source;
        }

        public Constraint getTarget() {
            return target;
        }

        public boolean isNew() {
            return isNew;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public boolean isModified() {
            return isModified;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstraintDiff that = (ConstraintDiff) o;
            return Objects.equals(constraintName, that.constraintName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(constraintName);
        }

        @Override
        public String toString() {
            return "ConstraintDiff{"
                    + "constraintName='"
                    + constraintName
                    + '\''
                    + ", isNew="
                    + isNew
                    + ", isDeleted="
                    + isDeleted
                    + ", isModified="
                    + isModified
                    + '}';
        }
    }

    /** Builder for TableDiff. */
    public static class Builder {
        private String tableName;
        private Table source;
        private Table target;
        private Set<ColumnDiff> columnDiffs;
        private Set<IndexDiff> indexDiffs;
        private Set<ConstraintDiff> constraintDiffs;
        private boolean isNew;
        private boolean isDeleted;
        private boolean isModified;

        private Builder() {}

        /**
         * Set the table name.
         *
         * @param tableName The table name
         * @return This builder
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * Set the source table.
         *
         * @param source The source table
         * @return This builder
         */
        public Builder source(Table source) {
            this.source = source;
            return this;
        }

        /**
         * Set the target table.
         *
         * @param target The target table
         * @return This builder
         */
        public Builder target(Table target) {
            this.target = target;
            return this;
        }

        /**
         * Add a column diff.
         *
         * @param columnDiff The column diff
         * @return This builder
         */
        public Builder columnDiff(ColumnDiff columnDiff) {
            if (columnDiffs == null) {
                columnDiffs = new HashSet<>();
            }
            columnDiffs.add(columnDiff);
            return this;
        }

        /**
         * Add an index diff.
         *
         * @param indexDiff The index diff
         * @return This builder
         */
        public Builder indexDiff(IndexDiff indexDiff) {
            if (indexDiffs == null) {
                indexDiffs = new HashSet<>();
            }
            indexDiffs.add(indexDiff);
            return this;
        }

        /**
         * Add a constraint diff.
         *
         * @param constraintDiff The constraint diff
         * @return This builder
         */
        public Builder constraintDiff(ConstraintDiff constraintDiff) {
            if (constraintDiffs == null) {
                constraintDiffs = new HashSet<>();
            }
            constraintDiffs.add(constraintDiff);
            return this;
        }

        /**
         * Set whether this table is new.
         *
         * @param isNew true if new
         * @return This builder
         */
        public Builder isNew(boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        /**
         * Set whether this table is deleted.
         *
         * @param isDeleted true if deleted
         * @return This builder
         */
        public Builder isDeleted(boolean isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        /**
         * Set whether this table is modified.
         *
         * @param isModified true if modified
         * @return This builder
         */
        public Builder isModified(boolean isModified) {
            this.isModified = isModified;
            return this;
        }

        /**
         * Build the TableDiff instance.
         *
         * @return The diff instance
         */
        public TableDiff build() {
            if (tableName == null) {
                throw new IllegalStateException("Table name must be set");
            }
            return new TableDiff(this);
        }
    }
}
