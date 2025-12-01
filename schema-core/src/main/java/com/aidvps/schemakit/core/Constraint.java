package com.aidvps.schemakit.core;

import java.util.*;

/**
 * Represents a table constraint (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK). Immutable object with
 * nested classes for each constraint type.
 */
public abstract class Constraint {
    protected final String name;

    protected Constraint(String name) {
        this.name = name;
    }

    /**
     * Get the constraint name.
     *
     * @return Constraint name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the constraint type.
     *
     * @return Type of constraint
     */
    public abstract ConstraintType getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return getType() + " constraint '" + name + "'";
    }

    /** Constraint type enum. */
    public enum ConstraintType {
        PRIMARY_KEY,
        FOREIGN_KEY,
        UNIQUE,
        CHECK
    }

    /** PRIMARY KEY constraint. */
    public static class PrimaryKey extends Constraint {
        private final List<String> columns;

        private PrimaryKey(Builder builder) {
            super(builder.name);
            this.columns =
                    builder.columns != null
                            ? Collections.unmodifiableList(new ArrayList<>(builder.columns))
                            : Collections.emptyList();
        }

        /**
         * Get the columns in this primary key.
         *
         * @return List of column names
         */
        public List<String> getColumns() {
            return columns;
        }

        @Override
        public ConstraintType getType() {
            return ConstraintType.PRIMARY_KEY;
        }

        @Override
        public String toString() {
            return "PRIMARY KEY (" + String.join(", ", columns) + ")";
        }

        /** Builder for PrimaryKey constraint. */
        public static class Builder {
            private String name;
            private List<String> columns;

            /**
             * Set the constraint name.
             *
             * @param name Constraint name
             * @return this builder
             */
            public Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Add a column to the primary key.
             *
             * @param column Column name
             * @return this builder
             */
            public Builder column(String column) {
                if (columns == null) {
                    columns = new ArrayList<>();
                }
                columns.add(column);
                return this;
            }

            /**
             * Build the PrimaryKey constraint.
             *
             * @return PrimaryKey constraint
             */
            public PrimaryKey build() {
                if (name == null) {
                    throw new IllegalStateException("Constraint name must be set");
                }
                if (columns == null || columns.isEmpty()) {
                    throw new IllegalStateException("At least one column must be set");
                }
                return new PrimaryKey(this);
            }
        }
    }

    /** FOREIGN KEY constraint. */
    public static class ForeignKey extends Constraint {
        private final List<String> columns;
        private final String referencedTable;
        private final List<String> referencedColumns;
        private final String onDelete;
        private final String onUpdate;

        private ForeignKey(Builder builder) {
            super(builder.name);
            this.columns =
                    builder.columns != null
                            ? Collections.unmodifiableList(new ArrayList<>(builder.columns))
                            : Collections.emptyList();
            this.referencedTable = builder.referencedTable;
            this.referencedColumns =
                    builder.referencedColumns != null
                            ? Collections.unmodifiableList(
                                    new ArrayList<>(builder.referencedColumns))
                            : Collections.emptyList();
            this.onDelete = builder.onDelete;
            this.onUpdate = builder.onUpdate;
        }

        /**
         * Get the columns in this foreign key.
         *
         * @return List of column names
         */
        public List<String> getColumns() {
            return columns;
        }

        /**
         * Get the referenced table name.
         *
         * @return Referenced table
         */
        public String getReferencedTable() {
            return referencedTable;
        }

        /**
         * Get the referenced columns.
         *
         * @return List of referenced column names
         */
        public List<String> getReferencedColumns() {
            return referencedColumns;
        }

        /**
         * Get the ON DELETE action.
         *
         * @return ON DELETE action (may be null)
         */
        public String getOnDelete() {
            return onDelete;
        }

        /**
         * Get the ON UPDATE action.
         *
         * @return ON UPDATE action (may be null)
         */
        public String getOnUpdate() {
            return onUpdate;
        }

        @Override
        public ConstraintType getType() {
            return ConstraintType.FOREIGN_KEY;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FOREIGN KEY (").append(String.join(", ", columns)).append(")");
            if (referencedTable != null) {
                sb.append(" REFERENCES ").append(referencedTable);
                if (!referencedColumns.isEmpty()) {
                    sb.append(" (").append(String.join(", ", referencedColumns)).append(")");
                }
            }
            return sb.toString();
        }

        /** Builder for ForeignKey constraint. */
        public static class Builder {
            private String name;
            private List<String> columns;
            private String referencedTable;
            private List<String> referencedColumns;
            private String onDelete;
            private String onUpdate;

            /**
             * Set the constraint name.
             *
             * @param name Constraint name
             * @return this builder
             */
            public Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Add a column to the foreign key.
             *
             * @param column Column name
             * @return this builder
             */
            public Builder column(String column) {
                if (columns == null) {
                    columns = new ArrayList<>();
                }
                columns.add(column);
                return this;
            }

            /**
             * Set the referenced table.
             *
             * @param table Referenced table name
             * @return this builder
             */
            public Builder referencedTable(String table) {
                this.referencedTable = table;
                return this;
            }

            /**
             * Add a referenced column.
             *
             * @param column Referenced column name
             * @return this builder
             */
            public Builder referencedColumn(String column) {
                if (referencedColumns == null) {
                    referencedColumns = new ArrayList<>();
                }
                referencedColumns.add(column);
                return this;
            }

            /**
             * Set the ON DELETE action.
             *
             * @param action ON DELETE action (e.g., CASCADE, SET NULL)
             * @return this builder
             */
            public Builder onDelete(String action) {
                this.onDelete = action;
                return this;
            }

            /**
             * Set the ON UPDATE action.
             *
             * @param action ON UPDATE action (e.g., CASCADE, SET NULL)
             * @return this builder
             */
            public Builder onUpdate(String action) {
                this.onUpdate = action;
                return this;
            }

            /**
             * Build the ForeignKey constraint.
             *
             * @return ForeignKey constraint
             */
            public ForeignKey build() {
                if (name == null) {
                    throw new IllegalStateException("Constraint name must be set");
                }
                return new ForeignKey(this);
            }
        }
    }

    /** UNIQUE constraint. */
    public static class Unique extends Constraint {
        private final List<String> columns;

        private Unique(Builder builder) {
            super(builder.name);
            this.columns =
                    builder.columns != null
                            ? Collections.unmodifiableList(new ArrayList<>(builder.columns))
                            : Collections.emptyList();
        }

        /**
         * Get the columns in this unique constraint.
         *
         * @return List of column names
         */
        public List<String> getColumns() {
            return columns;
        }

        @Override
        public ConstraintType getType() {
            return ConstraintType.UNIQUE;
        }

        @Override
        public String toString() {
            return "UNIQUE (" + String.join(", ", columns) + ")";
        }

        /** Builder for Unique constraint. */
        public static class Builder {
            private String name;
            private List<String> columns;

            /**
             * Set the constraint name.
             *
             * @param name Constraint name
             * @return this builder
             */
            public Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Add a column to the unique constraint.
             *
             * @param column Column name
             * @return this builder
             */
            public Builder column(String column) {
                if (columns == null) {
                    columns = new ArrayList<>();
                }
                columns.add(column);
                return this;
            }

            /**
             * Build the Unique constraint.
             *
             * @return Unique constraint
             */
            public Unique build() {
                if (name == null) {
                    throw new IllegalStateException("Constraint name must be set");
                }
                if (columns == null || columns.isEmpty()) {
                    throw new IllegalStateException("At least one column must be set");
                }
                return new Unique(this);
            }
        }
    }

    /** CHECK constraint. */
    public static class Check extends Constraint {
        private final String expression;

        private Check(Builder builder) {
            super(builder.name);
            this.expression = builder.expression;
        }

        /**
         * Get the check expression.
         *
         * @return Check expression (SQL)
         */
        public String getExpression() {
            return expression;
        }

        @Override
        public ConstraintType getType() {
            return ConstraintType.CHECK;
        }

        @Override
        public String toString() {
            return "CHECK (" + expression + ")";
        }

        /** Builder for Check constraint. */
        public static class Builder {
            private String name;
            private String expression;

            /**
             * Set the constraint name.
             *
             * @param name Constraint name
             * @return this builder
             */
            public Builder name(String name) {
                this.name = name;
                return this;
            }

            /**
             * Set the check expression.
             *
             * @param expression SQL check expression
             * @return this builder
             */
            public Builder expression(String expression) {
                this.expression = expression;
                return this;
            }

            /**
             * Build the Check constraint.
             *
             * @return Check constraint
             */
            public Check build() {
                if (name == null) {
                    throw new IllegalStateException("Constraint name must be set");
                }
                if (expression == null) {
                    throw new IllegalStateException("Expression must be set");
                }
                return new Check(this);
            }
        }
    }
}
