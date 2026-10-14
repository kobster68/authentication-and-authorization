package edu.rit.swen755.auth.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells an authenticated caller who they are.
 */
@RestController
public class MeController {

    /**
     * The JSON body of {@code GET /api/me}.
     *
     * @param username  the login name
     * @param role      the role name without the {@code ROLE_} prefix, for example {@code NURSE}
     * @param patientId the caller's patient id for a PATIENT; {@code null} (written as JSON null)
     *                  for staff
     */
    public record Me(String username, String role, Long patientId) {
    }

    /**
     * {@code GET /api/me}.
     *
     * <p>Required behaviour (slice 3: Chase): when the principal is a
     * {@link edu.rit.swen755.auth.authn.ClinicUser}, its username, role and patient id; otherwise
     * {@code authentication.getName()}, the first {@code ROLE_*} authority without the prefix, and
     * a null patient id. Never throws for a principal that is not a {@code ClinicUser}.
     *
     * <p>The authentication is taken from the security context ({@code @CurrentSecurityContext})
     * rather than from the servlet request's user principal, so it is also present when tests run
     * the controller with the security filters switched off.
     *
     * @param authentication the current authentication
     * @return who the caller is
     */
    @GetMapping("/api/me")
    public Me me(@CurrentSecurityContext(expression = "authentication") Authentication authentication) {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }
}
