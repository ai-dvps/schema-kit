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

package com.aidvps.schemakit.provider.git;

import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for GitSchemaProvider.
 *
 * <p>Supports configuration for cloning and checking out git repositories containing schema files.
 * Supports both local and remote repositories, branch/tag/commit references, and authentication.
 */
public class GitSchemaProviderConfig implements SchemaProviderConfig {

    private final String repositoryPath;
    private final String branch;
    private final String reference;
    private final GitCredentials credentials;
    private final boolean cloneToTemporary;

    private GitSchemaProviderConfig(Builder builder) {
        this.repositoryPath = builder.repositoryPath;
        this.branch = builder.branch;
        this.reference = builder.reference;
        this.credentials = builder.credentials;
        this.cloneToTemporary = builder.cloneToTemporary;
    }

    /**
     * Create a new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public ProviderType getType() {
        return ProviderType.GIT;
    }

    /**
     * Get the repository path. Can be a local directory path or remote repository URL.
     *
     * @return Repository path
     */
    public String getRepositoryPath() {
        return repositoryPath;
    }

    /**
     * Get the branch name to checkout. Defaults to "main" if not specified.
     *
     * @return Branch name
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Get the git reference (tag or commit hash). If specified, takes precedence over branch.
     *
     * @return Git reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * Get the git credentials for authentication.
     *
     * @return Git credentials, may be null for public repositories
     */
    public GitCredentials getCredentials() {
        return credentials;
    }

    /**
     * Check if repository should be cloned to a temporary location.
     *
     * @return true if cloning to temporary location, false otherwise
     */
    public boolean isCloneToTemporary() {
        return cloneToTemporary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GitSchemaProviderConfig that = (GitSchemaProviderConfig) o;
        return cloneToTemporary == that.cloneToTemporary
                && Objects.equals(repositoryPath, that.repositoryPath)
                && Objects.equals(branch, that.branch)
                && Objects.equals(reference, that.reference)
                && Objects.equals(credentials, that.credentials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryPath, branch, reference, credentials, cloneToTemporary);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("repositoryPath", repositoryPath);
        map.put("branch", branch);
        map.put("reference", reference);
        map.put("credentials", credentials);
        map.put("cloneToTemporary", cloneToTemporary);
        map.put("type", getType().name());
        return map;
    }

    @Override
    public String toString() {
        return "GitSchemaProviderConfig{"
                + "repositoryPath='"
                + repositoryPath
                + '\''
                + ", branch='"
                + branch
                + '\''
                + ", reference='"
                + reference
                + '\''
                + ", credentials="
                + credentials
                + ", cloneToTemporary="
                + cloneToTemporary
                + '}';
    }

    /** Builder for GitSchemaProviderConfig. */
    public static class Builder {

        private String repositoryPath;
        private String branch = "main";
        private String reference;
        private GitCredentials credentials;
        private boolean cloneToTemporary = true;

        private Builder() {}

        /**
         * Set the repository path. Can be a local directory path or remote repository URL.
         *
         * @param repositoryPath Repository path or URL
         * @return This builder
         */
        public Builder repositoryPath(String repositoryPath) {
            this.repositoryPath = repositoryPath;
            return this;
        }

        /**
         * Set the branch name to checkout. Defaults to "main" if not specified.
         *
         * @param branch Branch name
         * @return This builder
         */
        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        /**
         * Set the git reference (tag or commit hash). If specified, takes precedence over branch.
         *
         * @param reference Git reference (tag or commit hash)
         * @return This builder
         */
        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Set the git credentials for authentication.
         *
         * @param credentials Git credentials
         * @return This builder
         */
        public Builder credentials(GitCredentials credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Set whether to clone to a temporary location. Defaults to true.
         *
         * @param cloneToTemporary true to clone to temporary location, false otherwise
         * @return This builder
         */
        public Builder cloneToTemporary(boolean cloneToTemporary) {
            this.cloneToTemporary = cloneToTemporary;
            return this;
        }

        /**
         * Build the GitSchemaProviderConfig.
         *
         * @return GitSchemaProviderConfig instance
         * @throws IllegalArgumentException if repositoryPath is null or empty
         */
        public GitSchemaProviderConfig build() {
            if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
                throw new IllegalArgumentException("repositoryPath must not be null or empty");
            }
            return new GitSchemaProviderConfig(this);
        }
    }
}
