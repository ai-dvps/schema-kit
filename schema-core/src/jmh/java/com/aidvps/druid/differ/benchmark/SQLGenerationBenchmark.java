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

import com.aidvps.druid.differ.internal.comparator.ChangeDetector;
import com.aidvps.druid.differ.internal.generator.MySQLMigrationGenerator;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for SQL generation performance.
 *
 * <p>This benchmark measures the time it takes to generate SQL migration statements from schema
 * diffs of varying complexity.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 1,
        jvmArgs = {"-Xmx2G"})
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class SQLGenerationBenchmark {

    @Param({"10", "50", "100"})
    private int tableCount;

    @Param({"false", "true"})
    private boolean includeComments;

    private MySQLMigrationGenerator migrationGenerator;
    private SchemaDiff[] diffs;

    @Setup
    public void setup() throws Exception {
        migrationGenerator = new MySQLMigrationGenerator(includeComments);
        diffs = generateSchemaDiffs(tableCount);
    }

    @Benchmark
    public List<String> generateSQL() {
        List<String> result = null;
        for (SchemaDiff diff : diffs) {
            result = migrationGenerator.generate(diff);
        }
        return result;
    }

    /** Generates test schema diffs with varying complexity. */
    private SchemaDiff[] generateSchemaDiffs(int count) throws Exception {
        SchemaDiff[] diffs = new SchemaDiff[count];
        ChangeDetector changeDetector = new ChangeDetector();
        DruidParserAdapter parser = new DruidParserAdapter("mysql");

        for (int i = 0; i < count; i++) {
            String sourceSql = generateSimpleSchema("table_" + i);
            String targetSql = generateComplexSchema("table_" + i);

            Schema sourceSchema = parser.parseSchema(sourceSql);
            Schema targetSchema = parser.parseSchema(targetSql);

            diffs[i] = changeDetector.compare(sourceSchema, targetSchema);
        }

        return diffs;
    }

    /** Generates a simple schema. */
    private String generateSimpleSchema(String tableName) {
        return String.format(
                "CREATE TABLE %s (\n"
                        + "    id INT PRIMARY KEY,\n"
                        + "    name VARCHAR(100)\n"
                        + ");",
                tableName);
    }

    /** Generates a more complex schema. */
    private String generateComplexSchema(String tableName) {
        return String.format(
                "CREATE TABLE %s (\n"
                        + "    id INT PRIMARY KEY AUTO_INCREMENT,\n"
                        + "    name VARCHAR(255) NOT NULL,\n"
                        + "    email VARCHAR(255) UNIQUE,\n"
                        + "    status INT DEFAULT 1,\n"
                        + "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
                        + "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n"
                        + "    INDEX idx_name (name),\n"
                        + "    INDEX idx_status (status),\n"
                        + "    CONSTRAINT fk_user FOREIGN KEY (status) REFERENCES statuses(id)\n"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
                tableName);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder()
                        .include(SQLGenerationBenchmark.class.getSimpleName())
                        .resultFormat(ResultFormatType.JSON)
                        .build();

        new Runner(opt).run();
    }
}
