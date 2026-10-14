package edu.rit.swen755.auth.authn;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes the 401 response when a caller is not authenticated: no credentials, a wrong password,
 * or an unknown user, on any URL (including the public one when bad credentials are sent, and
 * including a URL that would also have been forbidden).
 *
 * <p>It is installed both on HTTP Basic (failed credentials) and on exception handling (an
 * anonymous caller refused by a URL rule).
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Writes the challenge.
     *
     * <p>Required behaviour (slice 1: Godson): status 401; header
     * {@code WWW-Authenticate: Basic realm="clinic"}; content type {@code application/json};
     * body {@code {"status":401,"error":"Unauthorized","path":"<request URI>"}}. The body must not
     * depend on why authentication failed, so a wrong password and an unknown user produce
     * byte-identical responses.
     *
     * @param request  the request that failed authentication
     * @param response the response to write
     * @param failure  the reason (deliberately not revealed to the caller)
     * @throws IOException      if the body cannot be written
     * @throws ServletException never in practice; declared by the interface
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException, ServletException {
        throw new UnsupportedOperationException("TODO(slice 1: Godson)");
    }
}
