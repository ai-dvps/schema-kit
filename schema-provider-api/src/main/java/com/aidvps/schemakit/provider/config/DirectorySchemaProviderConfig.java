package com.aidvps.schemakit.provider.config;

import com.aidvps.schemakit.provider.ConfigValidationException;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Configuration for directory-based provider. */
public interface DirectorySchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get the directory path.
     *
     * @return Path to directory containing schema files
     */
    Path getPath();

    /**
     * Whether to validate file structure.
     *
     * @return true to validate, false to skip
     */
    boolean isValidateStructure();

    /**
     * Get file encoding.
     *
     * @return Character encoding (default: UTF-8)
     */
    String getEncoding();

    /** Create builder for DirectorySchemaProviderConfig. */
    static Builder builder() {
        return new Builder();
    }

    /** Builder for DirectorySchemaProviderConfig. */
    class Builder {
        private Path path;
        private boolean validateStructure = true;
        private String encoding = "UTF-8";

        private Builder() {}

        /**
         * Set the directory path.
         *
         * @param path The directory path
         * @return This builder
         */
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Set whether to validate structure.
         *
         * @param validateStructure true to validate, false to skip
         * @return This builder
         */
        public Builder validateStructure(boolean validateStructure) {
            this.validateStructure = validateStructure;
            return this;
        }

        /**
         * Set the file encoding.
         *
         * @param encoding The character encoding
         * @return This builder
         */
        public Builder encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Build the DirectorySchemaProviderConfig instance.
         *
         * @return The configuration instance
         * @throws ConfigValidationException if configuration is invalid
         */
        public DirectorySchemaProviderConfig build() throws ConfigValidationException {
            return new DirectorySchemaProviderConfigImpl(this);
        }
    }

    /** Implementation of DirectorySchemaProviderConfig. */
    class DirectorySchemaProviderConfigImpl implements DirectorySchemaProviderConfig {
        private final Path path;
        private final boolean validateStructure;
        private final String encoding;

        private DirectorySchemaProviderConfigImpl(Builder builder)
                throws ConfigValidationException {
            this.path = builder.path;
            this.validateStructure = builder.validateStructure;
            this.encoding = builder.encoding;

            // Validate in constructor
            doValidate();
        }

        private void doValidate() throws ConfigValidationException {
            if (path == null) {
                throw new ConfigValidationException("Path must not be null");
            }
            if (!path.toFile().exists()) {
                throw new ConfigValidationException("Path does not exist: " + path);
            }
            if (!path.toFile().isDirectory()) {
                throw new ConfigValidationException("Path is not a directory: " + path);
            }
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public boolean isValidateStructure() {
            return validateStructure;
        }

        @Override
        public String getEncoding() {
            return encoding;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("path", path.toString());
            map.put("validateStructure", validateStructure);
            map.put("encoding", encoding);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public void validate() throws ConfigValidationException {
            doValidate();
        }
    }
}
