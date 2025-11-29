/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aidvps.druid.differ.exception;

/**
 * Exception thrown when schema parsing fails.
 *
 * <p>This exception is thrown when the druid-parser encounters invalid SQL syntax or unexpected
 * constructs in the input schema definition. It includes line and column information to help users
 * identify and fix the issue.
 */
public class SchemaParsingException extends TableDifferException {

    private final int line;
    private final int column;

    /**
     * Constructs a new SchemaParsingException with the specified detail message.
     *
     * @param message the detail message
     */
    public SchemaParsingException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    /**
     * Constructs a new SchemaParsingException with the specified detail message and location.
     *
     * @param message the detail message
     * @param line the line number where the error occurred (1-based, or -1 if unknown)
     * @param column the column number where the error occurred (1-based, or -1 if unknown)
     */
    public SchemaParsingException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    /**
     * Constructs a new SchemaParsingException with the specified detail message, location, and
     * cause.
     *
     * @param message the detail message
     * @param line the line number where the error occurred (1-based, or -1 if unknown)
     * @param column the column number where the error occurred (1-based, or -1 if unknown)
     * @param cause the cause
     */
    public SchemaParsingException(String message, int line, int column, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
    }

    /**
     * Constructs a new SchemaParsingException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SchemaParsingException(String message, Throwable cause) {
        super(message, cause);
        this.line = -1;
        this.column = -1;
    }

    /**
     * Returns the line number where the error occurred.
     *
     * @return the line number (1-based), or -1 if unknown
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number where the error occurred.
     *
     * @return the column number (1-based), or -1 if unknown
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns a detailed error message including location information if available.
     *
     * @return the formatted error message
     */
    @Override
    public String getMessage() {
        if (line > 0 || column > 0) {
            return super.getMessage() + " (Line: " + line + ", Column: " + column + ")";
        }
        return super.getMessage();
    }
}
