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
 * Exception thrown when schema compatibility issues are detected.
 *
 * <p>This exception is thrown when comparing schemas that cannot be migrated due to incompatible
 * changes, circular dependencies, or other structural issues that would prevent a safe migration.
 */
public class SchemaCompatibilityException extends TableDifferException {

    /**
     * Constructs a new SchemaCompatibilityException with the specified detail message.
     *
     * @param message the detail message
     */
    public SchemaCompatibilityException(String message) {
        super(message);
    }

    /**
     * Constructs a new SchemaCompatibilityException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SchemaCompatibilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
