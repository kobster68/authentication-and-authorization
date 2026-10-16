package edu.rit.swen755.auth.web;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * The JSON body of an error written by the security layer, for example
 * {@code {"status":401,"error":"Unauthorized","path":"/api/me"}}.
 *
 * <p>The property order is fixed so the body reads the same in every response and in the
 * documentation.
 *
 * @param status the HTTP status code
 * @param error  the status reason phrase, such as {@code Unauthorized} or {@code Forbidden}
 * @param path   the request URI
 */
@JsonPropertyOrder({"status", "error", "path"})
public record ApiError(int status, String error, String path) {

    /**
     * Writes an error response: discards any buffered body, then sets the status, the
     * {@code application/json} content type, the content length and the JSON body. Headers that
     * are specific to the status (such as {@code WWW-Authenticate}) are the caller's job.
     *
     * <p>The body is serialised with the given mapper, so the request URI is escaped as JSON.
     *
     * @param response the response to write
     * @param mapper   the JSON mapper
     * @param status   the HTTP status code
     * @param error    the status reason phrase
     * @param request  the request whose URI goes into {@code path}
     * @throws IOException if the body cannot be written
     */
    public static void write(HttpServletResponse response, ObjectMapper mapper, int status, String error,
                             HttpServletRequest request) throws IOException {
        byte[] body = mapper.writeValueAsBytes(new ApiError(status, error, request.getRequestURI()));
        if (!response.isCommitted()) {
            response.resetBuffer();
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }
}
