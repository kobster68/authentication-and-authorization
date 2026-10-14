package edu.rit.swen755.auth.authn;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link ApiAuthenticationEntryPoint} on its own, without a Spring context: the exact
 * response it writes, that the request URI is escaped as JSON, and that the reason for the failure
 * never reaches the caller.
 */
class ApiAuthenticationEntryPointTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(json);

    private MockHttpServletResponse commence(String uri, AuthenticationException reason) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest("GET", uri), response, reason);
        return response;
    }

    @Test
    void writesTheExactChallengeAndBody() throws Exception {
        MockHttpServletResponse response = commence("/api/me", new BadCredentialsException("Bad credentials"));

        assertEquals(401, response.getStatus());
        assertEquals("Basic realm=\"clinic\"", response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/me\"}", response.getContentAsString());
        assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    }

    @Test
    void escapesThePathAsJson() throws Exception {
        String uri = "/api/a\"b\\c</script>";
        MockHttpServletResponse response = commence(uri, new BadCredentialsException("Bad credentials"));

        JsonNode body = json.readTree(response.getContentAsString());
        assertEquals(List.of("status", "error", "path"), List.copyOf(body.propertyNames()));
        assertEquals(uri, body.get("path").asString());
    }

    @Test
    void theBodyDoesNotDependOnTheReason() throws Exception {
        byte[] badCredentials = commence("/api/me", new BadCredentialsException("user mallory not found"))
                .getContentAsByteArray();
        byte[] anonymous = commence("/api/me", new InsufficientAuthenticationException("Full authentication is required"))
                .getContentAsByteArray();

        assertArrayEquals(badCredentials, anonymous);
        assertFalse(new String(badCredentials).contains("mallory"), "the failure reason must not be revealed");
    }
}
