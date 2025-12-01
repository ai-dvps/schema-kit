package com.aidvps.schemakit.provider;

/** Built-in implementation using environment variables. */
public class EnvironmentVariableSecretProvider implements SecretProvider {
    /** Default constructor using System.getenv(). */
    public EnvironmentVariableSecretProvider() {
        // Uses System.getenv()
    }

    @Override
    public String getSecret(String key) throws SecretNotFoundException {
        String value = System.getenv(key);
        if (value == null) {
            throw new SecretNotFoundException(key);
        }
        return value;
    }

    @Override
    public boolean hasSecret(String key) {
        return System.getenv().containsKey(key);
    }
}
