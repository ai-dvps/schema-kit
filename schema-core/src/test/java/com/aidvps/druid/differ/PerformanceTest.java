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

package com.aidvps.druid.differ;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Performance tests for SQL Schema Differ.
 *
 * <p>These tests verify that the schema differ meets performance requirements for CI/CD
 * integration.
 */
public class PerformanceTest {

    private TableDiffer tableDiffer;

    @BeforeEach
    public void setup() {
        tableDiffer = TableDiffer.builder().withDialect(DatabaseDialect.MYSQL).build();
    }

    /**
     * Test that 100-table schema comparison completes in less than 5 seconds.
     *
     * <p>This is a critical performance requirement for CI/CD integration.
     */
    @Test
    public void test100TableSchemaComparisonPerformance() throws Exception {
        final int tableCount = 100;
        final long maxDurationMillis = 5000;

        String[] sourceSchemas = new String[tableCount];
        String[] targetSchemas = new String[tableCount];

        for (int i = 0; i < tableCount; i++) {
            sourceSchemas[i] = generateSimpleSchema(i);
            targetSchemas[i] = generateComplexSchema(i);
        }

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < tableCount; i++) {
            tableDiffer.generateMigration(sourceSchemas[i], targetSchemas[i]);
        }

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(
                duration < maxDurationMillis,
                String.format(
                        "100-table schema comparison took %dms, expected <%dms",
                        duration, maxDurationMillis));
    }

    /**
     * Test that 1000-table schema parsing completes in less than 10 seconds.
     *
     * <p>This verifies that schema parsing scales linearly with table count.
     */
    @Test
    public void test1000TableSchemaParsingPerformance() throws Exception {
        final int tableCount = 1000;
        final long maxDurationMillis = 10000;

        String[] schemas = new String[tableCount];
        for (int i = 0; i < tableCount; i++) {
            schemas[i] = generateSimpleSchema(i);
        }

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < tableCount; i++) {
            tableDiffer.generateMigration(schemas[i], schemas[i]);
        }

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(
                duration < maxDurationMillis,
                String.format(
                        "1000-table schema parsing took %dms, expected <%dms",
                        duration, maxDurationMillis));
    }

    /**
     * Test that memory usage grows linearly with table count.
     *
     * <p>This verifies that there are no unexpected memory usage patterns that could cause problems
     * with large schemas.
     */
    @Test
    public void testLinearMemoryGrowth() throws Exception {
        Runtime runtime = Runtime.getRuntime();

        // Test with 10 tables
        long memory10 = measureMemoryUsage(10, runtime);

        // Test with 100 tables
        long memory100 = measureMemoryUsage(100, runtime);

        // Memory should grow roughly linearly (within 2x tolerance)
        double growthRatio = (double) memory100 / memory10;
        double expectedGrowthRatio = 10.0; // 100 tables / 10 tables

        assertTrue(
                growthRatio < expectedGrowthRatio * 2,
                String.format(
                        "Memory growth ratio %.2f exceeds linear growth %.2f by more than 2x",
                        growthRatio, expectedGrowthRatio));
    }

    /**
     * Test that repeated operations don't cause memory leaks.
     *
     * <p>This verifies that repeated schema comparisons don't accumulate memory over time.
     */
    @Test
    public void testNoMemoryLeaksInRepeatedOperations() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        final int iterations = 50;
        final int tablesPerIteration = 10;

        // Warmup
        for (int i = 0; i < 10; i++) {
            performSchemaComparison(tablesPerIteration);
        }

        // Force GC and measure baseline
        System.gc();
        Thread.sleep(100);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

        // Perform repeated operations
        for (int i = 0; i < iterations; i++) {
            performSchemaComparison(tablesPerIteration);
        }

        // Force GC and measure final memory
        System.gc();
        Thread.sleep(100);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();

        long memoryGrowth = finalMemory - baselineMemory;
        long maxAcceptableGrowth = 10 * 1024 * 1024; // 10 MB

        assertTrue(
                memoryGrowth < maxAcceptableGrowth,
                String.format(
                        "Memory grew by %d bytes after %d iterations, expected <%d bytes",
                        memoryGrowth, iterations, maxAcceptableGrowth));
    }

    /** Measures memory usage for processing a given number of tables. */
    private long measureMemoryUsage(int tableCount, Runtime runtime) throws Exception {
        // Force GC before measurement
        System.gc();
        Thread.sleep(100);

        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        performSchemaComparison(tableCount);

        // Force GC to get accurate measurement
        System.gc();
        Thread.sleep(100);

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();

        return afterMemory - beforeMemory;
    }

    /** Performs schema comparison for a given number of tables. */
    private void performSchemaComparison(int tableCount) throws Exception {
        for (int i = 0; i < tableCount; i++) {
            String source = generateSimpleSchema(i);
            String target = generateComplexSchema(i);
            tableDiffer.generateMigration(source, target);
        }
    }

    /** Generates a simple table schema for testing. */
    private String generateSimpleSchema(int index) {
        return String.format(
                "CREATE TABLE table_%d (\n"
                        + "    id INT PRIMARY KEY,\n"
                        + "    name VARCHAR(100)\n"
                        + ");",
                index);
    }

    /** Generates a complex table schema for testing. */
    private String generateComplexSchema(int index) {
        return String.format(
                "CREATE TABLE table_%d (\n"
                        + "    id INT PRIMARY KEY AUTO_INCREMENT,\n"
                        + "    name VARCHAR(255) NOT NULL,\n"
                        + "    email VARCHAR(255) UNIQUE,\n"
                        + "    status INT DEFAULT 1,\n"
                        + "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
                        + "    INDEX idx_name (name),\n"
                        + "    INDEX idx_status (status)\n"
                        + ");",
                index);
    }
}
