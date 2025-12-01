package com.aidvps.schemakit.core;

import java.util.*;

/** Represents a column in a table. Immutable object with builder pattern for construction. */
public final class Column {
    private final String name;
    private final DataType type;
    private final boolean nullable;
    private final Object defaultValue;
    private final List<String> annotations;

    private Column(Builder builder) {
        this.name = validateName(builder.name);
        this.type = Objects.requireNonNull(builder.type, "Column type cannot be null");
        this.nullable = builder.nullable;
        this.defaultValue = builder.defaultValue;
        this.annotations =
                builder.annotations != null
                        ? Collections.unmodifiableList(new ArrayList<>(builder.annotations))
                        : Collections.emptyList();
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        return name;
    }

    /**
     * Get the column name.
     *
     * @return Column name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the column data type.
     *
     * @return Data type
     */
    public DataType getType() {
        return type;
    }

    /**
     * Check if column accepts NULL values.
     *
     * @return true if nullable
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Get the default value.
     *
     * @return Default value (may be null)
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get column annotations.
     *
     * @return List of annotations
     */
    public List<String> getAnnotations() {
        return annotations;
    }

    /**
     * Check if column has a default value.
     *
     * @return true if default value is set
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Check if column has the given annotation.
     *
     * @param annotation Annotation to check
     * @return true if annotation exists
     */
    public boolean hasAnnotation(String annotation) {
        return annotations.contains(annotation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return nullable == column.nullable
                && Objects.equals(name, column.name)
                && Objects.equals(type, column.type)
                && Objects.equals(defaultValue, column.defaultValue)
                && Objects.equals(annotations, column.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, nullable, defaultValue, annotations);
    }

    @Override
    public String toString() {
        return "Column{"
                + "name='"
                + name
                + '\''
                + ", type="
                + type
                + ", nullable="
                + nullable
                + '}';
    }

    /**
     * Create a new builder for Column.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for Column class. */
    public static class Builder {
        private String name;
        private DataType type;
        private boolean nullable = false;
        private Object defaultValue;
        private List<String> annotations;

        /**
         * Set the column name.
         *
         * @param name Column name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the column data type.
         *
         * @param type Data type
         * @return this builder
         */
        public Builder type(DataType type) {
            this.type = type;
            return this;
        }

        /**
         * Set whether the column is nullable.
         *
         * @param nullable true if nullable
         * @return this builder
         */
        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        /**
         * Set the default value.
         *
         * @param defaultValue Default value (may be null)
         * @return this builder
         */
        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Add an annotation.
         *
         * @param annotation Annotation
         * @return this builder
         */
        public Builder annotation(String annotation) {
            if (annotations == null) {
                annotations = new ArrayList<>();
            }
            annotations.add(annotation);
            return this;
        }

        /**
         * Build the Column instance.
         *
         * @return Column instance
         */
        public Column build() {
            if (name == null) {
                throw new IllegalStateException("Column name must be set");
            }
            if (type == null) {
                throw new IllegalStateException("Column type must be set");
            }
            return new Column(this);
        }
    }
}
