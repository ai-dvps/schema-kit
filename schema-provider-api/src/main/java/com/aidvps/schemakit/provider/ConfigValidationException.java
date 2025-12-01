package com.aidvps.schemakit.provider;

import java.util.ArrayList;
import java.util.List;

/** Thrown when configuration is invalid. */
public class ConfigValidationException extends SchemaProviderException {
    private final List<ValidationError> errors;

    /**
     * Construct a new exception with the specified error code and message.
     *
     * @param errorCode The error code
     * @param message The detail message
     */
    public ConfigValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.errors = new ArrayList<>();
    }

    /**
     * Construct a new exception with the specified error code, message, and cause.
     *
     * @param errorCode The error code
     * @param message The detail message
     * @param cause The cause
     */
    public ConfigValidationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.errors = new ArrayList<>();
    }

    /**
     * Construct a new exception with validation errors.
     *
     * @param message The detail message
     * @param errors The validation errors
     */
    public ConfigValidationException(String message, List<ValidationError> errors) {
        super(ErrorCode.CONFIG_INVALID, message);
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    /**
     * Construct a new exception with just a message.
     *
     * @param message The detail message
     */
    public ConfigValidationException(String message) {
        super(ErrorCode.CONFIG_INVALID, message);
        this.errors = new ArrayList<>();
    }

    /**
     * Get all validation errors.
     *
     * @return The validation errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /** Represents a validation error. */
    public static class ValidationError {
        private final String field;
        private final String message;
        private final Object invalidValue;

        /**
         * Construct a new validation error.
         *
         * @param field The field name
         * @param message The error message
         * @param invalidValue The invalid value
         */
        public ValidationError(String field, String message, Object invalidValue) {
            this.field = field;
            this.message = message;
            this.invalidValue = invalidValue;
        }

        /**
         * Get the field name.
         *
         * @return The field name
         */
        public String getField() {
            return field;
        }

        /**
         * Get the error message.
         *
         * @return The error message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Get the invalid value.
         *
         * @return The invalid value
         */
        public Object getInvalidValue() {
            return invalidValue;
        }
    }
}
