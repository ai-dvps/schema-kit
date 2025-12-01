package com.aidvps.schemakit.provider;

/** Base exception for all provider errors. */
public class SchemaProviderException extends Exception {
    private final ErrorCode errorCode;
    private final String providerType;

    /** Error codes for provider exceptions. */
    public enum ErrorCode {
        CONFIG_INVALID,
        SOURCE_NOT_FOUND,
        SOURCE_INACCESSIBLE,
        PARSE_ERROR,
        VALIDATION_ERROR,
        NETWORK_ERROR,
        AUTHENTICATION_FAILED,
        QUOTA_EXCEEDED,
        TIMEOUT,
        UNKNOWN_ERROR
    }

    /**
     * Construct a new exception with the specified error code and message.
     *
     * @param errorCode The error code
     * @param message The detail message
     */
    public SchemaProviderException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.providerType = null;
    }

    /**
     * Construct a new exception with the specified error code, message, and cause.
     *
     * @param errorCode The error code
     * @param message The detail message
     * @param cause The cause
     */
    public SchemaProviderException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.providerType = null;
    }

    /**
     * Get the error code.
     *
     * @return The error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Get the provider type.
     *
     * @return The provider type
     */
    public String getProviderType() {
        return providerType;
    }
}
