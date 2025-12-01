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

import com.aidvps.druid.differ.internal.model.Schema;
import java.util.List;

/** Default implementation of SchemaMigrator. */
public class DefaultSchemaMigrator implements SchemaMigrator {
    private final SchemaComparator schemaComparator;
    private final SqlGenerator sqlGenerator;
    private final DependencyAnalyzer dependencyAnalyzer;

    /** Creates a new DefaultSchemaMigrator with default components. */
    public DefaultSchemaMigrator() {
        this.schemaComparator = new SchemaComparator();
        this.sqlGenerator = new SqlGenerator();
        this.dependencyAnalyzer = new DependencyAnalyzer();
    }

    /** Creates a DefaultSchemaMigrator with custom components. */
    public DefaultSchemaMigrator(
            SchemaComparator schemaComparator,
            SqlGenerator sqlGenerator,
            DependencyAnalyzer dependencyAnalyzer) {
        this.schemaComparator = schemaComparator;
        this.sqlGenerator = sqlGenerator;
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    @Override
    public MigrationScript generateMigration(Schema source, Schema target, MigrationConfig config)
            throws MigrationException {
        if (source == null || target == null || config == null) {
            throw new MigrationException(
                    MigrationException.ErrorCode.SOURCE_SCHEMA_INVALID,
                    "Source schema, target schema, and configuration cannot be null");
        }

        try {
            // Compare schemas
            SchemaDiff diff = compare(source, target);

            // Generate SQL statements
            List<MigrationStatement> statements = sqlGenerator.generate(diff, config);

            // Build migration script
            MigrationScript.Builder builder =
                    MigrationScript.builder()
                            .targetPlatform(config.getTargetPlatform())
                            .mode(config.getMode());

            for (MigrationStatement stmt : statements) {
                builder.statement(stmt);
            }

            return builder.build();

        } catch (Exception e) {
            throw new MigrationException(
                    MigrationException.ErrorCode.GENERATION_ERROR,
                    "Failed to generate migration: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public SchemaDiff compare(Schema source, Schema target) throws MigrationException {
        if (source == null || target == null) {
            throw new MigrationException(
                    MigrationException.ErrorCode.SOURCE_SCHEMA_INVALID,
                    "Source schema and target schema cannot be null");
        }

        return schemaComparator.compare(source, target);
    }

    @Override
    public ValidationResult validateMigration(MigrationScript script, Schema targetSchema) {
        return ValidationResult.builder().build();
    }
}
