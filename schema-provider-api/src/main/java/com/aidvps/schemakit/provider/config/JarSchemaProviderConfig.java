package com.aidvps.schemakit.provider.config;

import com.aidvps.schemakit.provider.ConfigValidationException;
import com.aidvps.schemakit.provider.SchemaProviderConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Configuration for JAR-embedded provider. */
public interface JarSchemaProviderConfig extends SchemaProviderConfig {
    /**
     * Get JAR file path or classpath pattern.
     *
     * @return JAR path or classpath resource path
     */
    String getJarPath();

    /**
     * Get base path in JAR for schema directories.
     *
     * @return Base path in JAR (e.g., "schemas/")
     */
    String getBasePath();

    /**
     * Get classloader for resource loading.
     *
     * @return ClassLoader (default: thread context classloader)
     */
    ClassLoader getClassLoader();

    /** Create builder. */
    static Builder builder() {
        return new Builder();
    }

    /** Builder for JarSchemaProviderConfig. */
    class Builder {
        private String jarPath;
        private String basePath;
        private ClassLoader classLoader;

        private Builder() {}

        /**
         * Set the JAR path.
         *
         * @param jarPath The JAR path
         * @return This builder
         */
        public Builder jarPath(String jarPath) {
            this.jarPath = jarPath;
            return this;
        }

        /**
         * Set the base path.
         *
         * @param basePath The base path in JAR
         * @return This builder
         */
        public Builder basePath(String basePath) {
            this.basePath = basePath;
            return this;
        }

        /**
         * Set the classloader.
         *
         * @param classLoader The ClassLoader
         * @return This builder
         */
        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Build the JarSchemaProviderConfig instance.
         *
         * @return The configuration instance
         * @throws ConfigValidationException if configuration is invalid
         */
        public JarSchemaProviderConfig build() throws ConfigValidationException {
            return new JarSchemaProviderConfigImpl(this);
        }
    }

    /** Implementation of JarSchemaProviderConfig. */
    class JarSchemaProviderConfigImpl implements JarSchemaProviderConfig {
        private final String jarPath;
        private final String basePath;
        private final ClassLoader classLoader;

        private JarSchemaProviderConfigImpl(Builder builder) throws ConfigValidationException {
            this.jarPath = builder.jarPath;
            this.basePath = builder.basePath != null ? builder.basePath : "";
            this.classLoader =
                    builder.classLoader != null
                            ? builder.classLoader
                            : Thread.currentThread().getContextClassLoader();

            // Validate in constructor
            doValidate();
        }

        private void doValidate() throws ConfigValidationException {
            if (jarPath == null || jarPath.trim().isEmpty()) {
                throw new ConfigValidationException("JAR path must not be null or empty");
            }
        }

        @Override
        public String getJarPath() {
            return jarPath;
        }

        @Override
        public String getBasePath() {
            return basePath;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("jarPath", jarPath);
            map.put("basePath", basePath);
            map.put("classLoader", classLoader != null ? classLoader.getClass().getName() : null);
            return Collections.unmodifiableMap(map);
        }

        @Override
        public void validate() throws ConfigValidationException {
            doValidate();
        }
    }
}
