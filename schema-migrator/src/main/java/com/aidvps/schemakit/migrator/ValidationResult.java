package com.aidvps.schemakit.migrator;

import java.util.Collections;
import java.util.List;

/** Result of migration validation. */
public final class ValidationResult {
    private final boolean isValid;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;

    private ValidationResult(Builder builder) {
        this.isValid = builder.isValid;
        this.errors =
                builder.errors != null
                        ? new java.util.ArrayList<>(builder.errors)
                        : new java.util.ArrayList<>();
        this.warnings =
                builder.warnings != null
                        ? new java.util.ArrayList<>(builder.warnings)
                        : new java.util.ArrayList<>();
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
     * Check if validation passed.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Get all errors.
     *
     * @return The list of errors
     */
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Get all warnings.
     *
     * @return The list of warnings
     */
    public List<ValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /** Builder for ValidationResult. */
    public static class Builder {
        private boolean isValid;
        private List<ValidationError> errors;
        private List<ValidationWarning> warnings;

        private Builder() {}

        /**
         * Set whether the result is valid.
         *
         * @param isValid true if valid
         * @return This builder
         */
        public Builder isValid(boolean isValid) {
            this.isValid = isValid;
            return this;
        }

        /**
         * Add an error.
         *
         * @param error The validation error
         * @return This builder
         */
        public Builder error(ValidationError error) {
            if (errors == null) {
                errors = new java.util.ArrayList<>();
            }
            errors.add(error);
            return this;
        }

        /**
         * Add a warning.
         *
         * @param warning The validation warning
         * @return This builder
         */
        public Builder warning(ValidationWarning warning) {
            if (warnings == null) {
                warnings = new java.util.ArrayList<>();
            }
            warnings.add(warning);
            return this;
        }

        /**
         * Build the ValidationResult instance.
         *
         * @return The result instance
         */
        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}
