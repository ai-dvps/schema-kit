package com.aidvps.schemakit.migrator;

/** Migration mode enum. */
public enum MigrationMode {
    /** Full migration: create new objects, alter existing, drop removed */
    FULL("Complete migration with all changes"),

    /** Forward-only: create new objects, alter existing (no drops) */
    FORWARD_ONLY("Forward migration only, no drops"),

    /** Diff-only: return SQL for specific changes */
    DIFF_ONLY("Generate SQL for detected changes only");

    private final String description;

    MigrationMode(String description) {
        this.description = description;
    }

    /**
     * Get the migration mode description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
}
