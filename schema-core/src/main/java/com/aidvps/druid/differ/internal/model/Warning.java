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
 * Represents a warning about a migration.
 *
 * <p>Warnings notify users about potential issues or considerations that should be reviewed before
 * executing a migration, without preventing the migration from being generated.
 */
public final class Warning {

    private final Type type;
    private final String message;
    private final Severity severity;
    private final Integer statementIndex;

    /** Enumeration of warning types. */
    public enum Type {
        DEPRECATED_SYNTAX,
        DATA_LOSS_RISK,
        PERFORMANCE_NOTE,
        COMPATIBILITY_WARNING,
        DEPENDENCY_NOTE
    }

    /** Enumeration of severity levels. */
    public enum Severity {
        INFO,
        WARN,
        ERROR
    }

    /**
     * Creates a new Warning.
     *
     * @param type the warning type
     * @param message the warning message
     * @param severity the severity level
     * @param statementIndex the index of the related statement (may be null)
     */
    public Warning(Type type, String message, Severity severity, Integer statementIndex) {
        this.type = Objects.requireNonNull(type, "Warning type cannot be null");
        this.message = Objects.requireNonNull(message, "Warning message cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.statementIndex = statementIndex;
    }

    /**
     * Creates a new Warning without a statement index.
     *
     * @param type the warning type
     * @param message the warning message
     * @param severity the severity level
     */
    public Warning(Type type, String message, Severity severity) {
        this(type, message, severity, null);
    }

    /**
     * Returns the warning type.
     *
     * @return the type (never null)
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the warning message.
     *
     * @return the message (never null)
     */
    public String getMessage() {
        return message;
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
     * Returns the statement index.
     *
     * @return an Optional containing the statement index, or empty if not specified
     */
    public java.util.Optional<Integer> getStatementIndex() {
        return java.util.Optional.ofNullable(statementIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warning warning = (Warning) o;
        return type == warning.type
                && Objects.equals(message, warning.message)
                && severity == warning.severity
                && Objects.equals(statementIndex, warning.statementIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, message, severity, statementIndex);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        if (type != null) {
            sb.append("(").append(type).append(") ");
        }
        sb.append(message);
        if (statementIndex != null) {
            sb.append(" (statement ").append(statementIndex).append(")");
        }
        return sb.toString();
    }
}
