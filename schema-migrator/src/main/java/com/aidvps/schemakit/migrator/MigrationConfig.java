package com.aidvps.schemakit.migrator;

import com.aidvps.schemakit.core.DatabasePlatform;
import java.util.Optional;

/** Configuration for migration generation. */
public interface MigrationConfig {
    /**
     * Get target database platform.
     *
     * @return DatabasePlatform (MYSQL, POSTGRESQL, MARIADB, SQLITE)
     */
    DatabasePlatform getTargetPlatform();

    /**
     * Get migration mode.
     *
     * @return MigrationMode
     */
    MigrationMode getMode();

    /**
     * Whether to include DROP statements.
     *
     * @return true to include, false to skip
     */
    boolean isIncludeDrops();

    /**
     * Whether to wrap in transactions.
     *
     * @return true to wrap in transactions
     */
    boolean isTransactional();

    /**
     * Get custom SQL generator for specific features.
     *
     * @return Optional custom generator
     */
    Optional<CustomSqlGenerator> getCustomGenerator();

    /** Create builder. */
    static Builder builder() {
        return new Builder();
    }

    /** Builder for MigrationConfig. */
    class Builder {
        private DatabasePlatform targetPlatform;
        private MigrationMode mode = MigrationMode.FULL;
        private boolean includeDrops = true;
        private boolean transactional = false;
        private CustomSqlGenerator customGenerator;

        private Builder() {}

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
         * Set whether to include drops.
         *
         * @param includeDrops true to include drops
         * @return This builder
         */
        public Builder includeDrops(boolean includeDrops) {
            this.includeDrops = includeDrops;
            return this;
        }

        /**
         * Set whether to use transactions.
         *
         * @param transactional true to use transactions
         * @return This builder
         */
        public Builder transactional(boolean transactional) {
            this.transactional = transactional;
            return this;
        }

        /**
         * Set the custom SQL generator.
         *
         * @param generator The custom SQL generator
         * @return This builder
         */
        public Builder customGenerator(CustomSqlGenerator generator) {
            this.customGenerator = generator;
            return this;
        }

        /**
         * Build the MigrationConfig instance.
         *
         * @return The configuration instance
         */
        public MigrationConfig build() {
            return new MigrationConfigImpl(this);
        }
    }

    /** Implementation of MigrationConfig. */
    class MigrationConfigImpl implements MigrationConfig {
        private final DatabasePlatform targetPlatform;
        private final MigrationMode mode;
        private final boolean includeDrops;
        private final boolean transactional;
        private final CustomSqlGenerator customGenerator;

        private MigrationConfigImpl(Builder builder) {
            this.targetPlatform = builder.targetPlatform;
            this.mode = builder.mode;
            this.includeDrops = builder.includeDrops;
            this.transactional = builder.transactional;
            this.customGenerator = builder.customGenerator;
        }

        @Override
        public DatabasePlatform getTargetPlatform() {
            return targetPlatform;
        }

        @Override
        public MigrationMode getMode() {
            return mode;
        }

        @Override
        public boolean isIncludeDrops() {
            return includeDrops;
        }

        @Override
        public boolean isTransactional() {
            return transactional;
        }

        @Override
        public Optional<CustomSqlGenerator> getCustomGenerator() {
            return Optional.ofNullable(customGenerator);
        }
    }
}
