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
import com.aidvps.schemakit.provider.BuiltInProviders;
import com.aidvps.schemakit.provider.SchemaProvider;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import com.aidvps.schemakit.provider.SchemaProviderException;
import java.io.IOException;
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
    public String getProviderId() {
        return BuiltInProviders.GIT;
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

        // Clone repository and extract schema
        try {
            String repositoryPath = gitConfig.getRepositoryPath();
            String reference = gitConfig.getReference();
            String branch = gitConfig.getBranch();
            GitCredentials credentials = gitConfig.getCredentials();

            // Determine which reference to use (branch or reference)
            String resolvedReference = branch != null ? branch : reference;

            // Clone repository
            TemporaryRepository tempRepo =
                    repositoryManager.cloneRepository(
                            repositoryPath,
                            resolvedReference,
                            null, // additional reference
                            credentials);

            // Extract schema from cloned repository
            return extractSchemaFromRepository(tempRepo.getPath(), gitConfig);
        } catch (Exception e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Failed to get schema from git repository: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Extract schema from a cloned git repository.
     *
     * @param repositoryPath Path to the cloned repository
     * @param config Git configuration
     * @return Schema extracted from repository
     * @throws Exception if extraction fails
     */
    private Schema extractSchemaFromRepository(Path repositoryPath, GitSchemaProviderConfig config)
            throws Exception {
        String repoName = repositoryPath.getFileName().toString();

        // Get dialect from config, default to MYSQL if not specified
        com.aidvps.druid.differ.DatabaseDialect dialect = config.getDialect();
        if (dialect == null) {
            dialect = com.aidvps.druid.differ.DatabaseDialect.MYSQL;
        }

        // Scan repository for schema files
        java.util.List<Path> schemaFiles = findSchemaFiles(repositoryPath);

        if (schemaFiles.isEmpty()) {
            // No schema files found, return empty schema
            return Schema.builder(dialect).build();
        }

        // Parse schema files and combine into a single schema
        Schema.Builder schemaBuilder = Schema.builder(dialect);

        for (Path schemaFile : schemaFiles) {
            try {
                parseSchemaFile(schemaFile, schemaBuilder);
            } catch (Exception e) {
                // Log warning but continue processing other files
                System.err.println(
                        "Warning: Failed to parse schema file "
                                + schemaFile
                                + ": "
                                + e.getMessage());
            }
        }

        return schemaBuilder.build();
    }

    /**
     * Scan repository for schema files.
     *
     * @param repositoryPath Path to repository
     * @return List of schema file paths
     * @throws IOException if scanning fails
     */
    private java.util.List<Path> findSchemaFiles(Path repositoryPath) throws IOException {
        java.util.List<Path> schemaFiles = new java.util.ArrayList<>();

        // Common schema file extensions
        String[] extensions = {".sql", ".db", ".tbl"};

        // Walk the directory tree
        java.nio.file.Files.walkFileTree(
                repositoryPath,
                new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(
                            Path file, java.nio.file.attribute.BasicFileAttributes attrs)
                            throws IOException {
                        String fileName = file.getFileName().toString().toLowerCase();

                        // Check if file has a schema file extension
                        for (String ext : extensions) {
                            if (fileName.endsWith(ext)) {
                                // Skip files in .git directory
                                if (!file.toString()
                                        .contains(
                                                java.io.File.separator
                                                        + ".git"
                                                        + java.io.File.separator)) {
                                    schemaFiles.add(file);
                                }
                                break;
                            }
                        }
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });

        return schemaFiles;
    }

    /**
     * Parse a single schema file and add tables to the schema builder.
     *
     * @param schemaFile Path to schema file
     * @param schemaBuilder Schema builder to add tables to
     * @throws Exception if parsing fails
     */
    private void parseSchemaFile(Path schemaFile, Schema.Builder schemaBuilder) throws Exception {
        String fileName = schemaFile.getFileName().toString().toLowerCase();

        try {
            // Read file content
            String content = new String(java.nio.file.Files.readAllBytes(schemaFile));

            // Simple parsing logic - in a full implementation, this would use
            // proper SQL parsing libraries or the existing schema parsing infrastructure
            if (fileName.endsWith(".sql")) {
                parseSqlFile(content, schemaBuilder);
            } else if (fileName.endsWith(".db") || fileName.endsWith(".tbl")) {
                parseTableFile(content, schemaBuilder);
            }
        } catch (Exception e) {
            throw new Exception("Failed to parse schema file: " + schemaFile, e);
        }
    }

    /**
     * Parse SQL file content and extract table definitions.
     *
     * @param content SQL file content
     * @param schemaBuilder Schema builder
     */
    private void parseSqlFile(String content, Schema.Builder schemaBuilder) {
        // Simple regex-based parsing for CREATE TABLE statements
        // In production, use a proper SQL parser

        // This is a simplified implementation
        // Real implementation would use a proper SQL parser like JSqlParser
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "CREATE TABLE\\s+(\\w+)\\s*\\((.*?)\\);",
                        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);

        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String tableName = matcher.group(1);
            String columnsDef = matcher.group(2);

            // For now, just add the table name to metadata
            // In full implementation, parse columns and create Table objects
            schemaBuilder.addMetadata("table:" + tableName, "SQL file parsed");
        }
    }

    /**
     * Parse table file content (DB or TBL format).
     *
     * @param content Table file content
     * @param schemaBuilder Schema builder
     */
    private void parseTableFile(String content, Schema.Builder schemaBuilder) {
        // Simple parsing for table files
        // These files typically contain table definitions in a simpler format

        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Add line to metadata as a simple table reference
                // In full implementation, parse the format properly
                schemaBuilder.addMetadata("table:" + line, "Table file parsed");
            }
        }
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
