package com.aidvps.schemakit.migrator;

import java.util.Objects;

/** Individual change description. */
public abstract class SchemaChange {
    private final ChangeType type;
    private final String description;
    private final String entityPath; // e.g., "mydb.users.email"

    protected SchemaChange(Builder<?> builder) {
        this.type = builder.type;
        this.description = builder.description;
        this.entityPath = builder.entityPath;
    }

    /** Change type enum. */
    public enum ChangeType {
        DATABASE_CREATED,
        DATABASE_DROPPED,
        TABLE_CREATED,
        TABLE_DROPPED,
        TABLE_MODIFIED,
        COLUMN_ADDED,
        COLUMN_DROPPED,
        COLUMN_MODIFIED,
        INDEX_CREATED,
        INDEX_DROPPED,
        CONSTRAINT_ADDED,
        CONSTRAINT_DROPPED
    }

    /**
     * Get the change type.
     *
     * @return The change type
     */
    public ChangeType getType() {
        return type;
    }

    /**
     * Get the description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the entity path.
     *
     * @return The entity path
     */
    public String getEntityPath() {
        return entityPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaChange that = (SchemaChange) o;
        return type == that.type && Objects.equals(entityPath, that.entityPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, entityPath);
    }

    @Override
    public String toString() {
        return type + ": " + description + " (" + entityPath + ")";
    }

    /**
     * Abstract builder for SchemaChange.
     *
     * @param <T> The builder type
     */
    public abstract static class Builder<T extends Builder<T>> {
        private ChangeType type;
        private String description;
        private String entityPath;

        protected Builder() {}

        /**
         * Set the change type.
         *
         * @param type The change type
         * @return This builder
         */
        public T type(ChangeType type) {
            this.type = type;
            return self();
        }

        /**
         * Set the description.
         *
         * @param description The description
         * @return This builder
         */
        public T description(String description) {
            this.description = description;
            return self();
        }

        /**
         * Set the entity path.
         *
         * @param entityPath The entity path
         * @return This builder
         */
        public T entityPath(String entityPath) {
            this.entityPath = entityPath;
            return self();
        }

        /**
         * Build the SchemaChange.
         *
         * @return The schema change
         */
        public abstract SchemaChange build();

        /**
         * Return this as the type T.
         *
         * @return This builder as type T
         */
        protected abstract T self();
    }
}
