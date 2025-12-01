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

package com.aidvps.druid.differ.internal.generator;

import com.aidvps.druid.differ.exception.GenerationException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes dependencies between migration statements and orders them for safe execution.
 *
 * <p>This class builds a dependency graph from migration statements and performs topological
 * sorting to ensure statements execute in the correct order. It also detects circular dependencies
 * that would prevent successful migration.
 *
 * <p>Ordering rules:
 *
 * <ul>
 *   <li>Drop foreign keys before dropping referenced tables
 *   <li>Drop indexes before dropping tables
 *   <li>Add tables before adding foreign keys
 *   <li>Add columns before adding constraints
 * </ul>
 */
public class DependencyAnalyzer {

    /**
     * Orders migration statements based on their dependencies.
     *
     * @param statements the list of migration statements to order
     * @return ordered list of statements safe for execution
     * @throws GenerationException if circular dependencies are detected
     */
    public List<MigrationStatement> orderStatements(List<MigrationStatement> statements)
            throws GenerationException {
        if (statements.isEmpty()) {
            return new ArrayList<>();
        }

        // Group statements by priority first
        Map<Integer, List<MigrationStatement>> priorityGroups =
                statements.stream().collect(Collectors.groupingBy(MigrationStatement::getPriority));

        List<MigrationStatement> orderedStatements = new ArrayList<>();

        // Process each priority group
        List<Integer> priorities = new ArrayList<>(priorityGroups.keySet());
        Collections.sort(priorities);

        for (Integer priority : priorities) {
            List<MigrationStatement> group = priorityGroups.get(priority);
            orderedStatements.addAll(topologicalSort(group));
        }

        return orderedStatements;
    }

    /**
     * Performs topological sort on a list of statements.
     *
     * @param statements the statements to sort
     * @return topologically sorted list
     * @throws GenerationException if circular dependencies exist
     */
    private List<MigrationStatement> topologicalSort(List<MigrationStatement> statements)
            throws GenerationException {
        // Build adjacency list for dependency graph
        Map<String, List<MigrationStatement>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Initialize graph nodes
        for (MigrationStatement stmt : statements) {
            String key = getStatementKey(stmt);
            graph.putIfAbsent(key, new ArrayList<>());
            inDegree.putIfAbsent(key, 0);
        }

        // Build edges based on dependencies
        for (MigrationStatement stmt : statements) {
            String stmtKey = getStatementKey(stmt);

            for (String dependency : stmt.getDependsOn()) {
                // Find statements that create/affect the dependency
                for (MigrationStatement depStmt : statements) {
                    if (dependency.equals(depStmt.getTableName())
                            && shouldDependOn(stmt, depStmt)) {
                        String depKey = getStatementKey(depStmt);
                        graph.get(depKey).add(stmt);
                        inDegree.put(stmtKey, inDegree.get(stmtKey) + 1);
                    }
                }
            }
        }

        // Kahn's algorithm for topological sort
        Queue<String> queue = new LinkedList<>();
        Map<String, MigrationStatement> stmtMap = new HashMap<>();

        for (MigrationStatement stmt : statements) {
            String key = getStatementKey(stmt);
            stmtMap.put(key, stmt);
            if (inDegree.get(key) == 0) {
                queue.offer(key);
            }
        }

        List<MigrationStatement> result = new ArrayList<>();
        int processed = 0;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(stmtMap.get(current));
            processed++;

            for (MigrationStatement neighbor : graph.get(current)) {
                String neighborKey = getStatementKey(neighbor);
                inDegree.put(neighborKey, inDegree.get(neighborKey) - 1);
                if (inDegree.get(neighborKey) == 0) {
                    queue.offer(neighborKey);
                }
            }
        }

        // Check for circular dependencies
        if (processed < statements.size()) {
            List<String> cyclicStatements =
                    statements.stream()
                            .filter(stmt -> inDegree.get(getStatementKey(stmt)) > 0)
                            .map(MigrationStatement::toString)
                            .collect(Collectors.toList());

            throw new GenerationException(
                    "Circular dependency detected in migration statements: "
                            + String.join(", ", cyclicStatements));
        }

        return result;
    }

    /**
     * Determines if stmt should depend on depStmt based on their types.
     *
     * @param stmt the statement that might depend
     * @param depStmt the potential dependency
     * @return true if stmt should wait for depStmt
     */
    private boolean shouldDependOn(MigrationStatement stmt, MigrationStatement depStmt) {
        MigrationStatement.Type stmtType = stmt.getType();
        MigrationStatement.Type depType = depStmt.getType();

        // Adding foreign key depends on referenced table existing
        if (stmtType == MigrationStatement.Type.ADD_CONSTRAINT
                && depType == MigrationStatement.Type.CREATE_TABLE) {
            return true;
        }

        // Adding constraint depends on column existing
        if (stmtType == MigrationStatement.Type.ADD_CONSTRAINT
                && depType == MigrationStatement.Type.ADD_COLUMN) {
            return true;
        }

        // Dropping table depends on foreign keys being dropped first
        if (stmtType == MigrationStatement.Type.DROP_TABLE
                && depType == MigrationStatement.Type.DROP_CONSTRAINT) {
            return true;
        }

        // Dropping table depends on indexes being dropped first
        if (stmtType == MigrationStatement.Type.DROP_TABLE
                && depType == MigrationStatement.Type.DROP_INDEX) {
            return true;
        }

        return false;
    }

    /**
     * Generates a unique key for a statement.
     *
     * @param stmt the statement
     * @return unique key string
     */
    private String getStatementKey(MigrationStatement stmt) {
        return stmt.getType() + ":" + stmt.getTableName() + ":" + stmt.getSql().hashCode();
    }
}
