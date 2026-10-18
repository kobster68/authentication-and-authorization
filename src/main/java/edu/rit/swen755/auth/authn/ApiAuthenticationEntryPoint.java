package edu.rit.swen755.auth.authn;

import edu.rit.swen755.auth.web.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

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

    /** The challenge sent with every 401: the client should retry with HTTP Basic credentials. */
    static final String CHALLENGE = "Basic realm=\"clinic\"";

    private final ObjectMapper json;

    /**
     * @param json the application's JSON mapper, so the body is escaped like any other response
     */
    public ApiAuthenticationEntryPoint(ObjectMapper json) {
        this.json = json;
    }

    /**
     * Writes the challenge: status 401, the {@link #CHALLENGE} header, and an {@link ApiError}
     * body. Nothing in the response depends on why authentication failed, so a wrong password and
     * an unknown user produce byte-identical responses.
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
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, CHALLENGE);
        ApiError.write(response, json, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", request);
    }
}
