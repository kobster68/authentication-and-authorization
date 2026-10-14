package edu.rit.swen755.auth.audit;

import edu.rit.swen755.auth.authn.ClinicUser;
import edu.rit.swen755.auth.authn.DemoUserDetailsService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

import static edu.rit.swen755.auth.ContractSmokeTest.password;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Acceptance tests for the Audit tactic (slice 3).
 *
 * <p>The context, and with it the one {@link AuditLog}, is shared with the other test classes, so
 * every test takes a baseline index first and only looks at entries recorded after it.
 */
@Disabled("TODO(slice 3: Chase) — enable when implementing")
@SpringBootTest
@AutoConfigureMockMvc
class AuditTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private AuditLog auditLog;

    @Autowired
    private DemoUserDetailsService users;

    @Autowired
    private MockMvc mvc;

    @Test
    void recordsTheThreeKindsOfSecurityEvent() {
        int baseline = auditLog.entries().size();
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients/2/record");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            publisher.publishEvent(new AuthenticationFailureBadCredentialsEvent(
                    UsernamePasswordAuthenticationToken.unauthenticated("mallory", "x"),
                    new BadCredentialsException("Bad credentials")));

            ClinicUser alice = (ClinicUser) users.loadUserByUsername("alice");
            publisher.publishEvent(new AuthenticationSuccessEvent(
                    UsernamePasswordAuthenticationToken.authenticated(alice, null, alice.getAuthorities())));

            Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                    AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            publisher.publishEvent(new AuthorizationDeniedEvent<>(() -> anonymous, request,
                    new AuthorityAuthorizationDecision(false, AuthorityUtils.createAuthorityList("ROLE_ADMIN"))));
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
        }

        List<AuditEntry> recorded = after(baseline);
        AuditEntry failure = find(recorded, AuditKind.AUTHN_FAILURE, "mallory");
        assertEquals("BadCredentialsException", failure.detail());
        assertEquals("GET", failure.method());
        assertEquals("/api/patients/2/record", failure.path());

        AuditEntry success = find(recorded, AuditKind.AUTHN_SUCCESS, "alice");
        assertEquals("GET", success.method());
        assertEquals("/api/patients/2/record", success.path());

        AuditEntry denied = find(recorded, AuditKind.AUTHZ_DENIED, "anonymous");
        assertEquals("GET", denied.method());
        assertEquals("/api/patients/2/record", denied.path());
        assertTrue(denied.detail().contains("ROLE_ADMIN"), "denial detail should name the required role: " + denied.detail());
        assertTrue(recorded.stream().allMatch(e -> e.at() != null && !e.at().isAfter(Instant.now())));
    }

    @Test
    void aForbiddenRequestThroughTheRealChainLogsSuccessThenDenial() throws Exception {
        int baseline = auditLog.entries().size();

        // The response status is slice 2's concern and is asserted in AccessMatrixTest; until
        // slice 2 lands, its 403 handler is still a Step 0 stub, which is tolerated here.
        performToleratingStubs("/api/admin/users", "dr.chen");

        List<AuditEntry> recorded = after(baseline).stream()
                .filter(e -> "dr.chen".equals(e.username()) && "/api/admin/users".equals(e.path()))
                .toList();
        int success = indexOf(recorded, AuditKind.AUTHN_SUCCESS);
        int denied = indexOf(recorded, AuditKind.AUTHZ_DENIED);
        assertTrue(success >= 0 && denied > success,
                "expected AUTHN_SUCCESS then AUTHZ_DENIED for dr.chen on /api/admin/users, got " + recorded);
        assertEquals("GET", recorded.get(denied).method());
    }

    @Test
    void entriesAreAnUnmodifiableSnapshot() {
        List<AuditEntry> snapshot = auditLog.entries();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(
                new AuditEntry(Instant.now(), AuditKind.AUTHN_SUCCESS, "x", "-", "-", "-")));
    }

    private void performToleratingStubs(String path, String username) throws Exception {
        try {
            mvc.perform(get(path).with(httpBasic(username, password(username))));
        } catch (Exception e) {
            Throwable cause = e;
            while (cause != null && !(cause instanceof UnsupportedOperationException)) {
                cause = cause.getCause();
            }
            if (cause == null || cause.getMessage() == null || !cause.getMessage().startsWith("TODO(")) {
                throw e;
            }
        }
    }

    private List<AuditEntry> after(int baseline) {
        List<AuditEntry> all = auditLog.entries();
        return all.subList(baseline, all.size());
    }

    private static AuditEntry find(List<AuditEntry> entries, AuditKind kind, String username) {
        return entries.stream()
                .filter(e -> e.kind() == kind && username.equals(e.username()))
                .findFirst()
                .orElseGet(() -> fail("no " + kind + " entry for " + username + " in " + entries));
    }

    private static int indexOf(List<AuditEntry> entries, AuditKind kind) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).kind() == kind) {
                return i;
            }
        }
        return -1;
    }
}
