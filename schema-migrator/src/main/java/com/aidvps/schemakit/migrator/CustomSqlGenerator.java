package com.aidvps.schemakit.migrator;

/** Interface for custom SQL generation. */
public interface CustomSqlGenerator {
    /**
     * Generate SQL for custom change.
     *
     * @param change Custom change
     * @return SQL statement
     */
    MigrationStatement generateSql(CustomChange change);

    /**
     * Check if generator handles this change type.
     *
     * @param change Change to check
     * @return true if can handle
     */
    boolean canHandle(CustomChange change);
}
