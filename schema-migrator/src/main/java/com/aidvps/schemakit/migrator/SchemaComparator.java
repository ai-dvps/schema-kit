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

package com.aidvps.schemakit.migrator;

import com.aidvps.druid.differ.internal.model.Database;
import com.aidvps.druid.differ.internal.model.Schema;
import com.aidvps.druid.differ.internal.model.Table;
import java.util.HashSet;
import java.util.Set;

/** Compares two database schemas and generates a detailed diff. */
public class SchemaComparator {
    /**
     * Compare two schemas and return all differences.
     *
     * @param source Source schema
     * @param target Target schema
     * @return SchemaDiff containing all differences
     */
    public SchemaDiff compare(Schema source, Schema target) {
        // Create a default database to hold all tables (backward compatibility)
        Database sourceDb = createDatabase("default", source);
        Database targetDb = createDatabase("default", target);

        DatabaseDiff dbDiff =
                DatabaseDiff.builder()
                        .databaseName("default")
                        .source(sourceDb)
                        .target(targetDb)
                        .build();

        Set<TableDiff> tableDiffs = new HashSet<>();

        // Compare tables within the database
        Set<String> allTableNames = new HashSet<>();
        allTableNames.addAll(source.getTables().keySet());
        allTableNames.addAll(target.getTables().keySet());

        // Compare each table
        for (String tableName : allTableNames) {
            Table sourceTable = source.getTables().get(tableName);
            Table targetTable = target.getTables().get(tableName);

            if (sourceTable == null) {
                // Table created
                TableDiff diff =
                        TableDiff.builder()
                                .tableName(tableName)
                                .isNew(true)
                                .target(targetTable)
                                .build();
                dbDiff.getTableDiffs().add(diff);
            } else if (targetTable == null) {
                // Table dropped
                TableDiff diff =
                        TableDiff.builder()
                                .tableName(tableName)
                                .isDeleted(true)
                                .source(sourceTable)
                                .build();
                dbDiff.getTableDiffs().add(diff);
            } else {
                // Table exists in both - mark as modified
                TableDiff diff =
                        TableDiff.builder()
                                .tableName(tableName)
                                .isModified(true)
                                .source(sourceTable)
                                .target(targetTable)
                                .build();
                dbDiff.getTableDiffs().add(diff);
            }
        }

        return SchemaDiff.builder().databaseDiff(dbDiff).build();
    }

    /** Create a database from a schema (helper method). */
    private Database createDatabase(String name, Schema schema) {
        Database.Builder builder = Database.builder().name(name);
        for (Table table : schema.getTables().values()) {
            builder.table(table.getName(), table);
        }
        return builder.build();
    }
}
