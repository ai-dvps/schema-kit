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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses .db files containing CREATE DATABASE statements. Each .db file should contain a single
 * CREATE DATABASE statement that defines the database name.
 */
public class DatabaseFileParser {

    private static final String DB_FILE_EXTENSION = ".db";

    /**
     * Parses all .db files in the given directory and extracts database names.
     *
     * @param directoryPath Path to the directory containing .db files
     * @return Map of database name to file path
     * @throws IOException if reading files fails
     */
    public Map<String, String> parseDatabaseFiles(Path directoryPath) throws IOException {
        Map<String, String> databaseNames = new HashMap<>();

        Files.list(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(DB_FILE_EXTENSION))
                .forEach(
                        dbFile -> {
                            try {
                                String databaseName = parseDatabaseName(dbFile);
                                if (databaseName != null) {
                                    databaseNames.put(databaseName, dbFile.toString());
                                }
                            } catch (Exception e) {
                                // Log error but continue processing other files
                                System.err.println(
                                        "Error parsing database file "
                                                + dbFile
                                                + ": "
                                                + e.getMessage());
                            }
                        });

        return databaseNames;
    }

    /**
     * Parses a single .db file to extract the database name.
     *
     * @param dbFile Path to the .db file
     * @return Database name, or null if parsing fails
     * @throws IOException if reading file fails
     */
    public String parseDatabaseName(Path dbFile) throws IOException {
        String content = new String(Files.readAllBytes(dbFile), StandardCharsets.UTF_8);
        return parseDatabaseNameFromContent(content);
    }

    /**
     * Parses CREATE DATABASE statement from SQL content.
     *
     * @param content SQL content containing CREATE DATABASE statement
     * @return Database name, or null if parsing fails
     */
    public String parseDatabaseNameFromContent(String content) {
        return extractDatabaseNameWithRegex(content);
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

    /**
     * Fallback method to extract database name using regex if SQL parsing fails.
     *
     * @param content SQL content
     * @return Database name, or null if not found
     */
    private String extractDatabaseNameWithRegex(String content) {
        // Match CREATE DATABASE statement
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "CREATE\\s+DATABASE\\s+`?([\\w]+)`?",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
