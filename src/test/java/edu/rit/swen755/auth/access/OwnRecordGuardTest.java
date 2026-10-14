package edu.rit.swen755.auth.access;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for the per-actor own-record check (slice 2), calling the guard directly with
 * the security context that the test annotations install.
 */
@Disabled("TODO(slice 2: Kobe) — enable when implementing")
@SpringBootTest
@AutoConfigureMockMvc
class OwnRecordGuardTest {

    @Autowired
    private OwnRecordGuard guard;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMayReadAnyPatient() {
        assertStaffAllowedForAnyId();
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorMayReadAnyPatient() {
        assertStaffAllowedForAnyId();
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void nurseMayReadAnyPatient() {
        assertStaffAllowedForAnyId();
    }

    @Test
    @WithUserDetails("alice")
    void patientMayReadOnlyTheirOwnRecord() {
        assertTrue(guard.allows(current(), 1));
        assertFalse(guard.allows(current(), 2));
        assertFalse(guard.allows(current(), 99));
    }

    @Test
    @WithMockUser(username = "alice", roles = "PATIENT")
    void patientPrincipalThatIsNotAClinicUserIsRefused() {
        assertFalse(guard.allows(current(), 1));
        assertFalse(guard.allows(current(), 2));
    }

    private void assertStaffAllowedForAnyId() {
        assertTrue(guard.allows(current(), 1));
        assertTrue(guard.allows(current(), 2));
        assertTrue(guard.allows(current(), 99));
    }

    private static Authentication current() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
