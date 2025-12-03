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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

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
                    return resolveCommitHash(repositoryPath, trimmedRef);
                case BRANCH:
                    return resolveBranch(repositoryPath, trimmedRef);
                case TAG:
                    return resolveTag(repositoryPath, trimmedRef);
                default:
                    throw new SchemaProviderException(
                            SchemaProviderException.ErrorCode.CONFIG_INVALID,
                            "Unknown reference type: " + type);
            }
        } catch (IllegalArgumentException | SchemaProviderException e) {
            // Re-throw validation and configuration exceptions as-is
            throw e;
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
     * @param repositoryPath Path to the repository
     * @param commitHash Commit hash to validate
     * @return The validated commit hash
     */
    private String resolveCommitHash(String repositoryPath, String commitHash) {
        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository path must not be null or empty");
        }

        Path path = Paths.get(repositoryPath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Repository path does not exist: " + repositoryPath);
        }

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
        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new IOException("Repository path must not be null or empty");
        }

        Path path = Paths.get(repositoryPath);
        if (!Files.exists(path)) {
            throw new IOException("Repository path does not exist: " + repositoryPath);
        }

        try (Git git = Git.open(path.toFile())) {
            Repository repository = git.getRepository();

            // Try local branch first
            String branchRefName = "refs/heads/" + branch;
            Ref branchRef = repository.findRef(branchRefName);

            if (branchRef != null) {
                ObjectId objectId = branchRef.getObjectId();
                if (objectId != null) {
                    return objectId.getName();
                }
            }

            // Try remote branch
            String remoteBranchRefName = "refs/remotes/origin/" + branch;
            Ref remoteBranchRef = repository.findRef(remoteBranchRefName);

            if (remoteBranchRef != null) {
                ObjectId objectId = remoteBranchRef.getObjectId();
                if (objectId != null) {
                    return objectId.getName();
                }
            }

            throw new IOException("Branch not found: " + branch);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to resolve branch: " + branch, e);
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
        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new IOException("Repository path must not be null or empty");
        }

        Path path = Paths.get(repositoryPath);
        if (!Files.exists(path)) {
            throw new IOException("Repository path does not exist: " + repositoryPath);
        }

        try (Git git = Git.open(path.toFile())) {
            Repository repository = git.getRepository();

            String tagRefName = "refs/tags/" + tag;
            Ref tagRef = repository.findRef(tagRefName);

            if (tagRef != null) {
                ObjectId objectId = tagRef.getObjectId();
                if (objectId != null) {
                    return objectId.getName();
                }
            }

            throw new IOException("Tag not found: " + tag);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to resolve tag: " + tag, e);
        }
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
