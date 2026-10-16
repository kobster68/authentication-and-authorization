package edu.rit.swen755.auth.access;

import edu.rit.swen755.auth.web.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes the 403 response for an authenticated caller who is not permitted to call a service.
 *
 * <p>Spring Security's {@code ExceptionTranslationFilter} sends an access denial here only when
 * the caller is authenticated; an anonymous caller who is denied gets the 401 entry point
 * instead, so 401 always means "who are you?" and 403 always means "you may not".
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper json;

    public ApiAccessDeniedHandler(ObjectMapper json) {
        this.json = json;
    }

    /**
     * Writes the denial.
     *
     * <p>Required behaviour (slice 2: Kobe): status 403, content type {@code application/json},
     * body {@code {"status":403,"error":"Forbidden","path":"<request URI>"}}.
     *
     * @param request  the denied request
     * @param response the response to write
     * @param denied   the denial
     * @throws IOException      if the body cannot be written
     * @throws ServletException never in practice; declared by the interface
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied)
            throws IOException, ServletException {
        ApiError.write(response, json, HttpServletResponse.SC_FORBIDDEN, "Forbidden", request);
    }
}
