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

package com.aidvps.druid.differ.internal.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents differences between two column definitions.
 *
 * <p>A ColumnDiff captures all semantic differences between two versions of a column, including
 * changes to data type, nullability, default values, and other properties.
 */
public final class ColumnDiff {

    private final Column oldColumn;
    private final Column newColumn;
    private final Set<ChangeType> changes;

    /** Enumeration of possible column changes. */
    public enum ChangeType {
        DATA_TYPE_CHANGED,
        NULLABILITY_CHANGED,
        DEFAULT_VALUE_CHANGED,
        AUTO_INCREMENT_CHANGED,
        CHARACTER_SET_CHANGED,
        COLLATION_CHANGED,
        COMMENT_CHANGED,
        LENGTH_CHANGED,
        PRECISION_CHANGED,
        SCALE_CHANGED
    }

    /**
     * Creates a new ColumnDiff.
     *
     * @param oldColumn the original column definition (never null)
     * @param newColumn the new column definition (never null)
     * @param changes the set of changes detected (never null)
     */
    public ColumnDiff(Column oldColumn, Column newColumn, Set<ChangeType> changes) {
        this.oldColumn = Objects.requireNonNull(oldColumn, "Old column cannot be null");
        this.newColumn = Objects.requireNonNull(newColumn, "New column cannot be null");
        this.changes = Collections.unmodifiableSet(EnumSet.copyOf(changes));
    }

    /**
     * Returns the original column definition.
     *
     * @return the old column (never null)
     */
    public Column getOldColumn() {
        return oldColumn;
    }

    /**
     * Returns the new column definition.
     *
     * @return the new column (never null)
     */
    public Column getNewColumn() {
        return newColumn;
    }

    /**
     * Returns all changes detected.
     *
     * @return an unmodifiable set of ChangeType
     */
    public Set<ChangeType> getChanges() {
        return changes;
    }

    /**
     * Checks if a specific change type was detected.
     *
     * @param changeType the change type to check
     * @return true if this change was detected, false otherwise
     */
    public boolean hasChange(ChangeType changeType) {
        return changes.contains(changeType);
    }

    /**
     * Returns whether the column definition is identical.
     *
     * @return true if no changes detected, false otherwise
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * Returns a human-readable description of the changes.
     *
     * @return a description of all changes
     */
    public String getDescription() {
        if (changes.isEmpty()) {
            return "No changes";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Column changes: ");

        for (ChangeType change : changes) {
            switch (change) {
                case DATA_TYPE_CHANGED:
                    sb.append(
                            String.format(
                                    "type: %s -> %s, ",
                                    oldColumn.getDataType(), newColumn.getDataType()));
                    break;
                case NULLABILITY_CHANGED:
                    sb.append(
                            String.format(
                                    "nullability: %s -> %s, ",
                                    oldColumn.isNullable() ? "NULL" : "NOT NULL",
                                    newColumn.isNullable() ? "NULL" : "NOT NULL"));
                    break;
                case DEFAULT_VALUE_CHANGED:
                    sb.append(
                            String.format(
                                    "default: %s -> %s, ",
                                    oldColumn.getDefaultValue().orElse("none"),
                                    newColumn.getDefaultValue().orElse("none")));
                    break;
                case AUTO_INCREMENT_CHANGED:
                    sb.append(
                            String.format(
                                    "autoIncrement: %s -> %s, ",
                                    oldColumn.isAutoIncrement(), newColumn.isAutoIncrement()));
                    break;
                case LENGTH_CHANGED:
                    sb.append(
                            String.format(
                                    "length: %s -> %s, ",
                                    oldColumn.getLength().map(Object::toString).orElse("none"),
                                    newColumn.getLength().map(Object::toString).orElse("none")));
                    break;
                case PRECISION_CHANGED:
                    sb.append(
                            String.format(
                                    "precision: %s -> %s, ",
                                    oldColumn.getPrecision().map(Object::toString).orElse("none"),
                                    newColumn.getPrecision().map(Object::toString).orElse("none")));
                    break;
                case SCALE_CHANGED:
                    sb.append(
                            String.format(
                                    "scale: %s -> %s, ",
                                    oldColumn.getScale().map(Object::toString).orElse("none"),
                                    newColumn.getScale().map(Object::toString).orElse("none")));
                    break;
                case COMMENT_CHANGED:
                    sb.append(
                            String.format(
                                    "comment: %s -> %s, ",
                                    oldColumn.getComment().orElse("none"),
                                    newColumn.getComment().orElse("none")));
                    break;
                case CHARACTER_SET_CHANGED:
                    sb.append(
                            String.format(
                                    "characterSet: %s -> %s, ",
                                    oldColumn.getCharacterSet().orElse("none"),
                                    newColumn.getCharacterSet().orElse("none")));
                    break;
                case COLLATION_CHANGED:
                    sb.append(
                            String.format(
                                    "collation: %s -> %s, ",
                                    oldColumn.getCollation().orElse("none"),
                                    newColumn.getCollation().orElse("none")));
                    break;
            }
        }

        return sb.substring(0, sb.length() - 2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDiff that = (ColumnDiff) o;
        return oldColumn.equals(that.oldColumn)
                && newColumn.equals(that.newColumn)
                && changes.equals(that.changes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldColumn, newColumn, changes);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /** Builder for creating ColumnDiff instances. */
    public static class Builder {
        private final Column oldColumn;
        private final Column newColumn;
        private final java.util.Set<ChangeType> changes = EnumSet.noneOf(ChangeType.class);

        /**
         * Creates a new Builder.
         *
         * @param oldColumn the original column
         * @param newColumn the new column
         */
        public Builder(Column oldColumn, Column newColumn) {
            this.oldColumn = oldColumn;
            this.newColumn = newColumn;
        }

        /**
         * Adds a change type.
         *
         * @param changeType the change type
         * @return this Builder for chaining
         */
        public Builder addChange(ChangeType changeType) {
            changes.add(changeType);
            return this;
        }

        /**
         * Builds the ColumnDiff.
         *
         * @return a new ColumnDiff
         */
        public ColumnDiff build() {
            return new ColumnDiff(oldColumn, newColumn, changes);
        }
    }
}
