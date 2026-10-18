package edu.rit.swen755.auth.authn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static edu.rit.swen755.auth.TestFixtures.password;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for authentication (slice 1): the 401 path through the real filter chain,
 * the indistinguishability of a wrong password and an unknown user, and the publication of the
 * authentication events the audit listener consumes. Green with slice 1 and Step 0 alone.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class AuthenticationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DemoUserDetailsService users;

    @Autowired
    private ApplicationEvents events;

    /** The exact 401 body for a request to {@code path}. */
    private static String unauthorizedBody(String path) {
        return "{\"status\":401,\"error\":\"Unauthorized\",\"path\":\"" + path + "\"}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/me", "/api/patients/1/record", "/api/admin/audit"})
    void noCredentialsOnAProtectedUrlIs401WithChallengeAndJsonBody(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic realm=")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(unauthorizedBody(path)));
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("nurse.patel", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserGetsTheSameBodyAsAWrongPassword() throws Exception {
        byte[] wrongPassword = mvc.perform(get("/api/me").with(httpBasic("nurse.patel", "wrong")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsByteArray();
        byte[] unknownUser = mvc.perform(get("/api/me").with(httpBasic("mallory", "x")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(wrongPassword.length > 0, "the 401 response must carry a JSON body");
        assertArrayEquals(wrongPassword, unknownUser, "a caller must not be able to tell an unknown user from a wrong password");
    }

    @Test
    void badCredentialsOnThePublicUrlAre401() throws Exception {
        mvc.perform(get("/api/public/info").with(httpBasic("mallory", "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousOnAnUnmappedUrlIs401NotForbidden() throws Exception {
        mvc.perform(get("/api/nothing"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic realm=")))
                .andExpect(content().string(unauthorizedBody("/api/nothing")));
    }

    @Test
    void validCredentialsOnThePublicUrlAre200() throws Exception {
        mvc.perform(get("/api/public/info").with(httpBasic("alice", password("alice"))))
                .andExpect(status().isOk());
    }

    @Test
    void failureAndSuccessEventsArePublished() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("mallory", "x")))
                .andExpect(status().isUnauthorized());
        assertTrue(events.stream(AbstractAuthenticationFailureEvent.class)
                        .anyMatch(e -> "mallory".equals(e.getAuthentication().getName())),
                "expected an authentication failure event for mallory");

        mvc.perform(get("/api/public/info").with(httpBasic("alice", password("alice"))))
                .andExpect(status().isOk());
        assertTrue(events.stream(AuthenticationSuccessEvent.class)
                        .anyMatch(e -> "alice".equals(e.getAuthentication().getName())),
                "expected an authentication success event for alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"mallory", "", "ALICE", "Admin", "admin ", " alice", "nurse.patel:patel-pass", "dr"})
    void userStoreThrowsOnlyUsernameNotFoundForUnknownNames(String name) {
        // Unknown names, including near misses of real ones (no case folding or trimming), must
        // fail with exactly this exception, or a caller could tell them apart from a wrong password.
        assertThrowsExactly(UsernameNotFoundException.class, () -> users.loadUserByUsername(name));
    }

    // ---------------------------------------------------------------- finer tests

    @Test
    void anonymousWriteIs401NotForbidden() throws Exception {
        mvc.perform(post("/api/patients/1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systolic\":120,\"diastolic\":80,\"pulse\":72}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(unauthorizedBody("/api/patients/1/vitals")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic", "Basic !!!not-base64!!!", "Basic bm9jb2xvbg==", "Bearer abc.def.ghi"})
    void aMalformedOrForeignAuthorizationHeaderIs401Not500(String authorization) throws Exception {
        // "bm9jb2xvbg==" decodes to "nocolon": valid Base64 but no username:password separator.
        // A non-Basic scheme is ignored by the Basic filter, so the caller is simply anonymous.
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic realm=")))
                .andExpect(content().string(unauthorizedBody("/api/me")));

        // The header is rejected before the authentication manager runs, so no failure event is
        // published and the attempt does not reach the audit log (a known gap, documented in
        // Section 3 of the design document).
        assertEquals(0, events.stream(AbstractAuthenticationFailureEvent.class).count());
    }

    @Test
    void unknownUserAndWrongPasswordGetTheSameStatusAndHeaders() throws Exception {
        MvcResult wrongPassword = mvc.perform(get("/api/me").with(httpBasic("nurse.patel", "wrong"))).andReturn();
        MvcResult unknownUser = mvc.perform(get("/api/me").with(httpBasic("mallory", "x"))).andReturn();

        assertEquals(wrongPassword.getResponse().getStatus(), unknownUser.getResponse().getStatus());
        for (String name : List.of(HttpHeaders.WWW_AUTHENTICATE, HttpHeaders.CONTENT_TYPE, HttpHeaders.CONTENT_LENGTH)) {
            assertEquals(wrongPassword.getResponse().getHeaders(name), unknownUser.getResponse().getHeaders(name),
                    name + " must not differ between an unknown user and a wrong password");
        }
    }

    @Test
    void unknownUserAndWrongPasswordPublishTheSameFailureEvent() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("nurse.patel", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").with(httpBasic("mallory", "x"))).andExpect(status().isUnauthorized());

        List<AbstractAuthenticationFailureEvent> failures = events.stream(AbstractAuthenticationFailureEvent.class).toList();
        assertEquals(List.of("nurse.patel", "mallory"),
                failures.stream().map(e -> e.getAuthentication().getName()).toList());
        for (AbstractAuthenticationFailureEvent failure : failures) {
            assertInstanceOf(AuthenticationFailureBadCredentialsEvent.class, failure);
            assertEquals(BadCredentialsException.class, failure.getException().getClass(),
                    "an unknown user must be reported as bad credentials, like a wrong password");
        }
    }

    @Test
    void everyAuthenticatedRequestAuthenticatesAgain() throws Exception {
        // Stateless HTTP Basic keeps no session, so each request is a fresh authentication.
        for (int i = 0; i < 2; i++) {
            MvcResult result = mvc.perform(get("/api/public/info").with(httpBasic("alice", password("alice"))))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                    .andReturn();
            assertNull(result.getRequest().getSession(false), "no session may be created");
        }
        assertEquals(2, events.stream(AuthenticationSuccessEvent.class)
                .filter(e -> "alice".equals(e.getAuthentication().getName()))
                .count());
    }
}
