package edu.rit.swen755.auth.authn;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static edu.rit.swen755.auth.TestFixtures.password;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for authentication (slice 1): the 401 path through the real filter chain,
 * the indistinguishability of a wrong password and an unknown user, and the publication of the
 * authentication events the audit listener consumes. Green with slice 1 and Step 0 alone.
 */
@Disabled("TODO(slice 1: Godson) — enable when implementing")
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

    @Test
    void noCredentialsOnAProtectedUrlIs401WithChallengeAndJsonBody() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic realm=")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/me"));
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
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic realm=")));
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

    @Test
    void userStoreThrowsOnlyUsernameNotFoundForUnknownNames() {
        assertThrowsExactly(UsernameNotFoundException.class, () -> users.loadUserByUsername("mallory"));
        assertThrowsExactly(UsernameNotFoundException.class, () -> users.loadUserByUsername(""));
        assertThrowsExactly(UsernameNotFoundException.class, () -> users.loadUserByUsername("ALICE"));
    }
}
