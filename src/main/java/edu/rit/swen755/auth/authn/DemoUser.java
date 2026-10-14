package edu.rit.swen755.auth.authn;

import edu.rit.swen755.auth.access.Role;

import java.util.OptionalLong;

/**
 * One seeded user of the portal.
 *
 * @param username     the login name
 * @param passwordHash the stored password, a {@code {bcrypt}} hash (never plaintext)
 * @param role         the user's single role
 * @param patientId    the patient this user is, for a PATIENT; empty for staff
 */
public record DemoUser(String username, String passwordHash, Role role, OptionalLong patientId) {
}
