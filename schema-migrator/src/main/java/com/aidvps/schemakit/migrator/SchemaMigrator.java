package com.aidvps.schemakit.migrator;

import com.aidvps.druid.differ.internal.model.Schema;

/** Core interface for schema migration. */
public interface SchemaMigrator {
    /**
     * Generate migration script from source to target schema.
     *
     * @param source Source schema
     * @param target Target schema
     * @param config Migration configuration
     * @return Migration script with SQL statements
     * @throws MigrationException if generation fails
     */
    MigrationScript generateMigration(Schema source, Schema target, MigrationConfig config)
            throws MigrationException;

    /**
     * Compare two schemas and return differences.
     *
     * @param source Source schema
     * @param target Target schema
     * @return SchemaDiff describing all differences
     * @throws MigrationException if comparison fails
     */
    SchemaDiff compare(Schema source, Schema target) throws MigrationException;

    /**
     * Validate migration script against target schema.
     *
     * @param script Migration script to validate
     * @param targetSchema Target schema
     * @return ValidationResult
     */
    ValidationResult validateMigration(MigrationScript script, Schema targetSchema);
}
