package com.aidvps.schemakit.provider;

import com.aidvps.druid.differ.internal.model.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Testing kit for custom schema provider implementations.
 *
 * <p>Provides utilities to validate custom providers against the contract: - Configuration
 * validation - Schema retrieval - Error handling - Thread safety - Resource management
 *
 * <p>Usage:
 *
 * <pre>
 * CustomProviderTestKit.testProvider(new MyCustomProvider())
 *     .withValidConfig(myConfig)
 *     .withInvalidConfig(invalidConfig)
 *     .runAllTests();
 * </pre>
 */
public class CustomProviderTestKit {

    private final SchemaProvider provider;
    private SchemaProviderConfig validConfig;
    private SchemaProviderConfig invalidConfig;
    private int timeoutSeconds = 30;
    private boolean skipThreadSafetyTest = false;

    private CustomProviderTestKit(SchemaProvider provider) {
        this.provider = provider;
    }

    /**
     * Create a test kit for the given provider.
     *
     * @param provider Provider to test
     * @return Test kit instance
     */
    public static CustomProviderTestKit testProvider(SchemaProvider provider) {
        return new CustomProviderTestKit(provider);
    }

    /**
     * Set a valid configuration for testing.
     *
     * @param config Valid configuration
     * @return This test kit for chaining
     */
    public CustomProviderTestKit withValidConfig(SchemaProviderConfig config) {
        this.validConfig = config;
        return this;
    }

    /**
     * Set an invalid configuration for testing.
     *
     * @param config Invalid configuration
     * @return This test kit for chaining
     */
    public CustomProviderTestKit withInvalidConfig(SchemaProviderConfig config) {
        this.invalidConfig = config;
        return this;
    }

    /**
     * Set timeout for operations.
     *
     * @param seconds Timeout in seconds
     * @return This test kit for chaining
     */
    public CustomProviderTestKit withTimeout(int seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }

    /**
     * Skip thread safety tests.
     *
     * @return This test kit for chaining
     */
    public CustomProviderTestKit skipThreadSafetyTest() {
        this.skipThreadSafetyTest = true;
        return this;
    }

    /**
     * Run all contract validation tests.
     *
     * @return Test results
     * @throws TestFailureException if any test fails
     */
    public TestResults runAllTests() throws TestFailureException {
        TestResults results = new TestResults();

        // Test 1: Provider type
        try {
            Objects.requireNonNull(provider.getType(), "Provider type must not be null");
            results.addSuccess("Provider type is valid");
        } catch (Exception e) {
            results.addFailure("Provider type validation failed", e);
        }

        // Test 2: Configuration validation
        if (invalidConfig != null) {
            try {
                provider.validateConfig(invalidConfig);
                results.addFailure("Should reject invalid configuration", null);
            } catch (SchemaProviderException e) {
                results.addSuccess("Invalid configuration properly rejected");
            } catch (Exception e) {
                results.addFailure("Wrong exception type for invalid config", e);
            }
        }

        // Test 3: Schema retrieval with valid config
        if (validConfig != null) {
            try {
                long startTime = System.currentTimeMillis();
                Schema schema = provider.getSchema(validConfig);
                long duration = System.currentTimeMillis() - startTime;

                Objects.requireNonNull(schema, "Schema must not be null");
                Objects.requireNonNull(schema.getDialect(), "Schema dialect must not be null");
                Objects.requireNonNull(schema.getTables(), "Schema tables must not be null");

                results.addSuccess("Schema retrieval successful (" + duration + "ms)");

                // Performance test
                if (duration > timeoutSeconds * 1000) {
                    results.addFailure("Schema retrieval took too long: " + duration + "ms", null);
                }

            } catch (Exception e) {
                results.addFailure("Schema retrieval failed", e);
            }
        }

        // Test 4: Null configuration handling
        try {
            provider.getSchema(null);
            results.addFailure("Should reject null configuration", null);
        } catch (SchemaProviderException e) {
            results.addSuccess("Null configuration properly rejected");
        } catch (Exception e) {
            results.addFailure("Wrong exception type for null config", e);
        }

        // Test 5: Thread safety (if not skipped)
        if (!skipThreadSafetyTest) {
            runThreadSafetyTest(results);
        }

        return results;
    }

    /** Run only schema retrieval test. */
    public void testSchemaRetrieval() throws TestFailureException {
        if (validConfig == null) {
            throw new TestFailureException("Valid configuration not set");
        }

        try {
            Schema schema = provider.getSchema(validConfig);
            if (schema == null) {
                throw new TestFailureException("Schema is null");
            }
        } catch (SchemaProviderException e) {
            throw new TestFailureException("Failed to retrieve schema", e);
        }
    }

    /** Run only configuration validation test. */
    public void testConfigurationValidation() throws TestFailureException {
        if (invalidConfig == null) {
            throw new TestFailureException("Invalid configuration not set");
        }

        try {
            provider.validateConfig(invalidConfig);
            throw new TestFailureException("Should reject invalid configuration");
        } catch (SchemaProviderException e) {
            // Expected
        }
    }

    private void runThreadSafetyTest(TestResults results) {
        final int threadCount = 10;
        final int iterationsPerThread = 10;

        Thread[] threads = new Thread[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    for (int j = 0; j < iterationsPerThread; j++) {
                                        if (validConfig != null) {
                                            provider.getSchema(validConfig);
                                        }
                                        provider.getType();
                                    }
                                } catch (Exception e) {
                                    exceptions[threadIndex] = e;
                                }
                            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.addFailure("Thread safety test interrupted", e);
                return;
            }
        }

        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                results.addFailure("Thread safety test failed in thread " + i, exceptions[i]);
                return;
            }
        }

        results.addSuccess(
                "Thread safety test passed ("
                        + threadCount
                        + " threads, "
                        + iterationsPerThread
                        + " iterations each)");
    }

    /** Test results container. */
    public static class TestResults {
        private final List<String> successes = new ArrayList<>();
        private final List<Failure> failures = new ArrayList<>();

        private void addSuccess(String message) {
            successes.add(message);
        }

        private void addFailure(String message, Exception cause) {
            failures.add(new Failure(message, cause));
        }

        public boolean hasFailures() {
            return !failures.isEmpty();
        }

        public int getSuccessCount() {
            return successes.size();
        }

        public int getFailureCount() {
            return failures.size();
        }

        public List<String> getSuccesses() {
            return new ArrayList<>(successes);
        }

        public List<Failure> getFailures() {
            return new ArrayList<>(failures);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Test Results:\n");
            sb.append("  Successes: ").append(getSuccessCount()).append("\n");
            sb.append("  Failures: ").append(getFailureCount()).append("\n");

            if (!failures.isEmpty()) {
                sb.append("\nFailures:\n");
                for (Failure failure : failures) {
                    sb.append("  - ").append(failure.message);
                    if (failure.cause != null) {
                        sb.append(": ").append(failure.cause.getMessage());
                    }
                    sb.append("\n");
                }
            }

            return sb.toString();
        }

        public static class Failure {
            private final String message;
            private final Exception cause;

            private Failure(String message, Exception cause) {
                this.message = message;
                this.cause = cause;
            }

            public String getMessage() {
                return message;
            }

            public Exception getCause() {
                return cause;
            }
        }
    }

    /** Exception thrown when tests fail. */
    public static class TestFailureException extends Exception {
        public TestFailureException(String message) {
            super(message);
        }

        public TestFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
