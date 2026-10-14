package edu.rit.swen755.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the clinic portal.
 *
 * <p>The portal is a small HTTP API whose services are identified by method and URL. Every
 * request passes through one Spring Security filter chain (see
 * {@link edu.rit.swen755.auth.config.SecurityConfig}), which authenticates the caller with HTTP
 * Basic and then authorizes the request against the access table in
 * {@link edu.rit.swen755.auth.access.AccessRules}.
 */
@SpringBootApplication
public class AuthApplication {

    /**
     * Starts the embedded server. Arguments such as {@code --server.port=9090} pass through to
     * Spring Boot.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
