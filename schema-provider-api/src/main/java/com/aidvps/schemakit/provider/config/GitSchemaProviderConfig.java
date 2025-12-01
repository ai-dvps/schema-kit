package com.aidvps.schemakit.provider.config;

import com.aidvps.schemakit.provider.ConfigValidationException;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Configuration for git repository provider. */
public interface GitSchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get repository URL.
     *
     * @return Git repository URL
     */
    String getRepositoryUrl();

    /**
     * Get branch/tag/commit reference.
     *
     * @return Git reference (branch, tag, or commit hash)
     */
    String getReference();

    /**
     * Get local directory for checkout.
     *
     * @return Path for checkout (temporary if not specified)
     */
    Path getLocalPath();

    /**
     * Get authentication credentials.
     *
     * @return Optional credentials
     */
    Optional<GitCredentials> getCredentials();

    /** Create builder. */
    static Builder builder() {
        return new Builder();
    }

    /** Git credentials. */
    class GitCredentials {
        private final String username;
        private final String password;
        private final String token;

        private GitCredentials(Builder builder) {
            this.username = builder.username;
            this.password = builder.password;
            this.token = builder.token;
        }

        /**
         * Create a new builder.
         *
         * @return A new builder instance
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Get the username.
         *
         * @return The username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Get the password.
         *
         * @return The password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Get the token.
         *
         * @return The token
         */
        public String getToken() {
            return token;
        }

        /** Builder for GitCredentials. */
        public static class Builder {
            private String username;
            private String password;
            private String token;

            private Builder() {}

            /**
             * Set the username.
             *
             * @param username The username
             * @return This builder
             */
            public Builder username(String username) {
                this.username = username;
                return this;
            }

            /**
             * Set the password.
             *
             * @param password The password
             * @return This builder
             */
            public Builder password(String password) {
                this.password = password;
                return this;
            }

            /**
             * Set the token.
             *
             * @param token The token
             * @return This builder
             */
            public Builder token(String token) {
                this.token = token;
                return this;
            }

            /**
             * Build the GitCredentials instance.
             *
             * @return The credentials instance
             */
            public GitCredentials build() {
                return new GitCredentials(this);
            }
        }
    }

    /** Builder for GitSchemaProviderConfig. */
    class Builder {
        private String repositoryUrl;
        private String reference;
        private Path localPath;
        private GitCredentials credentials;

        private Builder() {}

        /**
         * Set the repository URL.
         *
         * @param repositoryUrl The repository URL
         * @return This builder
         */
        public Builder repositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        /**
         * Set the git reference.
         *
         * @param reference The git reference (branch, tag, or commit)
         * @return This builder
         */
        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Set the local path.
         *
         * @param localPath The local path for checkout
         * @return This builder
         */
        public Builder localPath(Path localPath) {
            this.localPath = localPath;
            return this;
        }

        /**
         * Set the credentials.
         *
         * @param credentials The Git credentials
         * @return This builder
         */
        public Builder credentials(GitCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Build the GitSchemaProviderConfig instance.
         *
         * @return The configuration instance
         * @throws ConfigValidationException if configuration is invalid
         */
        public GitSchemaProviderConfig build() throws ConfigValidationException {
            return new GitSchemaProviderConfigImpl(this);
        }
    }

    /** Implementation of GitSchemaProviderConfig. */
    class GitSchemaProviderConfigImpl implements GitSchemaProviderConfig {
        private final String repositoryUrl;
        private final String reference;
        private final Path localPath;
        private final GitCredentials credentials;

        private GitSchemaProviderConfigImpl(Builder builder) throws ConfigValidationException {
            this.repositoryUrl = builder.repositoryUrl;
            this.reference = builder.reference;
            this.localPath = builder.localPath;
            this.credentials = builder.credentials;

            // Validate in constructor
            doValidate();
        }

        private void doValidate() throws ConfigValidationException {
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                throw new ConfigValidationException("Repository URL must not be null or empty");
            }
            if (reference == null || reference.trim().isEmpty()) {
                throw new ConfigValidationException("Reference must not be null or empty");
            }
        }

        @Override
        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public Path getLocalPath() {
            return localPath;
        }

        @Override
        public Optional<GitCredentials> getCredentials() {
            return Optional.ofNullable(credentials);
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("repositoryUrl", repositoryUrl);
            map.put("reference", reference);
            map.put("localPath", localPath != null ? localPath.toString() : null);
            map.put("credentials", credentials != null ? "***" : null);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public void validate() throws ConfigValidationException {
            doValidate();
        }
    }
}
