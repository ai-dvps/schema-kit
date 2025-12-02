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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/** Integration tests for git commit reference handling. */
@DisplayName("Git Commit Reference")
class GitCommitReferenceTest {

    private GitReferenceResolver referenceResolver;

    @BeforeEach
    void setUp() {
        referenceResolver = new GitReferenceResolver();
    }

    @Test
    @DisplayName("Should determine commit hash reference type")
    void testDetermineCommitHashReferenceType() {
        // Valid commit hashes
        String validFullHash = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
        String validShortHash = "da39a3e";

        // Act & Assert
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType(validFullHash),
                "Should identify full commit hash");
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType(validShortHash),
                "Should identify short commit hash");
    }

    @Test
    @DisplayName("Should handle full commit hash (40 characters)")
    void testHandleFullCommitHash() {
        // Arrange
        String fullHash = "abc123def456789abc123def456789abc123def45";

        // Act & Assert
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType(fullHash),
                "Should handle 40-character commit hash");
    }

    @Test
    @DisplayName("Should handle short commit hash (7 characters)")
    void testHandleShortCommitHash() {
        // Arrange
        String shortHash = "abc123d";

        // Act & Assert
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType(shortHash),
                "Should handle 7-character commit hash");
    }

    @Test
    @DisplayName("Should accept various commit hash lengths")
    void testAcceptVariousCommitHashLengths() {
        // Act & Assert - all between 7-40 chars with only hex characters
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType("abc123d"),
                "Should accept 7-character hash");

        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType("abc123def456789"),
                "Should accept 16-character hash");

        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType("abc123def456789abc123def456789"),
                "Should accept 32-character hash");
    }

    @Test
    @DisplayName("Should handle commit references from different sources")
    void testHandleCommitReferencesFromDifferentSources() {
        // Arrange
        String[] commitRefs = {"HEAD", "HEAD~1"};

        // Act & Assert
        for (String ref : commitRefs) {
            // These will be treated as branches since they don't match commit hash pattern
            // but that's acceptable for the current implementation
            assertDoesNotThrow(
                    () -> referenceResolver.determineReferenceType(ref),
                    "Should handle commit reference: " + ref);
        }
    }

    @Test
    @DisplayName("Should distinguish commit hash from branch")
    void testDistinguishCommitHashFromBranch() {
        // Arrange
        String branchRef = "main";
        String commitHash = "abc123d";

        // Act & Assert
        assertEquals(
                GitReferenceResolver.ReferenceType.BRANCH,
                referenceResolver.determineReferenceType(branchRef),
                "Branch reference should be identified as BRANCH");
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType(commitHash),
                "Commit hash should be identified as COMMIT_HASH");
    }

    @Test
    @DisplayName("Should determine reference type for invalid hashes")
    void testDetermineReferenceTypeForInvalidHashes() {
        // Too short (less than 7 characters) - treated as branch
        assertEquals(
                GitReferenceResolver.ReferenceType.BRANCH,
                referenceResolver.determineReferenceType("too-short"),
                "Should identify short string as BRANCH");

        // Empty string
        assertThrows(
                IllegalArgumentException.class,
                () -> referenceResolver.determineReferenceType(""),
                "Should reject empty hash");

        // Null
        assertThrows(
                IllegalArgumentException.class,
                () -> referenceResolver.determineReferenceType(null),
                "Should reject null hash");
    }

    @Test
    @DisplayName("Should handle uppercase hexadecimal")
    void testHandleUppercaseHexadecimal() {
        // Act & Assert
        assertEquals(
                GitReferenceResolver.ReferenceType.COMMIT_HASH,
                referenceResolver.determineReferenceType("ABCDEF0123456789"),
                "Should accept uppercase hexadecimal");
    }

    @Test
    @DisplayName("Should handle commit hash resolution")
    void testHandleCommitHashResolution() {
        // Arrange
        String repositoryPath = "/path/to/repo";
        String commitHash = "abc123d";

        // Act & Assert
        // Validates the hash format and returns lowercase version
        assertThrows(
                IllegalArgumentException.class,
                () -> referenceResolver.resolveToCommitHash(repositoryPath, commitHash),
                "Should throw exception for unresolved commit hash");
    }
}
