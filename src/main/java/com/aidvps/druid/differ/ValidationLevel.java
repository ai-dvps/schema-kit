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

package com.aidvps.druid.differ;

/**
 * Defines the strictness level for schema validation and migration generation.
 *
 * <p>Different validation levels control how strictly the schema differences are validated and what
 * warnings or errors are generated during migration planning.
 */
public enum ValidationLevel {

    /**
     * Strict validation mode.
     *
     * <p>In this mode, the generator fails on any potential issue. This includes: - Treating all
     * warnings as errors - Requiring confirmation for any destructive operations - Validating all
     * foreign key references exist - Checking for potential data loss scenarios - Ensuring backward
     * compatibility
     */
    STRICT,

    /**
     * Standard validation mode (default).
     *
     * <p>This is the default behavior with balanced validation: - Generates warnings for
     * destructive operations - Validates foreign key references - Flags potential data loss risks -
     * Allows migration generation with warnings
     */
    STANDARD,

    /**
     * Lenient validation mode.
     *
     * <p>In this mode, some inconsistencies are allowed: - Only generates INFO level warnings -
     * Allows missing foreign key references (for forward references) - Minimizes validation
     * overhead - Suitable for development environments
     */
    LENIENT
}
