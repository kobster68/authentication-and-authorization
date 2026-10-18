package edu.rit.swen755.auth.access;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiAccessDeniedHandlerTest {

    @Test
    void forbiddenResponseEscapesThePathAndDoesNotRevealTheDenialReason() throws Exception {
        JsonMapper json = JsonMapper.builder().build();
        ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(json);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getOutputStream().write("stale body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String path = "/api/a\"b\\c";

        handler.handle(new MockHttpServletRequest("GET", path), response,
                new AccessDeniedException("private policy detail"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals(response.getContentAsByteArray().length, response.getContentLength());
        assertEquals(json.readTree("{\"status\":403,\"error\":\"Forbidden\",\"path\":\"/api/a\\\"b\\\\c\"}"),
                json.readTree(response.getContentAsByteArray()));
        assertNull(response.getHeader("WWW-Authenticate"));
    }
}
