package com.aidvps.schemakit.migrator;

/** Thrown when migration generation fails. */
public class MigrationException extends Exception {
    private final ErrorCode errorCode;
    private final String phase; // DIFF, ORDER, GENERATE, VALIDATE

    /** Error codes for migration exceptions. */
    public enum ErrorCode {
        SOURCE_SCHEMA_INVALID,
        TARGET_SCHEMA_INVALID,
        PLATFORM_NOT_SUPPORTED,
        DEPENDENCY_CYCLE_DETECTED,
        GENERATION_ERROR,
        VALIDATION_FAILED,
        TIMEOUT,
        UNKNOWN_ERROR
    }

    /**
     * Construct a new exception with the specified error code and message.
     *
     * @param errorCode The error code
     * @param message The detail message
     */
    public MigrationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.phase = null;
    }

    /**
     * Construct a new exception with the specified error code, message, and cause.
     *
     * @param errorCode The error code
     * @param message The detail message
     * @param cause The cause
     */
    public MigrationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.phase = null;
    }

    /**
     * Construct a new exception with the specified error code, message, and phase.
     *
     * @param errorCode The error code
     * @param message The detail message
     * @param phase The migration phase
     */
    public MigrationException(ErrorCode errorCode, String message, String phase) {
        super(message);
        this.errorCode = errorCode;
        this.phase = phase;
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
     * Get the migration phase.
     *
     * @return The migration phase
     */
    public String getPhase() {
        return phase;
    }
}
