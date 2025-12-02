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

import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.schemakit.provider.ProviderType;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SchemaProvider implementation for git repository sources.
 *
 * <p>Supports cloning git repositories and extracting schema files from branches, tags, or specific
 * commits. Works with both local and remote repositories.
 */
public class GitSchemaProvider implements SchemaProvider {

    private final GitRepositoryManager repositoryManager;

    public GitSchemaProvider() {
        this.repositoryManager = new GitRepositoryManager();
    }

    // Constructor for testing
    GitSchemaProvider(GitRepositoryManager repositoryManager) {
        this.repositoryManager = repositoryManager;
    }

    @Override
    public ProviderType getType() {
        return ProviderType.GIT;
    }

    @Override
    public Schema getSchema(SchemaProviderConfig config) throws SchemaProviderException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }

        if (!(config instanceof GitSchemaProviderConfig)) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Configuration must be GitSchemaProviderConfig");
        }

        GitSchemaProviderConfig gitConfig = (GitSchemaProviderConfig) config;

        // Validate configuration
        validateConfig(gitConfig);

        // TODO: Implement full git repository cloning and schema extraction
        // For now, throw UnsupportedOperationException while dependencies are being built
        throw new UnsupportedOperationException(
                "GitSchemaProvider implementation in progress. Full schema extraction "
                        + "will be available once supporting classes are complete.");
    }

    /**
     * Validate the git provider configuration.
     *
     * @param config Git configuration to validate
     * @throws SchemaProviderException if configuration is invalid
     */
    private void validateConfig(GitSchemaProviderConfig config) throws SchemaProviderException {
        String repositoryPath = config.getRepositoryPath();

        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Repository path must not be null or empty");
        }

        // Check if repository path is valid
        try {
            Path path = Paths.get(repositoryPath);
            // Repository path will be validated by GitRepositoryManager
        } catch (Exception e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Invalid repository path: " + repositoryPath);
        }

        // Validate that if reference is provided, it's not empty
        String reference = config.getReference();
        if (reference != null && reference.trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Reference must not be empty if specified");
        }
    }
}
