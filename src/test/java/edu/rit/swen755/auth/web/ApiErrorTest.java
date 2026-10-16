package edu.rit.swen755.auth.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ApiError#write}, without a Spring context: the exact body, its content
 * type and length, JSON escaping of the path, and a clean body even when something was already
 * buffered.
 *
 * <p>These tests use a plain {@link JsonMapper} and check only this class's own logic. The path
 * through the mapper that Spring Boot configures, and the real filter chain, is covered by
 * {@code AuthenticationTest}.
 */
class ApiErrorTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private MockHttpServletResponse write(String uri, MockHttpServletResponse response) throws Exception {
        ApiError.write(response, json, 401, "Unauthorized", new MockHttpServletRequest("GET", uri));
        return response;
    }

    @Test
    void writesTheExactBodyWithJsonContentTypeAndLength() throws Exception {
        MockHttpServletResponse response = write("/api/me", new MockHttpServletResponse());

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/me\"}", response.getContentAsString());
        assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    }

    @Test
    void escapesThePathAsJson() throws Exception {
        String uri = "/api/a\"b\\c</script>";
        MockHttpServletResponse response = write(uri, new MockHttpServletResponse());

        JsonNode body = json.readTree(response.getContentAsString());
        assertEquals(List.of("status", "error", "path"), List.copyOf(body.propertyNames()));
        assertEquals(uri, body.get("path").asString());
    }

    @Test
    void replacesAnythingAlreadyBufferedAndTheStaleStatusAndContentType() throws Exception {
        MockHttpServletResponse stale = new MockHttpServletResponse();
        stale.setStatus(200);
        stale.setContentType("text/html");
        stale.setHeader(HttpHeaders.CONTENT_LENGTH, "999");
        stale.getOutputStream().write("<html>partial".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = write("/api/me", stale);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"status\":401,\"error\":\"Unauthorized\",\"path\":\"/api/me\"}", response.getContentAsString());
        assertEquals(response.getContentAsByteArray().length, response.getContentLength());
    }
}
