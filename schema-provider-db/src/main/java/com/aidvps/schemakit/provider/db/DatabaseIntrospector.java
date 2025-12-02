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

package com.aidvps.schemakit.provider.db;

import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.sql.Connection;
import java.sql.SQLException;

/** Orchestrates database schema introspection across different database platforms. */
public class DatabaseIntrospector {

    private final DatabaseMetadataExtractor metadataExtractor;
    private final DialectResolver dialectResolver;

    public DatabaseIntrospector() {
        this.metadataExtractor = new DatabaseMetadataExtractor();
        this.dialectResolver = new DialectResolver();
    }

    /**
     * Extract schema from database connection using platform-specific introspection.
     *
     * @param connection Database connection
     * @param platform Database platform
     * @return Schema object
     * @throws SchemaProviderException if extraction fails
     */
    public Schema introspect(Connection connection, DatabasePlatform platform)
            throws SchemaProviderException {
        try {
            if (platform == null) {
                platform = dialectResolver.resolve(connection);
            }

            return metadataExtractor.extractSchema(connection, platform);
        } catch (SQLException e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.PARSE_ERROR,
                    "Failed to introspect database schema: " + e.getMessage(),
                    e);
        }
    }
}
