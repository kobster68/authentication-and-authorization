package edu.rit.swen755.auth.access;

import java.util.Objects;
import java.util.Set;

/**
 * What a caller must satisfy to reach one service in the access table.
 *
 * <p>The interface is sealed so that the code turning the table into URL rules
 * ({@link AccessPolicy}) can switch over every case exhaustively.
 */
public sealed interface Requirement {

    /** Anyone may call the service, with or without credentials. */
    record PermitAll() implements Requirement {
    }

    /** Any authenticated user may call the service, whatever their role. */
    record Authenticated() implements Requirement {
    }

    /**
     * A caller holding any one of these roles may call the service. The role hierarchy applies,
     * so naming NURSE also admits DOCTOR and ADMIN.
     *
     * @param roles the roles admitted (at least one); stored as an unmodifiable copy
     */
    record AnyRole(Set<Role> roles) implements Requirement {

        /**
         * Validates and copies the role set.
         *
         * @throws IllegalArgumentException if {@code roles} is empty
         */
        public AnyRole {
            Objects.requireNonNull(roles, "roles");
            if (roles.isEmpty()) {
                throw new IllegalArgumentException("AnyRole needs at least one role");
            }
            roles = Set.copyOf(roles);
        }
    }
}
