package edu.rit.swen755.auth.audit;

import edu.rit.swen755.auth.TestFixtures.AuditBaseline;
import edu.rit.swen755.auth.access.ApiAccessDeniedHandler;
import edu.rit.swen755.auth.authn.ClinicUser;
import edu.rit.swen755.auth.authn.DemoUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

import static edu.rit.swen755.auth.TestFixtures.after;
import static edu.rit.swen755.auth.TestFixtures.baseline;
import static edu.rit.swen755.auth.TestFixtures.password;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for the Audit tactic (slice 3).
 *
 * <p>The 403 handler is replaced by a mock that only sets the status, so the real-chain test
 * depends on slice 3's code alone. Every test takes a baseline first and only looks at entries
 * recorded after it.
 */
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

    @MockitoBean
    private ApiAccessDeniedHandler deniedHandler;

    @BeforeEach
    void deniedHandlerAnswers403() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(1, HttpServletResponse.class).setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }).when(deniedHandler).handle(any(), any(), any());
    }

    @Test
    void recordsTheThreeKindsOfSecurityEvent() {
        AuditBaseline before = baseline(auditLog);
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

        List<AuditEntry> recorded = after(auditLog, before);
        AuditEntry failure = find(recorded, AuditKind.AUTHN_FAILURE, "mallory");
        assertEquals("BadCredentialsException", failure.detail());
        assertEquals("GET", failure.method());
        assertEquals("/api/patients/2/record", failure.path());

        AuditEntry success = find(recorded, AuditKind.AUTHN_SUCCESS, "alice");
        assertEquals("GET", success.method());
        assertEquals("/api/patients/2/record", success.path());
        assertEquals("-", success.detail());

        AuditEntry denied = find(recorded, AuditKind.AUTHZ_DENIED, "anonymous");
        assertEquals("GET", denied.method());
        assertEquals("/api/patients/2/record", denied.path());
        assertTrue(denied.detail().contains("ROLE_ADMIN"), "denial detail should name the required role: " + denied.detail());
        assertTrue(recorded.stream().allMatch(e -> e.at() != null && !e.at().isAfter(Instant.now())));
    }

    @Test
    void bareDenialsRecordAuthenticatedAndDenyAllDetails() {
        AuditBaseline before = baseline(auditLog);
        ClinicUser alice = (ClinicUser) users.loadUserByUsername("alice");
        Authentication aliceAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                alice, null, alice.getAuthorities());
        Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        try {
            MockHttpServletRequest meRequest = new MockHttpServletRequest("GET", "/api/me");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(meRequest));
            publisher.publishEvent(new AuthorizationDeniedEvent<>(() -> aliceAuthentication, meRequest,
                    new org.springframework.security.authorization.AuthorizationDecision(false)));

            MockHttpServletRequest unknownRequest = new MockHttpServletRequest("POST", "/api/nothing");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(unknownRequest));
            publisher.publishEvent(new AuthorizationDeniedEvent<>(() -> anonymous, unknownRequest,
                    new org.springframework.security.authorization.AuthorizationDecision(false)));
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
        }

        List<AuditEntry> recorded = after(auditLog, before);
        AuditEntry authenticatedDenial = find(recorded, AuditKind.AUTHZ_DENIED, "alice");
        assertEquals("authenticated", authenticatedDenial.detail());
        assertEquals("GET", authenticatedDenial.method());
        assertEquals("/api/me", authenticatedDenial.path());

        AuditEntry defaultDenial = find(recorded, AuditKind.AUTHZ_DENIED, "anonymous");
        assertEquals("denyAll", defaultDenial.detail());
        assertEquals("POST", defaultDenial.method());
        assertEquals("/api/nothing", defaultDenial.path());
    }

    @Test
    void aForbiddenRequestThroughTheRealChainLogsSuccessThenDenial() throws Exception {
        AuditBaseline before = baseline(auditLog);

        mvc.perform(get("/api/admin/users").with(httpBasic("dr.chen", password("dr.chen"))))
                .andExpect(status().isForbidden());

        List<AuditEntry> recorded = after(auditLog, before).stream()
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
