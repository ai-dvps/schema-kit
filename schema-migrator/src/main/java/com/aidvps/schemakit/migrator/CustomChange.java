package com.aidvps.schemakit.migrator;

/** Custom change for extensibility. */
public final class CustomChange {
    private final Type type;
    private final String description;
    private final java.util.Map<String, Object> properties;

    private CustomChange(Builder builder) {
        this.type = builder.type;
        this.description = builder.description;
        this.properties =
                builder.properties != null
                        ? new java.util.HashMap<>(builder.properties)
                        : new java.util.HashMap<>();
    }

    /** Custom change type enum. */
    public enum Type {
        MY_FEATURE,
        CUSTOM_SCRIPT,
        DATA_MIGRATION,
        OTHER
    }

    /**
     * Create a new builder.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the change type.
     *
     * @return The change type
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get a property.
     *
     * @param key The property key
     * @return The property value
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get all properties.
     *
     * @return The properties map
     */
    public java.util.Map<String, Object> getProperties() {
        return new java.util.HashMap<>(properties);
    }

    /** Builder for CustomChange. */
    public static class Builder {
        private Type type;
        private String description;
        private java.util.Map<String, Object> properties;

        private Builder() {}

        /**
         * Set the change type.
         *
         * @param type The change type
         * @return This builder
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Set the description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Add a property.
         *
         * @param key The property key
         * @param value The property value
         * @return This builder
         */
        public Builder property(String key, Object value) {
            if (properties == null) {
                properties = new java.util.HashMap<>();
            }
            properties.put(key, value);
            return this;
        }

        /**
         * Build the CustomChange instance.
         *
         * @return The change instance
         */
        public CustomChange build() {
            if (type == null) {
                throw new IllegalStateException("Type must be set");
            }
            if (description == null) {
                throw new IllegalStateException("Description must be set");
            }
            return new CustomChange(this);
        }
    }
}
