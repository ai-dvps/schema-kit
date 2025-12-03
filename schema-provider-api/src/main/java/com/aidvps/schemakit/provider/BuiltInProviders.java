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

package com.aidvps.schemakit.provider;

/**
 * Constants for built-in SchemaProvider identifiers.
 *
 * <p>These constants are used to identify the built-in providers that ship with the project.
 * Custom providers can use their own unique identifiers.
 */
public final class BuiltInProviders {

    /** Directory-based file provider ID */
    public static final String DIRECTORY = "directory";

    /** Live database connection provider ID */
    public static final String DATABASE = "database";

    /** Git repository provider ID */
    public static final String GIT = "git";

    /** JAR-embedded file provider ID */
    public static final String JAR = "jar";

    private BuiltInProviders() {
        // Utility class - prevent instantiation
    }
}
