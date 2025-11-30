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

package com.aidvps.druid.differ.internal.generator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a single migration statement with its dependencies.
 *
 * <p>Migration statements may have dependencies on other statements that must execute before them.
 * For example, adding a foreign key depends on the referenced table existing.
 */
public final class MigrationStatement {

    /** Type of migration operation. */
    public enum Type {
        CREATE_TABLE,
        DROP_TABLE,
        ADD_COLUMN,
        DROP_COLUMN,
        MODIFY_COLUMN,
        ADD_CONSTRAINT,
        DROP_CONSTRAINT,
        ADD_INDEX,
        DROP_INDEX,
        MODIFY_TABLE
    }

    private final String sql;
    private final Type type;
    private final String tableName;
    private final Set<String> dependsOn;
    private final int priority;

    /**
     * Creates a new MigrationStatement.
     *
     * @param sql the SQL statement
     * @param type the statement type
     * @param tableName the affected table name
     * @param dependsOn set of table names this statement depends on
     * @param priority execution priority (lower executes first)
     */
    public MigrationStatement(
            String sql, Type type, String tableName, Set<String> dependsOn, int priority) {
        this.sql = Objects.requireNonNull(sql, "SQL cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.tableName = Objects.requireNonNull(tableName, "Table name cannot be null");
        this.dependsOn =
                new HashSet<>(Objects.requireNonNull(dependsOn, "Dependencies cannot be null"));
        this.priority = priority;
    }

    /** Returns the SQL statement. */
    public String getSql() {
        return sql;
    }

    /** Returns the statement type. */
    public Type getType() {
        return type;
    }

    /** Returns the affected table name. */
    public String getTableName() {
        return tableName;
    }

    /** Returns the set of table names this statement depends on. */
    public Set<String> getDependsOn() {
        return Collections.unmodifiableSet(dependsOn);
    }

    /** Returns the execution priority (lower values execute first). */
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MigrationStatement that = (MigrationStatement) o;
        return priority == that.priority
                && sql.equals(that.sql)
                && type == that.type
                && tableName.equals(that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, type, tableName, priority);
    }

    @Override
    public String toString() {
        return "MigrationStatement{"
                + "type="
                + type
                + ", table='"
                + tableName
                + '\''
                + ", priority="
                + priority
                + ", dependsOn="
                + dependsOn
                + '}';
    }

    /** Builder for creating MigrationStatement instances. */
    public static class Builder {
        private String sql;
        private Type type;
        private String tableName;
        private final Set<String> dependsOn = new HashSet<>();
        private int priority = 50; // Default middle priority

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder addDependency(String tableName) {
            this.dependsOn.add(tableName);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public MigrationStatement build() {
            return new MigrationStatement(sql, type, tableName, dependsOn, priority);
        }
    }
}
