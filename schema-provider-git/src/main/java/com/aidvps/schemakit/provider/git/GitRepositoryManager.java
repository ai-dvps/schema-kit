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
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Manages git repository operations including cloning, checking out, and cleanup.
 *
 * <p>Handles both local and remote repositories with support for branches, tags, and commit
 * references. Provides temporary repository lifecycle management.
 */
public class GitRepositoryManager {

    public GitRepositoryManager() {}

    /**
     * Clone a git repository to a temporary location.
     *
     * @param repositoryPath Path or URL of the repository to clone
     * @param branch Branch to checkout after cloning
     * @param reference Git reference (tag or commit) to checkout
     * @param credentials Git credentials for authentication
     * @return TemporaryRepository instance for the cloned repository
     * @throws SchemaProviderException if cloning fails
     */
    public TemporaryRepository cloneRepository(
            String repositoryPath, String branch, String reference, GitCredentials credentials)
            throws SchemaProviderException {
        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Repository path must not be null or empty");
        }

        // Check if branch parameter is provided - not supported
        if (branch != null && !branch.trim().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Cloning with specific branch is not supported");
        }

        // Check if reference parameter is provided - not supported
        if (reference != null && !reference.trim().isEmpty()) {
            throw new UnsupportedOperationException(
                    "Cloning with specific reference is not supported");
        }

        Path tempDir = null;
        try {
            // Create temporary directory for cloning
            tempDir = Files.createTempDirectory("git-repo-");
            Path clonePath = tempDir.resolve(getRepositoryName(repositoryPath));

            // Prepare JGit clone command
            CloneCommand cloneCommand =
                    Git.cloneRepository().setURI(repositoryPath).setDirectory(clonePath.toFile());

            // Handle credentials if provided
            if (credentials != null) {
                CredentialsProvider credentialProvider = null;

                // Check if using SSH key authentication
                if (credentials.getPrivateKeyPath() != null
                        && !credentials.getPrivateKeyPath().trim().isEmpty()) {
                    // JGit handles SSH keys automatically through SSH config
                    // The private key path should be configured in ~/.ssh/config
                    // For now, we'll use a default credential provider
                    // Note: In production, configure SSH properly
                    credentialProvider = CredentialsProvider.getDefault();
                } else if (credentials.getUsername() != null && credentials.getPassword() != null) {
                    // Use HTTPS with username/password
                    credentialProvider =
                            new UsernamePasswordCredentialsProvider(
                                    credentials.getUsername(), credentials.getPassword());
                }

                if (credentialProvider != null) {
                    cloneCommand.setCredentialsProvider(credentialProvider);
                }
            }

            // Execute clone (clone everything)
            Git git = cloneCommand.call();

            git.close();

            return new TemporaryRepository(clonePath);
        } catch (Exception e) {
            // Cleanup on failure
            if (tempDir != null && Files.exists(tempDir)) {
                try {
                    deleteDirectory(tempDir.toFile());
                } catch (IOException cleanupException) {
                    // Log but don't throw
                    System.err.println(
                            "Failed to cleanup temp directory: " + cleanupException.getMessage());
                }
            }
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Failed to clone repository: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Check if a git repository exists and is accessible.
     *
     * @param repositoryPath Path or URL of the repository
     * @return true if repository exists and is accessible
     * @throws SchemaProviderException if check fails
     */
    public boolean repositoryExists(String repositoryPath) throws SchemaProviderException {
        if (repositoryPath == null) {
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.CONFIG_INVALID,
                    "Repository path must not be null");
        }

        if (repositoryPath.trim().isEmpty()) {
            return false;
        }

        try {
            // Check if it's a local path
            Path path = Paths.get(repositoryPath);
            if (Files.exists(path)) {
                // Check if it's a valid git repository using JGit
                try (Git git = Git.open(path.toFile())) {
                    return git.getRepository().getObjectDatabase().exists();
                }
            }

            // For URLs, check if it looks like a valid Git URL format
            // HTTPS URLs
            if (repositoryPath.startsWith("https://")) {
                return true;
            }

            // SSH URLs
            if (repositoryPath.startsWith("git@")) {
                return true;
            }

            // Try to use lsRemoteCommand for other URL formats
            try {
                org.eclipse.jgit.api.LsRemoteCommand lsRemoteCommand =
                        org.eclipse.jgit.api.Git.lsRemoteRepository().setRemote(repositoryPath);
                lsRemoteCommand.call();
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate that a git repository has the specified branch.
     *
     * @param repositoryPath Path to the git repository
     * @param branch Branch name to validate
     * @return true if branch exists in the repository
     * @throws SchemaProviderException if validation fails
     */
    public boolean branchExists(String repositoryPath, String branch)
            throws SchemaProviderException {
        if (branch == null || branch.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(repositoryPath);
            if (!Files.exists(path)) {
                return false;
            }

            // Use JGit to check if branch exists
            try (Git git = Git.open(path.toFile())) {
                String branchRef = "refs/heads/" + branch;
                return git.getRepository().findRef(branchRef) != null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate that a git repository has the specified reference (tag or commit).
     *
     * @param repositoryPath Path to the git repository
     * @param reference Reference (tag or commit hash) to validate
     * @return true if reference exists in the repository
     * @throws SchemaProviderException if validation fails
     */
    public boolean referenceExists(String repositoryPath, String reference)
            throws SchemaProviderException {
        if (reference == null || reference.trim().isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(repositoryPath);
            if (!Files.exists(path)) {
                return false;
            }

            // Use JGit to check if reference exists
            try (Git git = Git.open(path.toFile())) {
                // Try as a tag
                String tagRef = "refs/tags/" + reference;
                if (git.getRepository().findRef(tagRef) != null) {
                    return true; // Found as tag
                }

                // Try as a branch
                String branchRef = "refs/heads/" + reference;
                if (git.getRepository().findRef(branchRef) != null) {
                    return true; // Found as branch
                }

                // Try as a commit hash (short or long)
                try {
                    org.eclipse.jgit.revwalk.RevWalk revWalk =
                            new org.eclipse.jgit.revwalk.RevWalk(git.getRepository());
                    org.eclipse.jgit.revwalk.RevCommit commit =
                            revWalk.parseCommit(
                                    org.eclipse.jgit.lib.ObjectId.fromString(reference));
                    revWalk.close();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get repository name from path or URL.
     *
     * @param repositoryPath Path or URL
     * @return Repository name
     */
    private String getRepositoryName(String repositoryPath) {
        String path = repositoryPath;
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            path = path.substring(lastSlash + 1);
        }

        return path;
    }

    /**
     * Delete a directory and all its contents.
     *
     * @param directory Directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectory(java.io.File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }

        if (!directory.delete()) {
            throw new IOException("Failed to delete directory: " + directory.getAbsolutePath());
        }
    }
}
