package com.aidvps.schemakit.migrator;

/** Validation warning. */
public final class ValidationWarning {
    private final String code;
    private final String message;
    private final String statement;

    /**
     * Construct a new validation warning.
     *
     * @param code The warning code
     * @param message The warning message
     * @param statement The SQL statement
     */
    public ValidationWarning(String code, String message, String statement) {
        this.code = code;
        this.message = message;
        this.statement = statement;
    }

    /**
     * Get the warning code.
     *
     * @return The warning code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get the warning message.
     *
     * @return The warning message
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

    @Override
    public String toString() {
        return "ValidationWarning{" + "code='" + code + '\'' + ", message='" + message + '\'' + '}';
    }
}
