package com.aidvps.schemakit.provider;

import java.util.Map;

/** Built-in implementation using environment variables. */
public class EnvironmentVariableSecretProvider implements SecretProvider {
    private final Map<String, String> environment;

    /** Default constructor using System.getenv(). */
    public EnvironmentVariableSecretProvider() {
        this(System.getenv());
    }

    /** Constructor for testing with custom environment map. */
    EnvironmentVariableSecretProvider(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public String getSecret(String key) throws SecretNotFoundException {
        if (key == null) {
            throw new NullPointerException("Key must not be null");
        }
        String value = environment.get(key);
        if (value == null) {
            throw new SecretNotFoundException(key);
        }
        return value;
    }

    @Override
    public boolean hasSecret(String key) {
        if (key == null) {
            return false;
        }
        return environment.containsKey(key);
    }
}
