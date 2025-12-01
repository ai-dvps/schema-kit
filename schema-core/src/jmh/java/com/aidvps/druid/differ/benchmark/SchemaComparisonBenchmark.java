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
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.SchemaDiff;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for schema comparison performance.
 *
 * <p>This benchmark measures the time it takes to compare schemas of different sizes, testing both
 * identical schemas (fast path) and different schemas (full comparison).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 1,
        jvmArgs = {"-Xmx2G"})
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
public class SchemaComparisonBenchmark {

    @Param({"10", "50", "100"})
    private int tableCount;

    @Param({"true", "false"})
    private boolean identicalSchemas;

    private ChangeDetector changeDetector;
    private Schema[] sourceSchemas;
    private Schema[] targetSchemas;

    @Setup
    public void setup() throws Exception {
        changeDetector = new ChangeDetector();
        sourceSchemas = generateSchemas(tableCount);
        targetSchemas =
                identicalSchemas ? sourceSchemas.clone() : generateModifiedSchemas(tableCount);
    }

    @Benchmark
    public SchemaDiff compareSchemas() {
        SchemaDiff result = null;
        for (int i = 0; i < sourceSchemas.length; i++) {
            result = changeDetector.compare(sourceSchemas[i], targetSchemas[i]);
        }
        return result;
    }

    /** Generates test schemas. */
    private Schema[] generateSchemas(int count) throws Exception {
        Schema[] schemas = new Schema[count];
        DruidParserAdapter parser = new DruidParserAdapter("mysql");

        for (int i = 0; i < count; i++) {
            String sql = generateTableSchema("table_" + i, false);
            schemas[i] = parser.parseSchema(sql);
        }
        return schemas;
    }

    /** Generates modified test schemas. */
    private Schema[] generateModifiedSchemas(int count) throws Exception {
        Schema[] schemas = new Schema[count];
        DruidParserAdapter parser = new DruidParserAdapter("mysql");

        for (int i = 0; i < count; i++) {
            // Modify every other table to simulate changes
            boolean addColumn = (i % 2) == 0;
            String sql = generateTableSchema("table_" + i, addColumn);
            schemas[i] = parser.parseSchema(sql);
        }
        return schemas;
    }

    /** Generates a table schema SQL. */
    private String generateTableSchema(String tableName, boolean addExtraColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");
        sb.append("    id INT PRIMARY KEY AUTO_INCREMENT,\n");
        sb.append("    name VARCHAR(255) NOT NULL,\n");
        sb.append("    email VARCHAR(255),\n");
        sb.append("    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");

        if (addExtraColumn) {
            sb.append(
                    ",\n    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n");
        }

        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        return sb.toString();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder()
                        .include(SchemaComparisonBenchmark.class.getSimpleName())
                        .resultFormat(ResultFormatType.JSON)
                        .build();

        new Runner(opt).run();
    }
}
