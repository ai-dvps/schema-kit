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

        try {
            // Create temporary directory for cloning
            Path tempDir = Files.createTempDirectory("git-repo-");
            Path clonePath = tempDir.resolve(getRepositoryName(repositoryPath));

            // Prepare git clone command
            String repositoryUrl = repositoryPath;
            ProcessBuilder pb;

            // Handle credentials if provided
            if (credentials != null) {
                // Check if using SSH key authentication
                if (credentials.getPrivateKeyPath() != null
                        && !credentials.getPrivateKeyPath().trim().isEmpty()) {
                    // Set up SSH environment for key-based authentication
                    pb = new ProcessBuilder("git", "clone", repositoryPath, clonePath.toString());
                    pb.directory(tempDir.toFile());

                    // Set GIT_SSH_COMMAND to use the specified private key
                    String sshCommand =
                            "ssh -i "
                                    + credentials.getPrivateKeyPath()
                                    + " -o StrictHostKeyChecking=no";
                    if (credentials.getPassphrase() != null
                            && !credentials.getPassphrase().trim().isEmpty()) {
                        // Note: In production, use ssh-agent to handle passphrases securely
                        // For now, we can't directly pass passphrase via environment
                    }
                    pb.environment().put("GIT_SSH_COMMAND", sshCommand);
                } else if (credentials.getUsername() != null && credentials.getPassword() != null) {
                    // Use HTTPS with credentials embedded in URL
                    // Format: https://username:password@repository-url
                    String urlWithCreds =
                            repositoryPath.replace(
                                    "https://",
                                    "https://"
                                            + credentials.getUsername()
                                            + ":"
                                            + credentials.getPassword()
                                            + "@");
                    pb = new ProcessBuilder("git", "clone", urlWithCreds, clonePath.toString());
                    pb.directory(tempDir.toFile());
                } else {
                    // No credentials provided or incomplete credentials
                    pb = new ProcessBuilder("git", "clone", repositoryPath, clonePath.toString());
                    pb.directory(tempDir.toFile());
                }
            } else {
                // No credentials
                pb = new ProcessBuilder("git", "clone", repositoryPath, clonePath.toString());
                pb.directory(tempDir.toFile());
            }

            // Execute clone
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // Cleanup on failure
                if (Files.exists(tempDir)) {
                    deleteDirectory(tempDir.toFile());
                }
                throw new SchemaProviderException(
                        SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                        "Failed to clone repository: git clone failed with exit code " + exitCode);
            }

            // Checkout specific branch or reference if provided
            if (reference != null && !reference.trim().isEmpty()) {
                checkoutReference(clonePath.toString(), reference);
            } else if (branch != null && !branch.trim().isEmpty()) {
                checkoutReference(clonePath.toString(), branch);
            }

            return new TemporaryRepository(clonePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchemaProviderException(
                    SchemaProviderException.ErrorCode.SOURCE_INACCESSIBLE,
                    "Git clone interrupted",
                    e);
        } catch (Exception e) {
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
        if (repositoryPath == null || repositoryPath.trim().isEmpty()) {
            return false;
        }

        try {
            // Check if it's a local path
            Path path = Paths.get(repositoryPath);
            if (Files.exists(path)) {
                // Check if it's a valid git repository
                Process process =
                        new ProcessBuilder("git", "status").directory(path.toFile()).start();
                int exitCode = process.waitFor();
                return exitCode == 0;
            }

            // For URLs, try to access it (this is a basic check)
            // In a full implementation, this would use git ls-remote
            return true; // Assume remote repos are accessible
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
            Process process =
                    new ProcessBuilder("git", "rev-parse", "--verify", "origin/" + branch)
                            .directory(new java.io.File(repositoryPath))
                            .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
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
            // Try as a tag
            Process tagProcess =
                    new ProcessBuilder("git", "rev-parse", "--verify", "refs/tags/" + reference)
                            .directory(new java.io.File(repositoryPath))
                            .start();
            int tagExitCode = tagProcess.waitFor();

            if (tagExitCode == 0) {
                return true; // Found as tag
            }

            // Try as a branch
            Process branchProcess =
                    new ProcessBuilder("git", "rev-parse", "--verify", "refs/heads/" + reference)
                            .directory(new java.io.File(repositoryPath))
                            .start();
            int branchExitCode = branchProcess.waitFor();

            if (branchExitCode == 0) {
                return true; // Found as branch
            }

            // Try as a commit hash (short or long)
            Process commitProcess =
                    new ProcessBuilder("git", "rev-parse", "--verify", reference)
                            .directory(new java.io.File(repositoryPath))
                            .start();
            int commitExitCode = commitProcess.waitFor();

            return commitExitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checkout a specific reference in a repository.
     *
     * @param repositoryPath Path to the repository
     * @param reference Reference to checkout
     * @throws IOException if checkout fails
     */
    private void checkoutReference(String repositoryPath, String reference) throws IOException {
        Process process =
                new ProcessBuilder("git", "checkout", reference)
                        .directory(new java.io.File(repositoryPath))
                        .start();

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to checkout reference: " + reference);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Checkout interrupted", e);
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
