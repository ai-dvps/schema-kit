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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads resources from the classpath.
 *
 * <p>Handles loading of schema files from JARs that are available on the classpath. Supports both
 * direct resource loading and pattern-based resource discovery.
 */
public class ClasspathResourceLoader {

    private final ClassLoader classLoader;

    public ClasspathResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    // Constructor for testing
    ClasspathResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Load a resource from the classpath.
     *
     * @param resourcePath Path to the resource
     * @return Resource content as String
     * @throws IOException if loading fails
     */
    public String loadResource(String resourcePath) throws IOException {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path must not be null or empty");
        }

        // Remove leading slash if present
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            byte[] buffer = new byte[8192];
            StringBuilder content = new StringBuilder();
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }

            return content.toString();
        }
    }

    /**
     * Load multiple resources matching a pattern.
     *
     * @param pattern Resource pattern (supports ** wildcard)
     * @return List of resource contents
     * @throws IOException if loading fails
     */
    public List<String> loadResources(String pattern) throws IOException {
        List<String> resources = new ArrayList<>();

        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern must not be null or empty");
        }

        // Normalize pattern
        if (pattern.startsWith("/")) {
            pattern = pattern.substring(1);
        }

        try {
            Enumeration<URL> resourceUrls = classLoader.getResources(pattern);

            while (resourceUrls.hasMoreElements()) {
                URL resourceUrl = resourceUrls.nextElement();
                String content = readUrl(resourceUrl);
                if (content != null) {
                    resources.add(content);
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to load resources with pattern: " + pattern, e);
        }

        return resources;
    }

    /**
     * Check if a resource exists on the classpath.
     *
     * @param resourcePath Path to the resource
     * @return true if resource exists
     */
    public boolean resourceExists(String resourcePath) {
        if (resourcePath == null) {
            return false;
        }

        // Remove leading slash if present
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        URL resourceUrl = classLoader.getResource(resourcePath);
        return resourceUrl != null;
    }

    /**
     * Get all resources with a given prefix.
     *
     * @param prefix Resource name prefix
     * @return List of resource names
     * @throws IOException if enumeration fails
     */
    public List<String> getResourcesWithPrefix(String prefix) throws IOException {
        List<String> resourceNames = new ArrayList<>();

        if (prefix == null || prefix.trim().isEmpty()) {
            return resourceNames;
        }

        // Normalize prefix
        String searchPrefix = prefix;
        if (!searchPrefix.endsWith("/") && !searchPrefix.isEmpty()) {
            searchPrefix += "/";
        }
        searchPrefix += "*";

        try {
            Enumeration<URL> resourceUrls = classLoader.getResources(searchPrefix);

            while (resourceUrls.hasMoreElements()) {
                URL resourceUrl = resourceUrls.nextElement();
                String resourceName = extractResourceName(resourceUrl);
                resourceNames.add(resourceName);
            }
        } catch (IOException e) {
            throw new IOException("Failed to enumerate resources with prefix: " + prefix, e);
        }

        return resourceNames;
    }

    /**
     * Load resource as a file path if it exists on the file system.
     *
     * @param resourcePath Path to the resource
     * @return File path if resource is on filesystem, null otherwise
     * @throws IOException if access fails
     */
    public Path loadResourceAsFile(String resourcePath) throws IOException {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path must not be null or empty");
        }

        // Remove leading slash if present
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        URL resourceUrl = classLoader.getResource(resourcePath);

        if (resourceUrl == null) {
            return null;
        }

        // Only return file path if it's actually a file on the filesystem
        if ("file".equals(resourceUrl.getProtocol())) {
            try {
                return Paths.get(resourceUrl.toURI());
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Get the class loader being used.
     *
     * @return ClassLoader instance
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Read content from a URL.
     *
     * @param resourceUrl URL to read
     * @return String content
     * @throws IOException if reading fails
     */
    private String readUrl(URL resourceUrl) throws IOException {
        try (InputStream is = resourceUrl.openStream()) {
            byte[] buffer = new byte[8192];
            StringBuilder content = new StringBuilder();
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }

            return content.toString();
        }
    }

    /**
     * Extract resource name from a URL.
     *
     * @param resourceUrl URL to process
     * @return Resource name
     */
    private String extractResourceName(URL resourceUrl) {
        String path = resourceUrl.getPath();

        // Handle both file:// and jar: protocols
        if (path.contains("!")) {
            // JAR URL format: jar:file:/path/to/file.jar!/path/inside/jar
            int separatorIndex = path.indexOf('!');
            if (separatorIndex >= 0 && separatorIndex < path.length() - 1) {
                path = path.substring(separatorIndex + 1);
            }
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            path = path.substring(lastSlash + 1);
        }

        return path;
    }
}
