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
 * Base exception for all table differ errors.
 *
 * <p>This is the root exception class for the SQL Table Differ library. All specific exceptions
 * should extend from this class to provide a consistent error handling pattern.
 */
public class TableDifferException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage()
     *     method)
     */
    public TableDifferException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the getMessage()
     *     method)
     * @param cause the cause (which is saved for later retrieval by the getCause() method)
     */
    public TableDifferException(String message, Throwable cause) {
        super(message, cause);
    }
}
