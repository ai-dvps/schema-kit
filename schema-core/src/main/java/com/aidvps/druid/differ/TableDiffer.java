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

import com.aidvps.druid.differ.exception.GenerationException;
import com.aidvps.druid.differ.exception.SchemaCompatibilityException;
import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.comparator.ChangeDetector;
import com.aidvps.druid.differ.internal.generator.MySQLMigrationGenerator;
import com.aidvps.druid.differ.internal.generator.RollbackGenerator;
import com.aidvps.druid.differ.internal.model.DestructiveOperation;
import com.aidvps.druid.differ.internal.model.MigrationPlan;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.model.Warning;
import com.aidvps.druid.differ.internal.parser.DatabaseDialectResolver;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import com.aidvps.druid.differ.internal.validator.SchemaValidator;
import java.util.List;

/**
 * Main facade class for the SQL Table Differ.
 *
 * <p>This class provides a simple, high-level API for comparing database schemas and generating
 * migration SQL. It orchestrates the parsing, comparison, and generation components to provide a
 * seamless experience.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Usage</h3>
 *
 * <pre>
 * // Create a TableDiffer with MySQL dialect
 * TableDiffer differ = TableDiffer.builder()
 *     .withDialect(DatabaseDialect.MYSQL)
 *     .build();
 *
 * // Generate migration SQL
 * String sourceSchema = "CREATE TABLE users (id INT, name VARCHAR(100))";
 * String targetSchema = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(200), email VARCHAR(255))";
 * MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
 *
 * // Print the migration statements
 * for (String statement : plan.getStatements()) {
 *     System.out.println(statement);
 * }
 * </pre>
 *
 * <h3>With Custom Options</h3>
 *
 * <pre>
 * // Configure migration options
 * MigrationOptions options = MigrationOptions.builder()
 *     .wrapInTransaction(true)
 *     .includeComments(true)
 *     .failOnDestructive(true)
 *     .build();
 *
 * TableDiffer differ = TableDiffer.builder()
 *     .withDialect(DatabaseDialect.POSTGRESQL)
 *     .withOptions(options)
 *     .withValidationLevel(ValidationLevel.STRICT)
 *     .build();
 *
 * MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
 * </pre>
 *
 * <h3>Generating Rollback SQL</h3>
 *
 * <pre>
 * TableDiffer differ = TableDiffer.builder()
 *     .withDialect(DatabaseDialect.MYSQL)
 *     .build();
 *
 * MigrationPlan plan = differ.generateMigration(sourceSchema, targetSchema);
 * List{@literal <String>} rollbackStatements = differ.generateRollback(plan);
 * </pre>
 *
 * <h3>Multi-Dialect Support</h3>
 *
 * <pre>
 * // MySQL
 * TableDiffer mysqlDiffer = TableDiffer.builder()
 *     .withDialect(DatabaseDialect.MYSQL)
 *     .build();
 *
 * // PostgreSQL
 * TableDiffer pgDiffer = TableDiffer.builder()
 *     .withDialect(DatabaseDialect.POSTGRESQL)
 *     .build();
 *
 * // Oracle
 * TableDiffer oracleDiffer = TableDiffer.builder()
 *     .withDialect(DatabaseDialect.ORACLE)
 *     .build();
 * </pre>
 */
public final class TableDiffer {

    private final DatabaseDialect dialect;
    private final MigrationOptions options;
    private final ValidationLevel validationLevel;
    private final DruidParserAdapter parserAdapter;
    private final ChangeDetector changeDetector;
    private final MySQLMigrationGenerator migrationGenerator;
    private final SchemaValidator schemaValidator;

    /**
     * Creates a new TableDiffer with the specified dialect.
     *
     * @param dialect the database dialect
     * @param options the migration options
     * @param validationLevel the validation level
     */
    private TableDiffer(
            DatabaseDialect dialect, MigrationOptions options, ValidationLevel validationLevel) {
        this.dialect = dialect;
        this.options = options;
        this.validationLevel = validationLevel;
        this.parserAdapter = new DruidParserAdapter(DatabaseDialectResolver.resolveDbType(dialect));
        this.changeDetector = new ChangeDetector();
        this.migrationGenerator = new MySQLMigrationGenerator(options.isIncludeComments());
        this.schemaValidator = new SchemaValidator();
    }

    /**
     * Generates a migration plan with default options.
     *
     * @param sourceSchema the source database schema
     * @param targetSchema the target database schema
     * @return a MigrationPlan
     * @throws SchemaParsingException if schemas cannot be parsed
     * @throws SchemaCompatibilityException if schemas are incompatible
     * @throws GenerationException if generation fails
     */
    public MigrationPlan generateMigration(String sourceSchema, String targetSchema)
            throws SchemaParsingException, SchemaCompatibilityException, GenerationException {
        return generateMigration(sourceSchema, targetSchema, MigrationOptions.defaults());
    }

