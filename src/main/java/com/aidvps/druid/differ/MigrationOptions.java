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
 * options.
 */
public final class MigrationOptions {

    private final boolean wrapInTransaction;
    private final boolean includeComments;
    private final boolean failOnDestructive;

    /**
     * Creates new MigrationOptions with default values.
     *
     * @param wrapInTransaction whether to wrap migration in a transaction
     * @param includeComments whether to include explanatory comments in SQL
     * @param failOnDestructive whether to fail if destructive operations are detected
     */
    private MigrationOptions(
            boolean wrapInTransaction, boolean includeComments, boolean failOnDestructive) {
        this.wrapInTransaction = wrapInTransaction;
        this.includeComments = includeComments;
        this.failOnDestructive = failOnDestructive;
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
        return new MigrationOptions(false, true, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationOptions that = (MigrationOptions) o;
        return wrapInTransaction == that.wrapInTransaction
                && includeComments == that.includeComments
                && failOnDestructive == that.failOnDestructive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrapInTransaction, includeComments, failOnDestructive);
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
                + '}';
    }

    /** Builder for creating MigrationOptions instances. */
    public static class Builder {
        private boolean wrapInTransaction = false;
        private boolean includeComments = true;
        private boolean failOnDestructive = false;

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
         * Builds the MigrationOptions.
         *
         * @return a new MigrationOptions instance
         */
        public MigrationOptions build() {
            return new MigrationOptions(wrapInTransaction, includeComments, failOnDestructive);
        }
    }
}
