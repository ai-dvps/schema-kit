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

import com.aidvps.schemakit.provider.SchemaProviderException;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Resolves git references (branches, tags, commits) to concrete commit hashes.
 *
 * <p>Handles the resolution of various reference types: - Branch names (e.g., "main",
 * "feature-branch") - Tags (e.g., "v1.0.0", "release-2023.01") - Full commit hashes (40 characters)
 * - Short commit hashes (7-39 characters)
 *
 * <p>This class encapsulates the logic for determining what type of reference is provided and how
 * to resolve it to a specific commit.
 */
public class GitReferenceResolver {

    // Pattern for validating commit hashes (hex characters)
    private static final Pattern COMMIT_HASH_PATTERN =
            Pattern.compile("^[0-9a-f]+$", Pattern.CASE_INSENSITIVE);

    public GitReferenceResolver() {}

    /**
     * Resolve a git reference to a commit hash.
     *
     * @param repositoryPath Path to the git repository
     * @param reference The reference to resolve (branch, tag, or commit hash)
     * @return Resolved commit hash
     * @throws SchemaProviderException if resolution fails
     */
    public String resolveToCommitHash(String repositoryPath, String reference)
            throws SchemaProviderException {
        if (reference == null || reference.trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Reference must not be null or empty");
        }

        String trimmedRef = reference.trim();

        // Determine the type of reference and resolve accordingly
        ReferenceType type = determineReferenceType(trimmedRef);

        // Resolve the reference to a commit hash
        try {
            switch (type) {
                case COMMIT_HASH:
                    return resolveCommitHash(trimmedRef);
                case BRANCH:
                    return resolveBranch(repositoryPath, trimmedRef);
                case TAG:
                    return resolveTag(repositoryPath, trimmedRef);
                default:
                    throw new SchemaProviderException(
                            SchemaProviderException.ErrorCode.CONFIG_INVALID,
                            "Unknown reference type: " + type);
            }
        } catch (Exception e) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Failed to resolve reference '" + reference + "': " + e.getMessage(),
                    e);
        }
    }

    /**
     * Determine the type of a git reference.
     *
     * @param reference The reference string
     * @return ReferenceType enum value
     */
    public ReferenceType determineReferenceType(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference must not be null or empty");
        }

        String trimmed = reference.trim();

        // Check if it's a commit hash (all hex characters)
        if (isValidCommitHash(trimmed)) {
            return ReferenceType.COMMIT_HASH;
        }

        // Check if it looks like a tag (typically version-like)
        if (isVersionLike(trimmed) || trimmed.startsWith("tag:")) {
            return ReferenceType.TAG;
        }

        // Otherwise, treat it as a branch name
        return ReferenceType.BRANCH;
    }

    /**
     * Check if a string is a valid commit hash.
     *
     * @param hash String to check
     * @return true if valid commit hash format
     */
    private boolean isValidCommitHash(String hash) {
        if (hash.length() < 7 || hash.length() > 40) {
            return false;
        }
        return COMMIT_HASH_PATTERN.matcher(hash).matches();
    }

    /**
     * Check if a string looks like a version (for tag detection).
     *
     * @param ref String to check
     * @return true if version-like
     */
    private boolean isVersionLike(String ref) {
        // Simple heuristic: contains digits and dots, or starts with 'v'
        return Pattern.matches(".*\\d.*", ref)
                && (ref.contains(".") || ref.startsWith("v") || ref.startsWith("V"));
    }

    /**
     * Resolve a commit hash (already in hash form).
     *
     * @param commitHash Commit hash to validate
     * @return The validated commit hash
     */
    private String resolveCommitHash(String commitHash) {
        // Validate format
        if (!isValidCommitHash(commitHash)) {
            throw new IllegalArgumentException("Invalid commit hash format: " + commitHash);
        }
        return commitHash.toLowerCase();
    }

    /**
     * Resolve a branch name to a commit hash.
     *
     * @param repositoryPath Path to the repository
     * @param branch Branch name
     * @return Commit hash of the branch
     * @throws IOException if resolution fails
     */
    private String resolveBranch(String repositoryPath, String branch) throws IOException {
        // Execute git command to resolve branch to commit hash
        // Try: git rev-parse {branch} or git rev-parse origin/{branch}

        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new IOException("Repository path must not be null or empty");
        }

        try {
            // First try local branch
            String result = executeGitCommand(repositoryPath, "rev-parse", branch);
            if (isValidCommitHash(result.trim())) {
                return result.trim().toLowerCase();
            }

            // Try remote branch if local not found
            result = executeGitCommand(repositoryPath, "rev-parse", "origin/" + branch);
            if (isValidCommitHash(result.trim())) {
                return result.trim().toLowerCase();
            }

            throw new IOException("Branch not found: " + branch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command interrupted while resolving branch: " + branch, e);
        }
    }

    /**
     * Resolve a tag to a commit hash.
     *
     * @param repositoryPath Path to the repository
     * @param tag Tag name
     * @return Commit hash of the tag
     * @throws IOException if resolution fails
     */
    private String resolveTag(String repositoryPath, String tag) throws IOException {
        // Execute git command to resolve tag to commit hash
        // git rev-parse {tag}

        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new IOException("Repository path must not be null or empty");
        }

        try {
            String result = executeGitCommand(repositoryPath, "rev-parse", tag);
            if (isValidCommitHash(result.trim())) {
                return result.trim().toLowerCase();
            }
            throw new IOException("Tag not found: " + tag);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command interrupted while resolving tag: " + tag, e);
        }
    }

    /**
     * Execute a git command and return the output.
     *
     * @param repositoryPath Path to the repository
     * @param command Git command arguments
     * @return Command output
     * @throws IOException if command fails
     * @throws InterruptedException if interrupted
     */
    private String executeGitCommand(String repositoryPath, String... command)
            throws IOException, InterruptedException {
        String[] fullCommand = new String[command.length + 1];
        fullCommand[0] = "git";
        System.arraycopy(command, 0, fullCommand, 1, command.length);

        Process process =
                Runtime.getRuntime().exec(fullCommand, null, new java.io.File(repositoryPath));

        // Read output
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader =
                new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Check for errors
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            StringBuilder errorOutput = new StringBuilder();
            try (java.io.BufferedReader errorReader =
                    new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }
            }
            throw new IOException(
                    "Git command failed with exit code " + exitCode + ": " + errorOutput);
        }

        return output.toString();
    }

    /** Enum representing different types of git references. */
    public enum ReferenceType {
        /** A branch name */
        BRANCH,

        /** A tag name */
        TAG,

        /** A commit hash */
        COMMIT_HASH
    }
}
