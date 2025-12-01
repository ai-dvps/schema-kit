package com.aidvps.schemakit.provider;

/** Enum identifying provider type. */
public enum ProviderType {
    DIRECTORY("Directory-based file provider"),
    DATABASE("Live database connection provider"),
    GIT("Git repository provider"),
    JAR("JAR-embedded file provider"),
    CUSTOM("Custom provider");

    private final String description;

    ProviderType(String description) {
        this.description = description;
    }

    /**
     * Get the provider type description.
     *
     * @return Description of this provider type
     */
    public String getDescription() {
        return description;
    }
}
