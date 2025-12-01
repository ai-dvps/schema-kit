package com.aidvps.schemakit.provider.config;

import com.aidvps.schemakit.provider.ConfigValidationException;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SecretProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/** Configuration for live database provider. */
public interface DatabaseSchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get the data source.
     *
     * @return JDBC DataSource
     */
    DataSource getDataSource();

    /**
     * Get included databases.
     *
     * @return List of database names to include (null/empty for all)
     */
    List<String> getIncludedDatabases();

    /**
     * Get excluded databases.
     *
     * @return List of database names to exclude
     */
    List<String> getExcludedDatabases();

    /**
     * Get credential provider.
     *
     * @return SecretProvider for credentials
     */
    SecretProvider getCredentialProvider();

    /** Create builder. */
    static Builder builder() {
        return new Builder();
    }

    /** Builder for DatabaseSchemaProviderConfig. */
    class Builder {
        private DataSource dataSource;
        private List<String> includedDatabases;
        private List<String> excludedDatabases;
        private SecretProvider credentialProvider;

        private Builder() {}

        /**
         * Set the data source.
         *
         * @param dataSource The JDBC DataSource
         * @return This builder
         */
        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * Include specific databases.
         *
         * @param databases Database names to include
         * @return This builder
         */
        public Builder includeDatabases(String... databases) {
            if (includedDatabases == null) {
                includedDatabases = new ArrayList<>();
            }
            for (String db : databases) {
                includedDatabases.add(db);
            }
            return this;
        }

        /**
         * Exclude specific databases.
         *
         * @param databases Database names to exclude
         * @return This builder
         */
        public Builder excludeDatabases(String... databases) {
            if (excludedDatabases == null) {
                excludedDatabases = new ArrayList<>();
            }
            for (String db : databases) {
                excludedDatabases.add(db);
            }
            return this;
        }

        /**
         * Set the credential provider.
         *
         * @param credentialProvider The SecretProvider
         * @return This builder
         */
        public Builder credentialProvider(SecretProvider credentialProvider) {
            this.credentialProvider = credentialProvider;
            return this;
        }

        /**
         * Build the DatabaseSchemaProviderConfig instance.
         *
         * @return The configuration instance
         * @throws ConfigValidationException if configuration is invalid
         */
        public DatabaseSchemaProviderConfig build() throws ConfigValidationException {
            return new DatabaseSchemaProviderConfigImpl(this);
        }
    }

    /** Implementation of DatabaseSchemaProviderConfig. */
    class DatabaseSchemaProviderConfigImpl implements DatabaseSchemaProviderConfig {
        private final DataSource dataSource;
        private final List<String> includedDatabases;
        private final List<String> excludedDatabases;
        private final SecretProvider credentialProvider;

        private DatabaseSchemaProviderConfigImpl(Builder builder) throws ConfigValidationException {
            this.dataSource = builder.dataSource;
            this.includedDatabases =
                    builder.includedDatabases != null
                            ? new ArrayList<>(builder.includedDatabases)
                            : null;
            this.excludedDatabases =
                    builder.excludedDatabases != null
                            ? new ArrayList<>(builder.excludedDatabases)
                            : null;
            this.credentialProvider = builder.credentialProvider;

            // Validate in constructor
            doValidate();
        }

        private void doValidate() throws ConfigValidationException {
            if (dataSource == null) {
                throw new ConfigValidationException("DataSource must not be null");
            }
        }

        @Override
        public DataSource getDataSource() {
            return dataSource;
        }

        @Override
        public List<String> getIncludedDatabases() {
            return includedDatabases != null
                    ? Collections.unmodifiableList(includedDatabases)
                    : Collections.emptyList();
        }

        @Override
        public List<String> getExcludedDatabases() {
            return excludedDatabases != null
                    ? Collections.unmodifiableList(excludedDatabases)
                    : Collections.emptyList();
        }

        @Override
        public SecretProvider getCredentialProvider() {
            return credentialProvider;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("dataSource", dataSource != null ? dataSource.getClass().getName() : null);
            map.put("includedDatabases", includedDatabases);
            map.put("excludedDatabases", excludedDatabases);
            map.put(
                    "credentialProvider",
                    credentialProvider != null ? credentialProvider.getClass().getName() : null);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public void validate() throws ConfigValidationException {
            doValidate();
        }
    }
}
