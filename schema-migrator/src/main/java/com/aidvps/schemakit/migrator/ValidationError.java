package com.aidvps.schemakit.migrator;

/** Validation error. */
public final class ValidationError {
    private final String code;
    private final String message;
    private final String statement;
    private final int lineNumber;

    /**
     * Construct a new validation error.
     *
     * @param code The error code
     * @param message The error message
     * @param statement The SQL statement
     * @param lineNumber The line number
     */
    public ValidationError(String code, String message, String statement, int lineNumber) {
        this.code = code;
        this.message = message;
        this.statement = statement;
        this.lineNumber = lineNumber;
    }

    /**
     * Get the error code.
     *
     * @return The error code
     */
    public String getCode() {
        return code;
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
     * Get the SQL statement.
     *
     * @return The SQL statement
     */
    public String getStatement() {
        return statement;
    }

    /**
     * Get the line number.
     *
     * @return The line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return "ValidationError{"
                + "code='"
                + code
                + '\''
                + ", message='"
                + message
                + '\''
                + ", lineNumber="
                + lineNumber
                + '}';
    }
}
