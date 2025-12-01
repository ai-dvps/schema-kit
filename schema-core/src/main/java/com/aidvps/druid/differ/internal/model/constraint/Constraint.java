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

package com.aidvps.druid.differ.internal.model.constraint;

import java.util.Objects;
import java.util.Optional;

/**
 * Base abstract class for all database constraints.
 *
 * <p>Constraints define rules that must be satisfied by data in a table. This base class provides
 * common properties for all constraint types.
 */
public abstract class Constraint {
    protected final String name;
    protected final boolean enabled;
    protected final boolean deferred;

    /**
     * Creates a new Constraint.
     *
     * @param name the constraint name (may be null for unnamed constraints)
     * @param enabled whether the constraint is enabled
     * @param deferred whether the constraint is deferred (PostgreSQL, Oracle)
     */
    protected Constraint(String name, boolean enabled, boolean deferred) {
        this.name = name;
        this.enabled = enabled;
        this.deferred = deferred;
    }

    /**
     * Returns the constraint name.
     *
     * @return an Optional containing the name, or empty if unnamed
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns whether the constraint is enabled.
     *
     * @return true if enabled, false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns whether the constraint is deferred.
     *
     * @return true if deferred, false if immediate
     */
    public boolean isDeferred() {
        return deferred;
    }

    /**
     * Returns the type of this constraint.
     *
     * @return the constraint type
     */
    public abstract ConstraintType getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return enabled == that.enabled
                && deferred == that.deferred
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enabled, deferred);
    }

    /** Enumeration of constraint types. */
    public enum ConstraintType {
        PRIMARY_KEY,
        FOREIGN_KEY,
        UNIQUE,
        CHECK,
        NOT_NULL
    }
}