    /**
     * Generates rollback SQL for a migration plan.
     *
     * @param migrationPlan the migration plan to generate rollback for
     * @return a list of rollback SQL statements
     * @throws GenerationException if rollback generation fails
     */
    public List<String> generateRollback(MigrationPlan migrationPlan) throws GenerationException {
        return generateRollback(migrationPlan, MigrationOptions.defaults());
    }

    /**
     * Generates rollback SQL for a migration plan with custom options.
     *
     * @param migrationPlan the migration plan to generate rollback for
     * @param options the migration options
     * @return a list of rollback SQL statements
     * @throws GenerationException if rollback generation fails
     */
    public List<String> generateRollback(MigrationPlan migrationPlan, MigrationOptions options)
            throws GenerationException {
        if (options.isDryRun()) {
            // In dry run mode, just validate without generating
            return java.util.Collections.emptyList();
        }

        if (!options.isIncludeRollback()) {
            // Rollback not requested
            return java.util.Collections.emptyList();
        }

        // Detect the schema diff from source to target
        SchemaDiff diff =
                changeDetector.compare(
                        migrationPlan.getTargetSchema(), migrationPlan.getSourceSchema());

        // Generate rollback statements using RollbackGenerator
        RollbackGenerator rollbackGenerator = new RollbackGenerator(options.isIncludeComments());
        return rollbackGenerator.generateRollback(diff);
    }

    /**
     * Generates a migration plan with custom options.
     *
     * @param sourceSchema the source database schema
     * @param targetSchema the target database schema
     * @param options the migration options
     * @return a MigrationPlan
     * @throws SchemaParsingException if schemas cannot be parsed
     * @throws SchemaCompatibilityException if schemas are incompatible
     * @throws GenerationException if generation fails
     */
    public MigrationPlan generateMigration(
            String sourceSchema, String targetSchema, MigrationOptions options)
            throws SchemaParsingException, SchemaCompatibilityException, GenerationException {
        // Parse schemas
        Schema source = parserAdapter.parseSchema(sourceSchema);
        Schema target = parserAdapter.parseSchema(targetSchema);

        // Detect changes
        SchemaDiff diff = changeDetector.compare(source, target);

        // Validate target schema and generate warnings
        List<Warning> warnings = schemaValidator.validate(target);

        // Add warnings from diff analysis
        warnings.addAll(generateWarnings(diff));

        // Check if schemas are identical
        if (diff.isEmpty()) {
            return createEmptyMigrationPlan(source, target);
        }

        // Enforce validation level
        enforceValidationLevel(warnings, diff);

        // Generate SQL statements based on dialect
        List<String> statements = migrationGenerator.generate(diff);

        return new MigrationPlan.Builder()
                .sourceSchema(source)
                .targetSchema(target)
                .databaseDialect(dialect)
                .addStatements(statements)
                .addWarnings(warnings)
                .build();
    }

    /** Enforces the validation level by evaluating warnings and potentially throwing exceptions. */
    private void enforceValidationLevel(List<Warning> warnings, SchemaDiff diff)
            throws SchemaCompatibilityException {
        switch (validationLevel) {
            case STRICT:
                // In strict mode, treat warnings as errors
                for (Warning warning : warnings) {
                    if (warning.getSeverity() != Warning.Severity.INFO) {
                        throw new SchemaCompatibilityException(
                                "Strict validation failed: " + warning.getMessage());
                    }
                }
                // Also check for destructive operations
                if (!diff.getRemovedTables().isEmpty() || hasRemovedColumns(diff)) {
                    throw new SchemaCompatibilityException(
                            "Strict validation failed: Destructive operations detected. "
                                    + "Please review and confirm these changes manually.");
                }
                break;

            case STANDARD:
                // Standard mode: generate warnings for destructive operations
                // Warnings are already generated in generateWarnings() method
                break;

            case LENIENT:
                // Lenient mode: downgrade WARN level warnings to INFO level
                // Replace WARN severity warnings with INFO severity warnings
                for (int i = 0; i < warnings.size(); i++) {
                    Warning warning = warnings.get(i);
                    if (warning.getSeverity() == Warning.Severity.WARN) {
                        warnings.set(
                                i,
                                new Warning(
                                        warning.getType(),
                                        warning.getMessage(),
                                        Warning.Severity.INFO,
                                        warning.getStatementIndex().orElse(null)));
                    }
                }
                break;
        }
    }

