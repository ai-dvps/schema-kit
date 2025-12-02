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
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Configuration for DatabaseSchemaProvider. */
public class DatabaseSchemaProviderConfig implements SchemaProviderConfig {

    private final Connection connection;
    private final DatabasePlatform platform;
    private final String jdbcUrl;

    private DatabaseSchemaProviderConfig(Builder builder) {
        this.connection = builder.connection;
        this.platform = builder.platform;
        this.jdbcUrl = builder.jdbcUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Connection getConnection() {
        return connection;
    }

    public DatabasePlatform getPlatform() {
        return platform;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("connection", connection);
        map.put("platform", platform);
        map.put("jdbcUrl", jdbcUrl);
        return Collections.unmodifiableMap(map);
    }

    /** Builder for DatabaseSchemaProviderConfig. */
    public static class Builder {
        private Connection connection;
        private DatabasePlatform platform;
        private String jdbcUrl;

        private Builder() {}

        public Builder connection(Connection connection) {
            this.connection = connection;
            return this;
        }

        public Builder platform(DatabasePlatform platform) {
            this.platform = platform;
            return this;
        }

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public DatabaseSchemaProviderConfig build() {
            return new DatabaseSchemaProviderConfig(this);
        }
    }
}
