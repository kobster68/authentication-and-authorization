package edu.rit.swen755.auth.authn;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link ApiAuthenticationEntryPoint} on its own, without a Spring context: the
 * status, the challenge header, and a response that never reveals the failure reason. The body
 * format itself is tested in {@code ApiErrorTest}.
 *
 * <p>These tests use a plain {@link JsonMapper} and check only the entry point's own logic. The
 * path through the mapper that Spring Boot configures, and the real filter chain, is covered by
 * {@code AuthenticationTest}.
 */
class ApiAuthenticationEntryPointTest {

    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(JsonMapper.builder().build());

    private MockHttpServletResponse commence(AuthenticationException reason) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest("GET", "/api/me"), response, reason);
        return response;
    }

    @Test
    void answers401WithTheBasicChallenge() throws Exception {
        MockHttpServletResponse response = commence(new BadCredentialsException("Bad credentials"));

        assertEquals(401, response.getStatus());
        assertEquals("Basic realm=\"clinic\"", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertEquals("application/json", response.getContentType());
    }

    @Test
    void theResponseDoesNotDependOnTheReason() throws Exception {
        MockHttpServletResponse badCredentials = commence(new BadCredentialsException("user mallory not found"));
        MockHttpServletResponse anonymous = commence(new InsufficientAuthenticationException("Full authentication is required"));

        assertEquals(badCredentials.getStatus(), anonymous.getStatus());
        assertEquals(badCredentials.getHeader(HttpHeaders.WWW_AUTHENTICATE), anonymous.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertArrayEquals(badCredentials.getContentAsByteArray(), anonymous.getContentAsByteArray());
        assertFalse(new String(badCredentials.getContentAsByteArray(), StandardCharsets.UTF_8).contains("mallory"),
                "the failure reason must not be revealed");
    }
}