    /** Checks if any table has removed columns. */
    private boolean hasRemovedColumns(SchemaDiff diff) {
        for (com.aidvps.druid.differ.internal.model.TableDiff tableDiff :
                diff.getModifiedTables().values()) {
            if (!tableDiff.getRemovedColumns().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Creates an empty migration plan for schemas with no differences. */
    private MigrationPlan createEmptyMigrationPlan(Schema source, Schema target) {
        return new MigrationPlan.Builder()
                .sourceSchema(source)
                .targetSchema(target)
                .databaseDialect(dialect)
                .build();
    }

    /** Generates warnings based on schema differences. */
    private java.util.List<Warning> generateWarnings(SchemaDiff diff) {
        java.util.List<Warning> warnings = new java.util.ArrayList<>();

        if (!diff.getAddedTables().isEmpty()) {
            warnings.add(
                    new Warning(
                            Warning.Type.COMPATIBILITY_WARNING,
                            "Adding "
                                    + diff.getAddedTables().size()
                                    + " new table(s). Ensure application code is updated.",
                            Warning.Severity.WARN));
        }

        if (!diff.getRemovedTables().isEmpty()) {
            warnings.add(
                    new Warning(
                            Warning.Type.DATA_LOSS_RISK,
                            "Removing "
                                    + diff.getRemovedTables().size()
                                    + " table(s). This will delete all data.",
                            Warning.Severity.ERROR));
        }

        for (com.aidvps.druid.differ.internal.model.TableDiff tableDiff :
                diff.getModifiedTables().values()) {
            if (!tableDiff.getRemovedColumns().isEmpty()) {
                warnings.add(
                        new Warning(
                                Warning.Type.DATA_LOSS_RISK,
                                "Table "
                                        + tableDiff.getTableName()
                                        + " has "
                                        + tableDiff.getRemovedColumns().size()
                                        + " column(s) removed. This will delete column data.",
                                Warning.Severity.ERROR));
            }

            for (com.aidvps.druid.differ.internal.model.ColumnDiff columnDiff :
                    tableDiff.getModifiedColumns().values()) {
                if (columnDiff.hasChange(
                        com.aidvps.druid.differ.internal.model.ColumnDiff.ChangeType
                                .DATA_TYPE_CHANGED)) {
                    warnings.add(
                            new Warning(
                                    Warning.Type.COMPATIBILITY_WARNING,
                                    "Column "
                                            + columnDiff.getNewColumn().getName()
                                            + " in table "
                                            + tableDiff.getTableName()
                                            + " has changed data type. Verify data compatibility.",
                                    Warning.Severity.WARN));
                }
            }
        }

        return warnings;
    }

    /** Detects destructive operations in the schema differences. */
    private java.util.List<DestructiveOperation> detectDestructiveOperations(SchemaDiff diff) {
        java.util.List<DestructiveOperation> operations = new java.util.ArrayList<>();

        for (Table table : diff.getRemovedTables().values()) {
            operations.add(
                    new DestructiveOperation(
                            DestructiveOperation.Type.DROP_TABLE,
                            table.getName(),
                            "DROP TABLE " + table.getName() + ";",
                            DestructiveOperation.Severity.HIGH,
                            true,
                            null));
        }

        for (com.aidvps.druid.differ.internal.model.TableDiff tableDiff :
                diff.getModifiedTables().values()) {
            for (String columnName : tableDiff.getRemovedColumns()) {
                operations.add(
                        new DestructiveOperation(
                                DestructiveOperation.Type.DROP_COLUMN,
                                tableDiff.getTableName() + "." + columnName,
                                "ALTER TABLE "
                                        + tableDiff.getTableName()
                                        + " DROP COLUMN "
                                        + columnName
                                        + ";",
                                DestructiveOperation.Severity.HIGH,
                                true,
                                null));
            }
        }

        return operations;
    }

    /** Builder for creating TableDiffer instances. */
    public static class Builder {
        private DatabaseDialect dialect = DatabaseDialect.MYSQL;
        private MigrationOptions options = MigrationOptions.defaults();
        private ValidationLevel validationLevel = ValidationLevel.STANDARD;

        /**
         * Sets the database dialect.
         *
         * @param dialect the database dialect
         * @return this Builder for chaining
         */
        public Builder withDialect(DatabaseDialect dialect) {
            this.dialect = dialect;
            return this;
        }

        /**
         * Sets the migration options.
         *
         * @param options the migration options
         * @return this Builder for chaining
         */
        public Builder withOptions(MigrationOptions options) {
            this.options = options;
            return this;
        }

        /**
         * Sets the validation level.
         *
         * @param validationLevel the validation level
         * @return this Builder for chaining
         */
        public Builder withValidationLevel(ValidationLevel validationLevel) {
            this.validationLevel = validationLevel;
            return this;
        }

        /**
         * Builds the TableDiffer instance.
         *
         * @return a new TableDiffer
         */
        public TableDiffer build() {
            return new TableDiffer(dialect, options, validationLevel);
        }
    }

    /**
     * Creates a new Builder for TableDiffer.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
