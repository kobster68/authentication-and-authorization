package edu.rit.swen755.auth.access;

import edu.rit.swen755.auth.authn.ClinicUser;
import edu.rit.swen755.auth.authn.DemoUsers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for the per-actor own-record check (slice 2): plain unit tests of the guard,
 * no Spring context.
 */
@Disabled("TODO(slice 2: Kobe) — enable when implementing")
class OwnRecordGuardTest {

    private final OwnRecordGuard guard = new OwnRecordGuard();

    @Test
    void adminMayReadAnyPatient() {
        assertAllowedForAnyId(staff("ROLE_ADMIN"));
    }

    @Test
    void doctorMayReadAnyPatient() {
        assertAllowedForAnyId(staff("ROLE_DOCTOR"));
    }

    @Test
    void nurseMayReadAnyPatient() {
        assertAllowedForAnyId(staff("ROLE_NURSE"));
    }

    @Test
    void patientMayReadOnlyTheirOwnRecord() {
        Authentication alice = signedIn(clinicUser("alice"));
        assertTrue(guard.allows(alice, 1));
        assertFalse(guard.allows(alice, 2));
        assertFalse(guard.allows(alice, 99));

        Authentication bob = signedIn(clinicUser("bob"));
        assertTrue(guard.allows(bob, 2));
        assertFalse(guard.allows(bob, 1));
    }

    @Test
    void patientPrincipalThatIsNotAClinicUserIsRefused() {
        UserDetails plain = User.withUsername("alice").password("unused").roles("PATIENT").build();
        Authentication auth = signedIn(plain);
        assertFalse(guard.allows(auth, 1));
        assertFalse(guard.allows(auth, 2));
    }

    private void assertAllowedForAnyId(Authentication auth) {
        assertTrue(guard.allows(auth, 1));
        assertTrue(guard.allows(auth, 2));
        assertTrue(guard.allows(auth, 99));
    }

    private static Authentication staff(String authority) {
        return new TestingAuthenticationToken("staff", null, authority);
    }

    private static ClinicUser clinicUser(String username) {
        return new ClinicUser(DemoUsers.all().stream()
                .filter(u -> u.username().equals(username))
                .findFirst()
                .orElseThrow());
    }

    private static Authentication signedIn(UserDetails user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
    }
}
