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

package com.aidvps.druid.differ.benchmark;

import com.aidvps.druid.differ.DatabaseDialect;
import com.aidvps.druid.differ.TableDiffer;
import com.aidvps.druid.differ.internal.model.MigrationPlan;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * End-to-end benchmark for complete migration generation workflow.
 *
 * <p>This benchmark measures the total time for the entire process: - Parsing source schema -
 * Parsing target schema - Comparing schemas - Generating SQL - Generating rollback SQL
 *
 * <p>Target: Complete 100-table schema comparison in <5 seconds
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 1,
        jvmArgs = {"-Xmx2G"})
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class EndToEndBenchmark {

    @Param({"10", "50", "100"})
    private int tableCount;

    @Param({"true", "false"})
    private boolean includeRollback;

    private TableDiffer tableDiffer;
    private String[] sourceSchemas;
    private String[] targetSchemas;

    @Setup
    public void setup() {
        tableDiffer = TableDiffer.builder().withDialect(DatabaseDialect.MYSQL).build();

        sourceSchemas = new String[tableCount];
        targetSchemas = new String[tableCount];

        for (int i = 0; i < tableCount; i++) {
            sourceSchemas[i] = generateSimpleSchema(i);
            targetSchemas[i] = generateComplexSchema(i);
        }
    }

    @Benchmark
    public MigrationPlan generateMigration() throws Exception {
        MigrationPlan result = null;
        for (int i = 0; i < tableCount; i++) {
            result = tableDiffer.generateMigration(sourceSchemas[i], targetSchemas[i]);
        }
        return result;
    }

    @Benchmark
    public List<String> generateMigrationWithRollback() throws Exception {
        List<String> result = null;
        for (int i = 0; i < tableCount; i++) {
            MigrationPlan plan = tableDiffer.generateMigration(sourceSchemas[i], targetSchemas[i]);
            if (includeRollback) {
                result = tableDiffer.generateRollback(plan);
            }
        }
        return result;
    }

    /** Generates a simple schema. */
    private String generateSimpleSchema(int index) {
        return String.format(
                "CREATE TABLE table_%d (\n"
                        + "    id INT PRIMARY KEY,\n"
                        + "    name VARCHAR(100)\n"
                        + ");",
                index);
    }

    /** Generates a complex schema with changes. */
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

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder()
                        .include(EndToEndBenchmark.class.getSimpleName())
                        .resultFormat(ResultFormatType.JSON)
                        .build();

        new Runner(opt).run();
    }
}
