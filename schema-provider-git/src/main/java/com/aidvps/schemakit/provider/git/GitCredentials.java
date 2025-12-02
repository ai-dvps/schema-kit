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

import java.util.Objects;

/**
 * Credentials for authenticating with git repositories.
 *
 * <p>Supports both username/password and SSH key-based authentication.
 */
public class GitCredentials {

    private final String username;
    private final String password;
    private final String privateKeyPath;
    private final String passphrase;

    private GitCredentials(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.privateKeyPath = builder.privateKeyPath;
        this.passphrase = builder.passphrase;
    }

    /**
     * Create a new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the username for authentication.
     *
     * @return Username, may be null
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password for authentication.
     *
     * @return Password, may be null
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get the SSH private key path.
     *
     * @return Private key path, may be null
     */
    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    /**
     * Get the passphrase for the private key.
     *
     * @return Passphrase, may be null
     */
    public String getPassphrase() {
        return passphrase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GitCredentials that = (GitCredentials) o;
        return Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(privateKeyPath, that.privateKeyPath)
                && Objects.equals(passphrase, that.passphrase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, privateKeyPath, passphrase);
    }

    @Override
    public String toString() {
        return "GitCredentials{"
                + "username='"
                + username
                + '\''
                + ", password='"
                + (password != null ? "***" : null)
                + '\''
                + ", privateKeyPath='"
                + privateKeyPath
                + '\''
                + ", passphrase='"
                + (passphrase != null ? "***" : null)
                + '\''
                + '}';
    }

    /** Builder for GitCredentials. */
    public static class Builder {

        private String username;
        private String password;
        private String privateKeyPath;
        private String passphrase;

        private Builder() {}

        /**
         * Set the username.
         *
         * @param username Username
         * @return This builder
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the password.
         *
         * @param password Password
         * @return This builder
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Set the SSH private key path.
         *
         * @param privateKeyPath Private key path
         * @return This builder
         */
        public Builder privateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
            return this;
        }

        /**
         * Set the passphrase for the private key.
         *
         * @param passphrase Passphrase
         * @return This builder
         */
        public Builder passphrase(String passphrase) {
            this.passphrase = passphrase;
            return this;
        }

        /**
         * Build the GitCredentials.
         *
         * @return GitCredentials instance
         */
        public GitCredentials build() {
            return new GitCredentials(this);
        }
    }
}
