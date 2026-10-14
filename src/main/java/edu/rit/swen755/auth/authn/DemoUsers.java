package edu.rit.swen755.auth.authn;

import edu.rit.swen755.auth.access.Role;

import java.util.List;
import java.util.OptionalLong;

/**
 * The five seeded users: one per staff role, and two patients so that the own-record rule can be
 * shown both ways.
 *
 * <p>Passwords are stored only as BCrypt hashes (cost 10) with the {@code {bcrypt}} prefix that
 * the delegating password encoder reads. The matching plaintext passwords are published in the
 * README on purpose, so that a grader can log in as each user type; the hashes therefore protect
 * nothing secret here. They show how credentials are stored, not a secret store. A real deployment
 * would keep users outside the code and force a password change on first login (Change
 * Credential Settings, SAiP 4th ed., p. 175).
 */
public final class DemoUsers {

    private static final List<DemoUser> ALL = List.of(
            new DemoUser("admin",
                    "{bcrypt}$2a$10$X.5J9VoOM971rjHeBLmOx.Tq3B1b.67DdyFMUKMAOO0dtVMYB08LS",
                    Role.ADMIN, OptionalLong.empty()),
            new DemoUser("dr.chen",
                    "{bcrypt}$2a$10$SCB4gmb2zZBYyMH6J3clX.oaeKf6sMcmXuIJ6ddj2s7x1.C/Pmzia",
                    Role.DOCTOR, OptionalLong.empty()),
            new DemoUser("nurse.patel",
                    "{bcrypt}$2a$10$FF4usO6va5FazXumLFL7AeIvp08JUCcOOZbcNV2WWhxF4pml25Y4i",
                    Role.NURSE, OptionalLong.empty()),
            new DemoUser("alice",
                    "{bcrypt}$2a$10$BPCTgHHBlyNiz2w5z7I3UO93oOWb/Ef3wSB9FrZzIvfSUGnA/ElFi",
                    Role.PATIENT, OptionalLong.of(1)),
            new DemoUser("bob",
                    "{bcrypt}$2a$10$UGIj4Fkf7mCvg3qUq2O7feAxqLTwNyCcTwOt4UURDjH.YtKNd4MMK",
                    Role.PATIENT, OptionalLong.of(2)));

    private DemoUsers() {
    }

    /**
     * The seeded users, in a fixed order.
     *
     * @return an unmodifiable list of the five users
     */
    public static List<DemoUser> all() {
        return ALL;
    }
}
