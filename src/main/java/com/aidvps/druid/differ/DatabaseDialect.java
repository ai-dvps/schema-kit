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
 * Enumeration of supported database dialects for SQL generation.
 *
 * <p>Each dialect represents a specific database management system with its own SQL syntax, data
 * types, and feature set. The dialect determines how ALTER TABLE statements are generated, what
 * data types are supported, and how constraints are named and manipulated.
 */
public enum DatabaseDialect {
    /** MySQL database dialect */
    MYSQL,

    /** PostgreSQL database dialect */
    POSTGRESQL,

    /** Oracle database dialect */
    ORACLE;

    /**
     * Returns the name of this dialect in lowercase for use in logging and debugging.
     *
     * @return the lowercase name of this dialect
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
