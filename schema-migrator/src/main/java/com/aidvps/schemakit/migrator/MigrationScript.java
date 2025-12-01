package com.aidvps.schemakit.migrator;

import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Generated migration SQL with metadata. */
public final class MigrationScript {
    private final List<MigrationStatement> statements;
    private final DatabasePlatform targetPlatform;
    private final MigrationMode mode;
    private final java.util.Map<String, Object> metadata;

    private MigrationScript(Builder builder) {
        this.statements =
                builder.statements != null
                        ? new ArrayList<>(builder.statements)
                        : new ArrayList<>();
        this.targetPlatform = builder.targetPlatform;
        this.mode = builder.mode;
        this.metadata =
                builder.metadata != null
                        ? new java.util.HashMap<>(builder.metadata)
                        : new java.util.HashMap<>();
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
     * Add statement to script.
     *
     * @param statement The migration statement
     * @return This script
     */
    public MigrationScript addStatement(MigrationStatement statement) {
        statements.add(statement);
        return this;
    }

    /**
     * Get all statements.
     *
     * @return The list of statements
     */
    public List<MigrationStatement> getStatements() {
        return Collections.unmodifiableList(statements);
    }

    /**
     * Get the target platform.
     *
     * @return The target platform
     */
    public DatabasePlatform getTargetPlatform() {
        return targetPlatform;
    }

    /**
     * Get the migration mode.
     *
     * @return The migration mode
     */
    public MigrationMode getMode() {
        return mode;
    }

    /**
     * Get metadata.
     *
     * @return The metadata map
     */
    public java.util.Map<String, Object> getMetadata() {
        return new java.util.HashMap<>(metadata);
    }

    /**
     * Convert to SQL string.
     *
     * @return The SQL string
     */
    public String toSql() {
        StringBuilder sb = new StringBuilder();
        for (MigrationStatement statement : statements) {
            sb.append(statement.getSql()).append(";\n");
        }
        return sb.toString();
    }

    /**
     * Convert to formatted SQL string.
     *
     * @return The formatted SQL string
     */
    public String toFormattedSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Migration Script\n");
        sb.append("-- Target Platform: ").append(targetPlatform).append("\n");
        sb.append("-- Mode: ").append(mode).append("\n\n");

        for (MigrationStatement statement : statements) {
            if (statement.getDescription() != null) {
                sb.append("-- ").append(statement.getDescription()).append("\n");
            }
            sb.append(statement.getSql()).append(";\n\n");
        }

        return sb.toString();
    }

    /** Builder for MigrationScript. */
    public static class Builder {
        private List<MigrationStatement> statements;
        private DatabasePlatform targetPlatform;
        private MigrationMode mode;
        private java.util.Map<String, Object> metadata;

        private Builder() {}

        /**
         * Add a statement.
         *
         * @param statement The migration statement
         * @return This builder
         */
        public Builder statement(MigrationStatement statement) {
            if (statements == null) {
                statements = new ArrayList<>();
            }
            if (statement != null) {
                statements.add(statement);
            }
            return this;
        }

        /**
         * Set the target platform.
         *
         * @param platform The target platform
         * @return This builder
         */
        public Builder targetPlatform(DatabasePlatform platform) {
            this.targetPlatform = platform;
            return this;
        }

        /**
         * Set the migration mode.
         *
         * @param mode The migration mode
         * @return This builder
         */
        public Builder mode(MigrationMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Add metadata.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder
         */
        public Builder metadata(String key, Object value) {
            if (metadata == null) {
                metadata = new java.util.HashMap<>();
            }
            metadata.put(key, value);
            return this;
        }

        /**
         * Build the MigrationScript instance.
         *
         * @return The script instance
         */
        public MigrationScript build() {
            if (targetPlatform == null) {
                throw new IllegalStateException("Target platform must be set");
            }
            if (mode == null) {
                throw new IllegalStateException("Mode must be set");
            }
            return new MigrationScript(this);
        }
    }
}
