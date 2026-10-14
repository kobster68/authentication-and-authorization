package edu.rit.swen755.auth.audit;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Turns Spring Security's authentication and authorization events into audit entries, so no
 * controller has to record anything itself.
 *
 * <p>Common rules for every entry (slice 3: Chase):
 * <ul>
 *   <li>username: {@code event.getAuthentication().get().getName()} for an authorization event
 *       (the authentication's name for an authentication event), with {@code anonymousUser}
 *       recorded as {@code anonymous};</li>
 *   <li>method and path: from {@code RequestContextHolder.getRequestAttributes()} when it is a
 *       {@code ServletRequestAttributes} ({@code getMethod()} and {@code getRequestURI()}),
 *       otherwise {@code -};</li>
 *   <li>each entry is appended to the {@link AuditLog} and written to the application log at INFO
 *       as {@code SECURITY_AUDIT kind=.. user=.. method=.. path=.. detail=..}.</li>
 * </ul>
 *
 * <p>Step 0 ships the three listener methods with empty bodies, so every request path works
 * while slice 3 is being built.
 */
@Component
public class SecurityEventListener {

    private final AuditLog log;

    /**
     * @param log where entries are recorded
     */
    public SecurityEventListener(AuditLog log) {
        this.log = log;
    }

    /**
     * Records {@link AuditKind#AUTHN_SUCCESS}. Under stateless HTTP Basic this fires on every
     * authenticated request, not once per login. Detail: {@code -}.
     *
     * @param event the success event
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        // Step 0: intentionally empty; slice 3 records the entry.
    }

    /**
     * Records {@link AuditKind#AUTHN_FAILURE}. Detail: the simple class name of the exception,
     * for example {@code BadCredentialsException}.
     *
     * @param event the failure event
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        // Step 0: intentionally empty; slice 3 records the entry.
    }

    /**
     * Records {@link AuditKind#AUTHZ_DENIED}. Detail: for a role decision
     * ({@code AuthorityAuthorizationDecision}) the required {@code ROLE_*} authorities; for a bare
     * decision {@code denyAll} or {@code authenticated}; when the event object is a
     * {@code MethodInvocation} (an own-record denial), the method name.
     *
     * @param event the denial event
     */
    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> event) {
        // Step 0: intentionally empty; slice 3 records the entry.
    }
}
