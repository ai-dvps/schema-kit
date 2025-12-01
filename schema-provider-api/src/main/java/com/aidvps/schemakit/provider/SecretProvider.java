package com.aidvps.schemakit.provider;

/** Interface for credential retrieval. */
public interface SecretProvider {
    /**
     * Get a secret value.
     *
     * @param key Secret key
     * @return Secret value
     * @throws SecretNotFoundException if key doesn't exist
     */
    String getSecret(String key) throws SecretNotFoundException;

    /**
     * Check if secret exists.
     *
     * @param key Secret key
     * @return true if exists
     */
    boolean hasSecret(String key);
}
