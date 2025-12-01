package com.aidvps.druid.differ.internal.model;

/** Supported database platforms. */
public enum DatabasePlatform {
    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    MARIADB("MariaDB"),
    SQLITE("SQLite");

    private final String displayName;

    DatabasePlatform(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the display name of the database platform.
     *
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
