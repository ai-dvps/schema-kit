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

package com.aidvps.druid.differ;

import java.util.Objects;

/**
 * Configuration options for migration generation.
 *
 * <p>This class allows users to customize how migrations are generated, including whether to wrap
 * the migration in a transaction, include comments, fail on destructive operations, and other
 * options for CI/CD integration.
 */
public final class MigrationOptions {

    private final boolean wrapInTransaction;
    private final boolean includeComments;
    private final boolean failOnDestructive;
    private final boolean includeRollback;
    private final boolean dryRun;
    private final long timeoutMillis;

    /**
     * Creates new MigrationOptions with default values.
     *
     * @param wrapInTransaction whether to wrap migration in a transaction
     * @param includeComments whether to include explanatory comments in SQL
     * @param failOnDestructive whether to fail if destructive operations are detected
     * @param includeRollback whether to include rollback SQL in the migration plan
     * @param dryRun whether to only validate without generating SQL
     * @param timeoutMillis timeout for migration generation in milliseconds
     */
    private MigrationOptions(
            boolean wrapInTransaction,
            boolean includeComments,
            boolean failOnDestructive,
            boolean includeRollback,
            boolean dryRun,
            long timeoutMillis) {
        this.wrapInTransaction = wrapInTransaction;
        this.includeComments = includeComments;
        this.failOnDestructive = failOnDestructive;
        this.includeRollback = includeRollback;
        this.dryRun = dryRun;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns whether to wrap the migration in a transaction.
     *
     * @return true if transactions should be used, false otherwise
     */
    public boolean isWrapInTransaction() {
        return wrapInTransaction;
    }

    /**
     * Returns whether to include comments in generated SQL.
     *
     * @return true if comments should be included, false otherwise
     */
    public boolean isIncludeComments() {
        return includeComments;
    }

    /**
     * Returns whether to fail if destructive operations are detected.
     *
     * @return true if the migration should fail on destructive operations, false otherwise
     */
    public boolean isFailOnDestructive() {
        return failOnDestructive;
    }

    /**
     * Returns whether to include rollback SQL in the migration plan.
     *
     * @return true if rollback SQL should be included, false otherwise
     */
    public boolean isIncludeRollback() {
        return includeRollback;
    }

    /**
     * Returns whether this is a dry run (validate without generating SQL).
     *
     * @return true if this is a dry run, false otherwise
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Returns the timeout for migration generation in milliseconds.
     *
     * @return the timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Creates a new Builder for MigrationOptions.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates MigrationOptions with default values.
     *
     * @return default MigrationOptions
     */
    public static MigrationOptions defaults() {
        return new MigrationOptions(false, true, false, false, false, 30000);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationOptions that = (MigrationOptions) o;
        return wrapInTransaction == that.wrapInTransaction
                && includeComments == that.includeComments
                && failOnDestructive == that.failOnDestructive
                && includeRollback == that.includeRollback
                && dryRun == that.dryRun
                && timeoutMillis == that.timeoutMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                wrapInTransaction,
                includeComments,
                failOnDestructive,
                includeRollback,
                dryRun,
                timeoutMillis);
    }

    @Override
    public String toString() {
        return "MigrationOptions{"
                + "wrapInTransaction="
                + wrapInTransaction
                + ", includeComments="
                + includeComments
                + ", failOnDestructive="
                + failOnDestructive
                + ", includeRollback="
                + includeRollback
                + ", dryRun="
                + dryRun
                + ", timeoutMillis="
                + timeoutMillis
                + '}';
    }

    /** Builder for creating MigrationOptions instances. */
    public static class Builder {
        private boolean wrapInTransaction = false;
        private boolean includeComments = true;
        private boolean failOnDestructive = false;
        private boolean includeRollback = false;
        private boolean dryRun = false;
        private long timeoutMillis = 30000;

        /**
         * Sets whether to wrap the migration in a transaction.
         *
         * @param wrapInTransaction true to wrap in transaction
         * @return this Builder for chaining
         */
        public Builder wrapInTransaction(boolean wrapInTransaction) {
            this.wrapInTransaction = wrapInTransaction;
            return this;
        }

        /**
         * Sets whether to include comments in generated SQL.
         *
         * @param includeComments true to include comments
         * @return this Builder for chaining
         */
        public Builder includeComments(boolean includeComments) {
            this.includeComments = includeComments;
            return this;
        }

        /**
         * Sets whether to fail if destructive operations are detected.
         *
         * @param failOnDestructive true to fail on destructive operations
         * @return this Builder for chaining
         */
        public Builder failOnDestructive(boolean failOnDestructive) {
            this.failOnDestructive = failOnDestructive;
            return this;
        }

        /**
         * Sets whether to include rollback SQL in the migration plan.
         *
         * @param includeRollback true to include rollback SQL
         * @return this Builder for chaining
         */
        public Builder includeRollback(boolean includeRollback) {
            this.includeRollback = includeRollback;
            return this;
        }

        /**
         * Sets whether to perform a dry run (validate without generating SQL).
         *
         * @param dryRun true to perform a dry run
         * @return this Builder for chaining
         */
        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        /**
         * Sets the timeout for migration generation.
         *
         * @param timeoutMillis the timeout in milliseconds
         * @return this Builder for chaining
         */
        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * Builds the MigrationOptions.
         *
         * @return a new MigrationOptions instance
         */
        public MigrationOptions build() {
            return new MigrationOptions(
                    wrapInTransaction,
                    includeComments,
                    failOnDestructive,
                    includeRollback,
                    dryRun,
                    timeoutMillis);
        }
    }
}
