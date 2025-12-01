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

import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for schema parsing performance.
 *
 * <p>This benchmark measures the time it takes to parse various schema sizes, helping identify
 * performance bottlenecks in the parsing layer.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 1,
        jvmArgs = {"-Xmx2G"})
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class SchemaParsingBenchmark {

    @Param({"10", "50", "100"})
    private int tableCount;

    private DruidParserAdapter parserAdapter;
    private String[] testSchemas;

    @Setup
    public void setup() {
        parserAdapter = new DruidParserAdapter("mysql");
        testSchemas = generateTestSchemas(tableCount);
    }

    @Benchmark
    public Schema parseSchemas() throws Exception {
        Schema result = null;
        for (String schema : testSchemas) {
            result = parserAdapter.parseSchema(schema);
        }
        return result;
    }

    /** Generates test schemas of varying sizes. */
    private String[] generateTestSchemas(int count) {
        String[] schemas = new String[count];
        for (int i = 0; i < count; i++) {
            schemas[i] = generateSingleTableSchema("table_" + i);
        }
        return schemas;
    }

    /** Generates a single table schema SQL. */
    private String generateSingleTableSchema(String tableName) {
        return String.format(
                "CREATE TABLE %s (\n"
                        + "    id INT PRIMARY KEY AUTO_INCREMENT,\n"
                        + "    name VARCHAR(255) NOT NULL,\n"
                        + "    email VARCHAR(255),\n"
                        + "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
                        + "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n"
                        + "    INDEX idx_name (name),\n"
                        + "    INDEX idx_email (email)\n"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Test table';",
                tableName);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder()
                        .include(SchemaParsingBenchmark.class.getSimpleName())
                        .resultFormat(ResultFormatType.JSON)
                        .build();

        new Runner(opt).run();
    }
}
