package com.aidvps.schemakit.provider;

/** Thrown when a secret key is not found. */
public class SecretNotFoundException extends Exception {
    private final String key;

    /**
     * Construct a new exception with the specified key.
     *
     * @param key The secret key
     */
    public SecretNotFoundException(String key) {
        super("Secret not found: " + key);
        this.key = key;
    }

    /**
     * Construct a new exception with the specified key and message.
     *
     * @param key The secret key
     * @param message The detail message
     */
    public SecretNotFoundException(String key, String message) {
        super(message);
        this.key = key;
    }

    /**
     * Get the secret key.
     *
     * @return The secret key
     */
    public String getKey() {
        return key;
    }
}
