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

import java.util.Objects;

/**
 * Represents a potentially destructive database operation.
 *
 * <p>Destructive operations are those that may result in data loss or irreversible changes to the
 * database structure. Examples include DROP TABLE, DROP COLUMN, or ALTER TABLE ... DROP PARTITION.
 */
public final class DestructiveOperation {

    private final Type operationType;
    private final String targetObject;
    private final String sqlStatement;
    private final Severity severity;
    private final boolean dataLossRisk;
    private final Long estimatedRowsAffected;

    /** Enumeration of destructive operation types. */
    public enum Type {
        DROP_TABLE,
        DROP_COLUMN,
        DROP_CONSTRAINT,
        DROP_INDEX,
        ALTER_TABLE_DROP_PARTITION,
        TRUNCATE_TABLE,
        DELETE_FROM_TABLE
    }

    /** Enumeration of severity levels. */
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Creates a new DestructiveOperation.
     *
     * @param operationType the type of operation
     * @param targetObject the object being affected (e.g., table name, column name)
     * @param sqlStatement the SQL statement that will be executed
     * @param severity the severity level
     * @param dataLossRisk whether this operation may cause data loss
     * @param estimatedRowsAffected estimated number of rows affected (may be null)
     */
    public DestructiveOperation(
            Type operationType,
            String targetObject,
            String sqlStatement,
            Severity severity,
            boolean dataLossRisk,
            Long estimatedRowsAffected) {
        this.operationType = Objects.requireNonNull(operationType, "Operation type cannot be null");
        this.targetObject = Objects.requireNonNull(targetObject, "Target object cannot be null");
        this.sqlStatement = Objects.requireNonNull(sqlStatement, "SQL statement cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.dataLossRisk = dataLossRisk;
        this.estimatedRowsAffected = estimatedRowsAffected;
    }

    /**
     * Creates a new DestructiveOperation with high data loss risk.
     *
     * @param operationType the type of operation
     * @param targetObject the object being affected
     * @param sqlStatement the SQL statement
     * @param severity the severity level
     */
    public DestructiveOperation(
            Type operationType, String targetObject, String sqlStatement, Severity severity) {
        this(operationType, targetObject, sqlStatement, severity, true, null);
    }

    /**
     * Returns the operation type.
     *
     * @return the type (never null)
     */
    public Type getOperationType() {
        return operationType;
    }

    /**
     * Returns the target object.
     *
     * @return the target object (never null)
     */
    public String getTargetObject() {
        return targetObject;
    }

    /**
     * Returns the SQL statement.
     *
     * @return the SQL statement (never null)
     */
    public String getSqlStatement() {
        return sqlStatement;
    }

    /**
     * Returns the severity level.
     *
     * @return the severity (never null)
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns whether this operation may cause data loss.
     *
     * @return true if there's data loss risk, false otherwise
     */
    public boolean isDataLossRisk() {
        return dataLossRisk;
    }

    /**
     * Returns the estimated number of rows affected.
     *
     * @return an Optional containing the estimate, or empty if not available
     */
    public java.util.Optional<Long> getEstimatedRowsAffected() {
        return java.util.Optional.ofNullable(estimatedRowsAffected);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DestructiveOperation that = (DestructiveOperation) o;
        return operationType == that.operationType
                && Objects.equals(targetObject, that.targetObject)
                && Objects.equals(sqlStatement, that.sqlStatement)
                && severity == that.severity
                && dataLossRisk == that.dataLossRisk
                && Objects.equals(estimatedRowsAffected, that.estimatedRowsAffected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                operationType,
                targetObject,
                sqlStatement,
                severity,
                dataLossRisk,
                estimatedRowsAffected);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(operationType).append(": ").append(targetObject);
        if (severity != Severity.LOW) {
            sb.append(" [").append(severity).append("]");
        }
        if (dataLossRisk) {
            sb.append(" (DATA LOSS RISK)");
        }
        return sb.toString();
    }
}
