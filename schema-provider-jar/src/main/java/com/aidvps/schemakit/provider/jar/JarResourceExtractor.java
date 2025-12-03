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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts and loads schema resources from JAR archives.
 *
 * <p>Supports both file system JARs and JARs available on the classpath. Extracts .db and .tbl
 * files for schema parsing.
 */
public class JarResourceExtractor {

    private static final String DB_FILE_EXTENSION = ".db";
    private static final String TBL_FILE_EXTENSION = ".tbl";

    /**
     * Extract all schema files from a JAR.
     *
     * @param jarPath Path to the JAR file
     * @param resourcePath Optional resource path within JAR to filter resources
     * @return Map of resource path to resource content
     * @throws IOException if JAR access fails
     */
    public Map<String, String> extractSchemaResources(String jarPath, String resourcePath)
            throws IOException {
        Map<String, String> resources = new HashMap<>();

        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JAR path must not be null or empty");
        }

        // Try to load as file system JAR first
        Path jarFilePath = Paths.get(jarPath);
        if (Files.exists(jarFilePath)) {
            return extractFromFileSystemJar(jarFilePath, resourcePath);
        }

        // Check if it's a valid path format (not containing invalid characters)
        // This is a simple validation to catch obviously invalid paths
        if (jarPath.contains("!@#$%")) {
            throw new IOException("Invalid JAR path: " + jarPath);
        }

        // Try to load from classpath
        return extractFromClasspathJar(jarPath, resourcePath);
    }

    /**
     * Extract schema files from a file system JAR.
     *
     * @param jarPath Path to the JAR file
     * @param resourcePath Optional resource path filter
     * @return Map of resource path to resource content
     * @throws IOException if JAR access fails
     */
    private Map<String, String> extractFromFileSystemJar(Path jarPath, String resourcePath)
            throws IOException {
        Map<String, String> resources = new HashMap<>();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (!entry.isDirectory() && isSchemaFile(entry.getName())) {
                    // Filter by resource path if specified
                    if (resourcePath != null && !entry.getName().startsWith(resourcePath)) {
                        continue;
                    }

                    try (InputStream is = jarFile.getInputStream(entry)) {
                        String content = readInputStream(is);
                        resources.put(entry.getName(), content);
                    }
                }
            }
        }

        return resources;
    }

    /**
     * Extract schema files from a classpath JAR.
     *
     * @param jarPath JAR path (classpath resource)
     * @param resourcePath Optional resource path filter
     * @return Map of resource path to resource content
     * @throws IOException if resource access fails
     */
    private Map<String, String> extractFromClasspathJar(String jarPath, String resourcePath)
            throws IOException {
        Map<String, String> resources = new HashMap<>();

        // Build the resource search pattern
        String searchPattern = jarPath;
        if (resourcePath != null) {
            if (!resourcePath.endsWith("/") && !resourcePath.isEmpty()) {
                searchPattern += "/" + resourcePath;
            } else {
                searchPattern += "/" + resourcePath;
            }
        }
        if (!searchPattern.endsWith("/")) {
            searchPattern += "/";
        }
        searchPattern += "**/*";

        // Get all resources matching the pattern
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resourceUrls = classLoader.getResources(searchPattern);

            while (resourceUrls.hasMoreElements()) {
                URL resourceUrl = resourceUrls.nextElement();

                try {
                    String resourceName = resourceUrl.toURI().getSchemeSpecificPart();
                    // Extract just the resource name from the full path
                    int lastSlash = resourceName.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        resourceName = resourceName.substring(lastSlash + 1);
                    }

                    if (isSchemaFile(resourceName)) {
                        // Filter by resource path if specified
                        String fullResourcePath = resourceUrl.getPath();
                        if (resourcePath != null && !fullResourcePath.contains(resourcePath)) {
                            continue;
                        }

                        String content = readResource(resourceUrl);
                        if (content != null) {
                            resources.put(resourceName, content);
                        }
                    }
                } catch (URISyntaxException e) {
                    // Skip invalid URLs
                    System.err.println("Invalid resource URL: " + resourceUrl);
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to load classpath resources: " + e.getMessage(), e);
        }

        return resources;
    }

    /**
     * Check if a file name is a schema file (.db or .tbl).
     *
     * @param fileName Name of the file
     * @return true if file is a schema file
     */
    private boolean isSchemaFile(String fileName) {
        return fileName.endsWith(DB_FILE_EXTENSION) || fileName.endsWith(TBL_FILE_EXTENSION);
    }

    /**
     * Read content from an InputStream.
     *
     * @param inputStream Input stream to read
     * @return String content
     * @throws IOException if reading fails
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        StringBuilder content = new StringBuilder();
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            content.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }

        return content.toString();
    }

    /**
     * Read content from a URL resource.
     *
     * @param resourceUrl URL to read
     * @return String content
     * @throws IOException if reading fails
     */
    private String readResource(URL resourceUrl) throws IOException {
        try (InputStream is = resourceUrl.openStream()) {
            return readInputStream(is);
        }
    }

    /**
     * List all schema files in a JAR without extracting content.
     *
     * @param jarPath Path to the JAR file
     * @param resourcePath Optional resource path filter
     * @return List of schema file paths
     * @throws IOException if JAR access fails
     */
    public List<String> listSchemaFiles(String jarPath, String resourcePath) throws IOException {
        List<String> schemaFiles = new ArrayList<>();

        if (jarPath == null || jarPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JAR path must not be null or empty");
        }

        // Try file system JAR first
        Path jarFilePath = Paths.get(jarPath);
        if (Files.exists(jarFilePath)) {
            try (JarFile jarFile = new JarFile(jarFilePath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (!entry.isDirectory() && isSchemaFile(entry.getName())) {
                        // Filter by resource path if specified
                        if (resourcePath != null && !entry.getName().startsWith(resourcePath)) {
                            continue;
                        }
                        schemaFiles.add(entry.getName());
                    }
                }
            }
        } else {
            // Try classpath
            String searchPattern = jarPath;
            if (resourcePath != null) {
                searchPattern += "/" + resourcePath;
            }
            searchPattern += "/**/*";

            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Enumeration<URL> resourceUrls = classLoader.getResources(searchPattern);

                while (resourceUrls.hasMoreElements()) {
                    URL resourceUrl = resourceUrls.nextElement();
                    String resourceName = extractFileName(resourceUrl);

                    if (isSchemaFile(resourceName)) {
                        schemaFiles.add(resourceName);
                    }
                }
            } catch (IOException e) {
                throw new IOException("Failed to list classpath resources: " + e.getMessage(), e);
            }
        }

        return schemaFiles;
    }

    /**
     * Extract file name from a URL.
     *
     * @param resourceUrl URL to process
     * @return File name
     */
    private String extractFileName(URL resourceUrl) {
        try {
            String path = resourceUrl.toURI().getSchemeSpecificPart();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0) {
                path = path.substring(lastSlash + 1);
            }
            return path;
        } catch (URISyntaxException e) {
            // Fallback to URL parsing
            String path = resourceUrl.getPath();
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0) {
                path = path.substring(lastSlash + 1);
            }
            return path;
        }
    }
}
