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
import java.util.Optional;

/**
 * Immutable representation of a database column.
 *
 * <p>A Column represents an individual field within a database table, including its data type,
 * constraints, and properties. Once created, a Column cannot be modified.
 */
public final class Column {
    private final String name;
    private final String dataType;
    private final Integer length;
    private final Integer precision;
    private final Integer scale;
    private final boolean nullable;
    private final String defaultValue;
    private final boolean autoIncrement;
    private final String comment;
    private final String characterSet;
    private final String collation;

    private Column(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Column name cannot be null");
        this.dataType = Objects.requireNonNull(builder.dataType, "Column data type cannot be null");
        this.length = builder.length;
        this.precision = builder.precision;
        this.scale = builder.scale;
        this.nullable = builder.nullable;
        this.defaultValue = builder.defaultValue;
        this.autoIncrement = builder.autoIncrement;
        this.comment = builder.comment;
        this.characterSet = builder.characterSet;
        this.collation = builder.collation;
    }

    /**
     * Creates a new Builder for Column.
     *
     * @param name the column name (required)
     * @param dataType the column data type (required)
     * @return a new Builder instance
     */
    public static Builder builder(String name, String dataType) {
        return new Builder(name, dataType);
    }

    /**
     * Returns the column name.
     *
     * @return the column name (never null)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the column data type.
     *
     * @return the data type (never null)
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns the column length if specified.
     *
     * @return an Optional containing the length, or empty if not specified
     */
    public Optional<Integer> getLength() {
        return Optional.ofNullable(length);
    }

    /**
     * Returns the column precision if specified.
     *
     * @return an Optional containing the precision, or empty if not specified
     */
    public Optional<Integer> getPrecision() {
        return Optional.ofNullable(precision);
    }

    /**
     * Returns the column scale if specified.
     *
     * @return an Optional containing the scale, or empty if not specified
     */
    public Optional<Integer> getScale() {
        return Optional.ofNullable(scale);
    }

    /**
     * Returns whether the column allows NULL values.
     *
     * @return true if the column is nullable, false if NOT NULL
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Returns the default value if specified.
     *
     * @return an Optional containing the default value, or empty if not specified
     */
    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    /**
     * Returns whether the column has auto-increment enabled.
     *
     * @return true if auto-increment is enabled, false otherwise
     */
    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    /**
     * Returns the comment if specified.
     *
     * @return an Optional containing the comment, or empty if not specified
     */
    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    /**
     * Returns the character set if specified.
     *
     * @return an Optional containing the character set, or empty if not specified
     */
    public Optional<String> getCharacterSet() {
        return Optional.ofNullable(characterSet);
    }

    /**
     * Returns the collation if specified.
     *
     * @return an Optional containing the collation, or empty if not specified
     */
    public Optional<String> getCollation() {
        return Optional.ofNullable(collation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return nullable == column.nullable
                && autoIncrement == column.autoIncrement
                && Objects.equals(name, column.name)
                && Objects.equals(dataType, column.dataType)
                && Objects.equals(length, column.length)
                && Objects.equals(precision, column.precision)
                && Objects.equals(scale, column.scale)
                && Objects.equals(defaultValue, column.defaultValue)
                && Objects.equals(comment, column.comment)
                && Objects.equals(characterSet, column.characterSet)
                && Objects.equals(collation, column.collation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                dataType,
                length,
                precision,
                scale,
                nullable,
                defaultValue,
                autoIncrement,
                comment,
                characterSet,
                collation);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(' ').append(dataType);
        if (length != null) {
            sb.append('(').append(length);
            if (scale != null) {
                sb.append(',').append(scale);
            }
            sb.append(')');
        } else if (precision != null && scale != null) {
            sb.append('(').append(precision).append(',').append(scale).append(')');
        }
        if (!nullable) {
            sb.append(" NOT NULL");
        }
        if (autoIncrement) {
            sb.append(" AUTO_INCREMENT");
        }
        if (defaultValue != null) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        if (comment != null) {
            sb.append(" COMMENT '").append(comment).append('\'');
        }
        return sb.toString();
    }

    /** Builder for creating Column instances. */
    public static final class Builder {
        private final String name;
        private final String dataType;
        private Integer length;
        private Integer precision;
        private Integer scale;
        private boolean nullable = true;
        private String defaultValue;
        private boolean autoIncrement = false;
        private String comment;
        private String characterSet;
        private String collation;

        private Builder(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        /**
         * Sets the length for variable-length types (e.g., VARCHAR(255)).
         *
         * @param length the column length
         * @return this Builder for chaining
         */
        public Builder length(int length) {
            this.length = length;
            return this;
        }

        /**
         * Sets the precision and scale for decimal types (e.g., DECIMAL(10,2)).
         *
         * @param precision the precision (total number of digits)
         * @param scale the scale (digits after decimal point)
         * @return this Builder for chaining
         */
        public Builder precision(int precision, int scale) {
            this.precision = precision;
            this.scale = scale;
            return this;
        }

        /**
         * Sets whether the column allows NULL values.
         *
         * @param nullable true if nullable, false for NOT NULL
         * @return this Builder for chaining
         */
        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        /**
         * Sets the default value for the column.
         *
         * @param defaultValue the default value (can be SQL expression)
         * @return this Builder for chaining
         */
        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Sets whether the column has auto-increment enabled.
         *
         * @param autoIncrement true for auto-increment
         * @return this Builder for chaining
         */
        public Builder autoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
            return this;
        }

        /**
         * Sets the column comment.
         *
         * @param comment the comment text
         * @return this Builder for chaining
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Sets the character set for string types.
         *
         * @param characterSet the character set name
         * @return this Builder for chaining
         */
        public Builder characterSet(String characterSet) {
            this.characterSet = characterSet;
            return this;
        }

        /**
         * Sets the collation for string types.
         *
         * @param collation the collation name
         * @return this Builder for chaining
         */
        public Builder collation(String collation) {
            this.collation = collation;
            return this;
        }

        /**
         * Builds the Column instance.
         *
         * @return a new immutable Column
         */
        public Column build() {
            return new Column(this);
        }
    }
}
