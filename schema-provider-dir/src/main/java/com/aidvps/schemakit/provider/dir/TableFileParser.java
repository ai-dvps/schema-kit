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

package com.aidvps.schemakit.provider.dir;

import com.aidvps.druid.differ.exception.SchemaParsingException;
import com.aidvps.druid.differ.internal.model.Table;
import com.aidvps.druid.differ.internal.parser.DruidParserAdapter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses .tbl files containing CREATE TABLE statements. Each .tbl file should contain a single
 * CREATE TABLE statement that defines a table.
 */
public class TableFileParser {

    private static final String TBL_FILE_EXTENSION = ".tbl";

    /**
     * Parses all .tbl files in the given directory and extracts table definitions.
     *
     * @param directoryPath Path to the directory containing .tbl files
     * @return Map of table name to Table object
     * @throws IOException if reading files fails
     */
    public Map<String, Table> parseTableFiles(Path directoryPath) throws IOException {
        Map<String, Table> tables = new HashMap<>();

        Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(TBL_FILE_EXTENSION))
                .forEach(
                        tblFile -> {
                            try {
                                Table table = parseTableFile(tblFile);
                                if (table != null) {
                                    tables.put(table.getName(), table);
                                }
                            } catch (Exception e) {
                                // Log error but continue processing other files
                                System.err.println(
                                        "Error parsing table file "
                                                + tblFile
                                                + ": "
                                                + e.getMessage());
                            }
                        });

        return tables;
    }

    /**
     * Parses a single .tbl file to extract the table definition.
     *
     * @param tblFile Path to the .tbl file
     * @return Table object, or null if parsing fails
     * @throws IOException if reading file fails
     */
    public Table parseTableFile(Path tblFile) throws IOException {
        String content = new String(Files.readAllBytes(tblFile), StandardCharsets.UTF_8);
        return parseTableFromContent(content);
    }

    /**
     * Parses CREATE TABLE statement from SQL content and converts to internal Table model.
     *
     * @param content SQL content containing CREATE TABLE statement
     * @return Table object, or null if parsing fails
     */
    public Table parseTableFromContent(String content) {
        try {
            // Use DruidParserAdapter to properly parse the table
            DruidParserAdapter parser = new DruidParserAdapter("mysql");
            com.aidvps.druid.differ.internal.model.Schema schema = parser.parseSchema(content);

            // Extract the first (and should be only) table from the schema
            if (!schema.getTables().isEmpty()) {
                String tableName = schema.getTables().keySet().iterator().next();
                return schema.getTables().get(tableName);
            }

            return null;
        } catch (SchemaParsingException e) {
            // Log error but continue
            System.err.println("Error parsing table content: " + e.getMessage());
            return null;
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Error parsing table content: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cleans SQL content by removing comments and extra whitespace.
     *
     * @param content Raw SQL content
     * @return Cleaned SQL content
     */
    private String cleanSqlContent(String content) {
        // Remove single-line comments (simplified, no Pattern.MULTILINE needed)
        content = content.replaceAll("--.*", "");

        // Remove multi-line comments (simplified)
        content = content.replaceAll("/\\*.*?\\*/", "");

        // Normalize whitespace
        content = content.replaceAll("\\s+", " ").trim();

        return content;
    }
}
