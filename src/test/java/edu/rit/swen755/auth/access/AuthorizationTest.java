package edu.rit.swen755.auth.access;

import edu.rit.swen755.auth.authn.ClinicUser;
import edu.rit.swen755.auth.authn.DemoUserDetailsService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static edu.rit.swen755.auth.TestFixtures.PRESCRIPTION;
import static edu.rit.swen755.auth.TestFixtures.VITALS;
import static edu.rit.swen755.auth.TestFixtures.password;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for authorization (slice 2): per-role denials from the URL rules, per-actor
 * denials from the own-record check, default deny, and the publication of denial events.
 *
 * <p>Only denials are asserted here. Success cells reach the controllers and are asserted in
 * {@code AccessMatrixTest}; the two hierarchy and error-dispatch checks below ask the URL
 * authorization manager directly for the same reason.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class AuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private SecurityFilterChain chain;

    @Autowired
    private DemoUserDetailsService users;

    @Test
    void nurseCannotPrescribeAndGetsA403JsonBody() throws Exception {
        mvc.perform(post("/api/patients/1/prescriptions")
                        .with(httpBasic("nurse.patel", password("nurse.patel")))
                        .contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/patients/1/prescriptions"));
    }

    @Test
    void doctorCannotListUsers() throws Exception {
        mvc.perform(get("/api/admin/users").with(httpBasic("dr.chen", password("dr.chen"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotListPatients() throws Exception {
        mvc.perform(get("/api/patients").with(httpBasic("alice", password("alice"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unmappedUrlIsDeniedEvenToAdmin() throws Exception {
        mvc.perform(get("/api/nothing").with(httpBasic("admin", password("admin"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/nothing"));
    }

    @Test
    void patientCannotReadAnotherPatientsRecord() throws Exception {
        mvc.perform(get("/api/patients/2/record").with(httpBasic("alice", password("alice"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/patients/2/record"));
    }

    @Test
    void patientCannotReadAnotherPatientsAppointments() throws Exception {
        mvc.perform(get("/api/patients/1/appointments").with(httpBasic("bob", password("bob"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotRecordVitalsEvenForThemselves() throws Exception {
        mvc.perform(post("/api/patients/1/vitals")
                        .with(httpBasic("alice", password("alice")))
                        .contentType(MediaType.APPLICATION_JSON).content(VITALS))
                .andExpect(status().isForbidden());

        // The URL rule refused it; the own-record check (method security) was never reached.
        assertTrue(events.stream(AuthorizationDeniedEvent.class)
                .anyMatch(e -> e.getObject() instanceof HttpServletRequest));
        assertFalse(events.stream(AuthorizationDeniedEvent.class)
                .anyMatch(e -> e.getObject() instanceof MethodInvocation));
    }

    // The event's authentication is a lazy supplier bound to the request's security context, so
    // it can only be read while the request is in flight (as the audit listener does), not here.

    @Test
    void urlDenialPublishesAnEventForTheRequest() throws Exception {
        mvc.perform(get("/api/admin/users").with(httpBasic("dr.chen", password("dr.chen"))))
                .andExpect(status().isForbidden());

        assertTrue(events.stream(AuthorizationDeniedEvent.class)
                        .anyMatch(e -> e.getObject() instanceof HttpServletRequest request
                                && "/api/admin/users".equals(request.getRequestURI())),
                "expected a URL-level denial event for /api/admin/users");
    }

    @Test
    void ownRecordDenialPublishesAnEventForTheMethod() throws Exception {
        mvc.perform(get("/api/patients/1/record").with(httpBasic("bob", password("bob"))))
                .andExpect(status().isForbidden());

        assertTrue(events.stream(AuthorizationDeniedEvent.class)
                        .anyMatch(e -> e.getObject() instanceof MethodInvocation invocation
                                && "record".equals(invocation.getMethod().getName())),
                "expected a method-level denial event for PatientController.record");
    }

    @Test
    void everyRoleRuleAdmitsAdminThroughTheHierarchy() {
        ClinicUser admin = (ClinicUser) users.loadUserByUsername("admin");
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities());

        for (AccessRule rule : AccessRules.all()) {
            if (!(rule.requirement() instanceof Requirement.AnyRole)) {
                continue;
            }
            MockHttpServletRequest request = new MockHttpServletRequest(rule.method().name(), rule.pattern().replace("{id}", "1"));
            AuthorizationResult result = urlAuthorization().authorize(() -> auth, request);
            assertTrue(result != null && result.isGranted(), "ADMIN should be admitted to " + rule.method() + " " + rule.pattern());
        }
    }

    @Test
    void errorDispatchIsPermittedSoAnErrorPageIsNeverItselfDenied() {
        Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        MockHttpServletRequest forward = new MockHttpServletRequest("GET", "/error");
        forward.setDispatcherType(DispatcherType.ERROR);

        AuthorizationResult result = urlAuthorization().authorize(() -> anonymous, forward);
        assertTrue(result != null && result.isGranted(), "the ERROR dispatch to /error must be permitted");
    }

    private AuthorizationManager<HttpServletRequest> urlAuthorization() {
        return chain.getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .findFirst()
                .orElseThrow()
                .getAuthorizationManager();
    }

    @Test
    void errorDispatchForAMissingResourceDoesNotPublishADenial() throws Exception {
        mvc.perform(get("/missing-resource").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                })
                        .requestAttr("jakarta.servlet.error.status_code", 404))
                .andExpect(status().isNotFound());
        assertFalse(events.stream(AuthorizationDeniedEvent.class).findAny().isPresent());
    }

    @Test
    void wrongHttpMethodIsDeniedEvenToAdmin() {
        ClinicUser admin = (ClinicUser) users.loadUserByUsername("admin");
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/users");
        AuthorizationResult result = urlAuthorization().authorize(() -> auth, request);
        assertTrue(result != null && !result.isGranted());
    }

    @Test
    void patientCannotProbeWhetherAnotherPatientExists() throws Exception {
        mvc.perform(get("/api/patients/99/record").with(httpBasic("alice", password("alice"))))
                .andExpect(status().isForbidden());
        assertTrue(events.stream(AuthorizationDeniedEvent.class)
                .anyMatch(e -> e.getObject() instanceof MethodInvocation));
    }
}
