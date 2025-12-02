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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a temporary git repository with automatic cleanup.
 *
 * <p>Manages the lifecycle of a cloned git repository, ensuring proper cleanup when no longer
 * needed. This is important for git operations that clone repositories to temporary directories to
 * avoid leaving artifacts on the filesystem.
 */
public class TemporaryRepository implements AutoCloseable {

    private final Path repositoryPath;
    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    /**
     * Create a new TemporaryRepository wrapping the given path.
     *
     * @param repositoryPath Path to the git repository
     */
    public TemporaryRepository(Path repositoryPath) {
        if (repositoryPath == null) {
            throw new IllegalArgumentException("Repository path must not be null");
        }
        this.repositoryPath = repositoryPath;
    }

    /**
     * Get the path to the repository.
     *
     * @return Path to the repository
     */
    public Path getPath() {
        return repositoryPath;
    }

    /**
     * Get the repository as a File.
     *
     * @return Repository directory as File
     */
    public File getFile() {
        return repositoryPath.toFile();
    }

    /**
     * Check if the repository directory exists.
     *
     * @return true if repository exists
     */
    public boolean exists() {
        return Files.exists(repositoryPath);
    }

    /**
     * Get the current git commit hash of the repository.
     *
     * @return Commit hash string
     * @throws IOException if unable to read commit hash
     */
    public String getCurrentCommit() throws IOException {
        try {
            return executeGitCommand("rev-parse", "HEAD").trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command interrupted while getting current commit", e);
        }
    }

    /**
     * Get the current branch name of the repository.
     *
     * @return Branch name string
     * @throws IOException if unable to read branch name
     */
    public String getCurrentBranch() throws IOException {
        try {
            String branch = executeGitCommand("rev-parse", "--abbrev-ref", "HEAD").trim();
            // Handle special cases like HEAD (detached)
            if ("HEAD".equals(branch)) {
                throw new IOException("Repository is in detached HEAD state");
            }
            return branch;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command interrupted while getting current branch", e);
        }
    }

    /**
     * Execute a git command in this repository.
     *
     * @param command Git command arguments
     * @return Command output
     * @throws IOException if command fails
     * @throws InterruptedException if interrupted
     */
    private String executeGitCommand(String... command) throws IOException, InterruptedException {
        String[] fullCommand = new String[command.length + 1];
        fullCommand[0] = "git";
        System.arraycopy(command, 0, fullCommand, 1, command.length);

        Process process = Runtime.getRuntime().exec(fullCommand, null, repositoryPath.toFile());

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
            throw new IOException("Git command failed: " + errorOutput);
        }

        return output.toString();
    }

    /**
     * Clean up the temporary repository by deleting the directory.
     *
     * <p>This method is safe to call multiple times. After cleanup, the repository should be
     * considered invalid and further operations should not be performed.
     *
     * @throws IOException if cleanup fails
     */
    public void cleanup() throws IOException {
        if (cleanedUp.getAndSet(true)) {
            return; // Already cleaned up
        }

        if (Files.exists(repositoryPath)) {
            deleteDirectory(repositoryPath.toFile());
        }
    }

    /**
     * Clean up the temporary repository using try-with-resources.
     *
     * <p>Example:
     *
     * <pre>
     * try (TemporaryRepository repo = new TemporaryRepository(path)) {
     *     // Use repository
     * } // Automatically cleaned up
     * </pre>
     *
     * @throws IOException if cleanup fails
     */
    @Override
    public void close() throws IOException {
        cleanup();
    }

    /**
     * Delete a directory and all its contents.
     *
     * @param directory Directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
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
