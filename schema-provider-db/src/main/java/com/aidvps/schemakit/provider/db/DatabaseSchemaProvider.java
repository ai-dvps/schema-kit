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

import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;

/** Database schema provider implementation. */
public class DatabaseSchemaProvider implements SchemaProvider {

    private final DatabaseIntrospector introspector;

    public DatabaseSchemaProvider() {
        this.introspector = new DatabaseIntrospector();
    }

    @Override
    public ProviderType getType() {
        return ProviderType.DATABASE;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        if (!(config instanceof DatabaseSchemaProviderConfig)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must be DatabaseSchemaProviderConfig");
        }
        DatabaseSchemaProviderConfig dbConfig = (DatabaseSchemaProviderConfig) config;
        validateConfig(dbConfig);

        try {
            return introspector.introspect(dbConfig.getConnection(), dbConfig.getPlatform());
        } catch (Exception e) {
            if (e instanceof SchemaProviderException) {
                throw e;
            }
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Failed to extract schema from database: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void validateConfig(SchemaProviderConfig config) throws SchemaProviderException {
        if (config == null) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must not be null");
        }
        if (!(config instanceof DatabaseSchemaProviderConfig)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must be DatabaseSchemaProviderConfig");
        }
        DatabaseSchemaProviderConfig dbConfig = (DatabaseSchemaProviderConfig) config;
        if (dbConfig.getConnection() == null) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Database connection must not be null");
        }
        if (dbConfig.getPlatform() == null) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Database platform must not be null");
        }
    }
}
