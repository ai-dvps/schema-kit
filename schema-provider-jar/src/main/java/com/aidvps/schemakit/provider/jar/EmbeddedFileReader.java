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

package com.aidvps.schemakit.provider.jar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads embedded files from JAR archives and file system.
 *
 * <p>Provides utilities for reading schema files (.db and .tbl) from JAR resources with proper
 * encoding handling.
 */
public class EmbeddedFileReader {

    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Read an embedded file from a JAR.
     *
     * @param jarPath Path to the JAR file
     * @param entryPath Path to the entry within the JAR
     * @return File content as String
     * @throws IOException if reading fails
     */
    public String readEmbeddedFile(String jarPath, String entryPath) throws IOException {
        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JAR path must not be null or empty");
        }

        if (entryPath == null || entryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry path must not be null or empty");
        }

        // Try file system JAR first
        Path jarFilePath = java.nio.file.Paths.get(jarPath);
        if (Files.exists(jarFilePath)) {
            return readFromFileSystemJar(jarFilePath, entryPath);
        }

        // Try classpath
        return readFromClasspath(jarPath, entryPath);
    }

    /**
     * Read all lines from an embedded file.
     *
     * @param jarPath Path to the JAR file
     * @param entryPath Path to the entry within the JAR
     * @return List of lines
     * @throws IOException if reading fails
     */
    public List<String> readEmbeddedFileLines(String jarPath, String entryPath) throws IOException {
        String content = readEmbeddedFile(jarPath, entryPath);
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }

    /**
     * Read an embedded file as a stream.
     *
     * @param jarPath Path to the JAR file
     * @param entryPath Path to the entry within the JAR
     * @return InputStream for the embedded file
     * @throws IOException if the file cannot be opened
     */
    public InputStream readEmbeddedFileAsStream(String jarPath, String entryPath)
            throws IOException {
        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JAR path must not be null or empty");
        }

        if (entryPath == null || entryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry path must not be null or empty");
        }

        // Try file system JAR first
        Path jarFilePath = java.nio.file.Paths.get(jarPath);
        if (Files.exists(jarFilePath)) {
            return openStreamFromFileSystemJar(jarFilePath, entryPath);
        }

        // Try classpath
        return openStreamFromClasspath(jarPath, entryPath);
    }

    /**
     * Read from a file system JAR.
     *
     * @param jarPath Path to the JAR file
     * @param entryPath Path to the entry within the JAR
     * @return File content
     * @throws IOException if reading fails
     */
    private String readFromFileSystemJar(Path jarPath, String entryPath) throws IOException {
        java.util.jar.JarFile jarFile = null;
        try {
            jarFile = new java.util.jar.JarFile(jarPath.toFile());
            java.util.jar.JarEntry entry = jarFile.getJarEntry(entryPath);

            if (entry == null) {
                throw new IOException("Entry not found in JAR: " + entryPath);
            }

            return readEntry(jarFile.getInputStream(entry));
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }

    /**
     * Open a stream from a file system JAR.
     *
     * @param jarPath Path to the JAR file
     * @param entryPath Path to the entry within the JAR
     * @return InputStream
     * @throws IOException if opening fails
     */
    private InputStream openStreamFromFileSystemJar(Path jarPath, String entryPath)
            throws IOException {
        java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath.toFile());
        java.util.jar.JarEntry entry = jarFile.getJarEntry(entryPath);

        if (entry == null) {
            jarFile.close();
            throw new IOException("Entry not found in JAR: " + entryPath);
        }

        // Return a wrapper that also closes the JAR file
        return new java.io.FilterInputStream(jarFile.getInputStream(entry)) {
            @Override
            public void close() throws IOException {
                super.close();
                jarFile.close();
            }
        };
    }

    /**
     * Read from classpath.
     *
     * @param jarPath Path to the JAR (as classpath resource)
     * @param entryPath Path to the entry within the JAR
     * @return File content
     * @throws IOException if reading fails
     */
    private String readFromClasspath(String jarPath, String entryPath) throws IOException {
        // Construct the full classpath resource path
        String resourcePath = jarPath;
        if (!jarPath.endsWith("/") && !entryPath.startsWith("/")) {
            resourcePath += "/";
        }
        resourcePath += entryPath;

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IOException("Resource not found in classpath: " + resourcePath);
        }

        return readEntry(is);
    }

    /**
     * Open a stream from classpath.
     *
     * @param jarPath Path to the JAR (as classpath resource)
     * @param entryPath Path to the entry within the JAR
     * @return InputStream
     * @throws IOException if opening fails
     */
    private InputStream openStreamFromClasspath(String jarPath, String entryPath)
            throws IOException {
        // Construct the full classpath resource path
        String resourcePath = jarPath;
        if (!jarPath.endsWith("/") && !entryPath.startsWith("/")) {
            resourcePath += "/";
        }
        resourcePath += entryPath;

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IOException("Resource not found in classpath: " + resourcePath);
        }

        return is;
    }

    /**
     * Read content from an InputStream.
     *
     * @param inputStream Stream to read
     * @return String content
     * @throws IOException if reading fails
     */
    private String readEntry(InputStream inputStream) throws IOException {
        try (InputStreamReader reader =
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[8192];
            int charsRead;

            while ((charsRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, charsRead);
            }

            return content.toString();
        }
    }

    /**
     * Check if an embedded file exists.
     *
     * @param jarPath Path to the JAR file
     * @param entryPath Path to the entry within the JAR
     * @return true if file exists
     */
    public boolean embeddedFileExists(String jarPath, String entryPath) {
        if (jarPath == null || jarPath.trim().isEmpty()) {
            return false;
        }

        if (entryPath == null || entryPath.trim().isEmpty()) {
            return false;
        }

        // Try file system JAR first
        Path jarFilePath = java.nio.file.Paths.get(jarPath);
        if (Files.exists(jarFilePath)) {
            try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarFilePath.toFile())) {
                java.util.jar.JarEntry entry = jarFile.getJarEntry(entryPath);
                return entry != null;
            } catch (IOException e) {
                return false;
            }
        }

        // Try classpath
        try {
            String resourcePath = jarPath;
            if (!jarPath.endsWith("/") && !entryPath.startsWith("/")) {
                resourcePath += "/";
            }
            resourcePath += entryPath;

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return classLoader.getResource(resourcePath) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
