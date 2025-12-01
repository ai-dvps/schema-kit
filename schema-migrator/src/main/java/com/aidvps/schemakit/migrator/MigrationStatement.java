package com.aidvps.schemakit.migrator;

import java.util.HashSet;
import java.util.Set;

/** Individual SQL statement with metadata. */
public final class MigrationStatement {
    private final String sql;
    private final StatementType type;
    private final String description;
    private final int order;
    private final Set<String> dependencies;

    private MigrationStatement(Builder builder) {
        this.sql = builder.sql;
        this.type = builder.type;
        this.description = builder.description;
        this.order = builder.order;
        this.dependencies =
                builder.dependencies != null
                        ? new HashSet<>(builder.dependencies)
                        : new HashSet<>();
    }

    /** Statement type enum. */
    public enum StatementType {
        CREATE_DATABASE,
        DROP_DATABASE,
        CREATE_TABLE,
        ALTER_TABLE,
        DROP_TABLE,
        CREATE_INDEX,
        DROP_INDEX,
        CREATE_CONSTRAINT,
        DROP_CONSTRAINT,
        CUSTOM
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
     * Get the SQL.
     *
     * @return The SQL statement
     */
    public String getSql() {
        return sql;
    }

    /**
     * Get the statement type.
     *
     * @return The statement type
     */
    public StatementType getType() {
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
     * Get the order.
     *
     * @return The execution order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Get the dependencies.
     *
     * @return The set of dependencies
     */
    public Set<String> getDependencies() {
        return new HashSet<>(dependencies);
    }

    /** Builder for MigrationStatement. */
    public static class Builder {
        private String sql;
        private StatementType type;
        private String description;
        private int order;
        private Set<String> dependencies;

        private Builder() {}

        /**
         * Set the SQL.
         *
         * @param sql The SQL statement
         * @return This builder
         */
        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        /**
         * Set the statement type.
         *
         * @param type The statement type
         * @return This builder
         */
        public Builder type(StatementType type) {
            this.type = type;
            return this;
        }

        /**
         * Set the description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the execution order.
         *
         * @param order The execution order
         * @return This builder
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * Add a dependency.
         *
         * @param dependency The dependency
         * @return This builder
         */
        public Builder dependency(String dependency) {
            if (dependencies == null) {
                dependencies = new HashSet<>();
            }
            dependencies.add(dependency);
            return this;
        }

        /**
         * Build the MigrationStatement instance.
         *
         * @return The statement instance
         */
        public MigrationStatement build() {
            if (sql == null) {
                throw new IllegalStateException("SQL must be set");
            }
            if (type == null) {
                throw new IllegalStateException("Type must be set");
            }
            return new MigrationStatement(this);
        }
    }
}
