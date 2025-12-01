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

import static org.junit.jupiter.api.Assertions.*;

import com.aidvps.druid.differ.internal.model.DatabasePlatform;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for MigrationConfig. */
class MigrationConfigTest {

    @Test
    void testBuilderWithAllOptions() {
        // Arrange
        com.aidvps.schemakit.migrator.CustomSqlGenerator customGenerator =
                Mockito.mock(com.aidvps.schemakit.migrator.CustomSqlGenerator.class);

        // Act
        MigrationConfig config =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .mode(MigrationMode.FULL)
                        .includeDrops(true)
                        .transactional(true)
                        .customGenerator(customGenerator)
                        .build();

        // Assert
        assertEquals(DatabasePlatform.MYSQL, config.getTargetPlatform());
        assertEquals(MigrationMode.FULL, config.getMode());
        assertTrue(config.isIncludeDrops());
        assertTrue(config.isTransactional());
        assertTrue(config.getCustomGenerator().isPresent());
        assertEquals(customGenerator, config.getCustomGenerator().get());
    }

    @Test
    void testBuilderWithDefaultValues() {
        // Act
        MigrationConfig config =
                MigrationConfig.builder().targetPlatform(DatabasePlatform.POSTGRESQL).build();

        // Assert
        assertEquals(DatabasePlatform.POSTGRESQL, config.getTargetPlatform());
        assertEquals(MigrationMode.FULL, config.getMode());
        assertTrue(config.isIncludeDrops()); // default is true
        assertFalse(config.isTransactional()); // default is false
        assertFalse(config.getCustomGenerator().isPresent());
    }

    @Test
    void testBuilderWithMinimalConfig() {
        // Act
        MigrationConfig config =
                MigrationConfig.builder().targetPlatform(DatabasePlatform.MARIADB).build();

        // Assert
        assertEquals(DatabasePlatform.MARIADB, config.getTargetPlatform());
        assertNotNull(config.getMode());
        assertNotNull(config);
    }

    @Test
    void testBuilderWithCustomGenerator() {
        // Arrange
        com.aidvps.schemakit.migrator.CustomSqlGenerator customGenerator =
                Mockito.mock(com.aidvps.schemakit.migrator.CustomSqlGenerator.class);

        // Act
        MigrationConfig config =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.SQLITE)
                        .customGenerator(customGenerator)
                        .build();

        // Assert
        assertEquals(DatabasePlatform.SQLITE, config.getTargetPlatform());
        assertTrue(config.getCustomGenerator().isPresent());
        assertEquals(customGenerator, config.getCustomGenerator().get());
    }

    @Test
    void testBuilderWithMigrationModeUpdate() {
        // Act
        MigrationConfig config =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .mode(MigrationMode.FULL)
                        .build();

        // Assert
        assertEquals(DatabasePlatform.MYSQL, config.getTargetPlatform());
        assertEquals(MigrationMode.FULL, config.getMode());
    }

    @Test
    void testBuilderWithIncludeDropsFalse() {
        // Act
        MigrationConfig config =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.POSTGRESQL)
                        .includeDrops(false)
                        .build();

        // Assert
        assertFalse(config.isIncludeDrops());
    }

    @Test
    void testBuilderWithTransactionalTrue() {
        // Act
        MigrationConfig config =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.MARIADB)
                        .transactional(true)
                        .build();

        // Assert
        assertTrue(config.isTransactional());
    }

    @Test
    void testBuilderReturnsThisForMethodChaining() {
        // Act
        MigrationConfig.Builder builder =
                MigrationConfig.builder().targetPlatform(DatabasePlatform.MYSQL);

        // Assert - verify that methods return the builder for chaining
        assertNotNull(builder);
        assertSame(builder, builder.targetPlatform(DatabasePlatform.MYSQL));
        assertSame(builder, builder.mode(MigrationMode.FULL));
        assertSame(builder, builder.includeDrops(true));
        assertSame(builder, builder.transactional(false));
        assertSame(builder, builder.customGenerator(null));
    }

    @Test
    void testMultipleBuildsProduceIndependentInstances() {
        // Act
        MigrationConfig config1 =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .mode(MigrationMode.FULL)
                        .build();

        MigrationConfig config2 =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.POSTGRESQL)
                        .mode(MigrationMode.FORWARD_ONLY)
                        .build();

        // Assert
        assertNotSame(config1, config2);
        assertNotEquals(config1, config2);
        assertEquals(DatabasePlatform.MYSQL, config1.getTargetPlatform());
        assertEquals(DatabasePlatform.POSTGRESQL, config2.getTargetPlatform());
    }

    @Test
    void testBuilderReuse() {
        // Arrange
        com.aidvps.schemakit.migrator.CustomSqlGenerator generator1 =
                Mockito.mock(com.aidvps.schemakit.migrator.CustomSqlGenerator.class);
        com.aidvps.schemakit.migrator.CustomSqlGenerator generator2 =
                Mockito.mock(com.aidvps.schemakit.migrator.CustomSqlGenerator.class);

        // Act
        MigrationConfig config1 =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.MYSQL)
                        .customGenerator(generator1)
                        .build();

        MigrationConfig config2 =
                MigrationConfig.builder()
                        .targetPlatform(DatabasePlatform.POSTGRESQL)
                        .customGenerator(generator2)
                        .build();

        // Assert
        assertEquals(generator1, config1.getCustomGenerator().get());
        assertEquals(generator2, config2.getCustomGenerator().get());
    }
}
