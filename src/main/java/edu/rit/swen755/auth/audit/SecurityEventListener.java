package edu.rit.swen755.auth.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityEventListener.class);

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
        record(AuditKind.AUTHN_SUCCESS, event.getAuthentication().getName(), "-");
    }

    /**
     * Records {@link AuditKind#AUTHN_FAILURE}. Detail: the simple class name of the exception,
     * for example {@code BadCredentialsException}.
     *
     * @param event the failure event
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        record(AuditKind.AUTHN_FAILURE, event.getAuthentication().getName(),
            event.getException().getClass().getSimpleName());
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
        String username = event.getAuthentication().get().getName();
        String detail;
        if (event.getObject() instanceof MethodInvocation invocation) {
            detail = invocation.getMethod().getName();
        } else if (event.getAuthorizationResult() instanceof AuthorityAuthorizationDecision authorityDecision) {
            detail = authorityDecision.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(","));
        } else {
            String path = requestDetails().path;
            detail = "/api/me".equals(path) ? "authenticated" : "denyAll";
        }
        record(AuditKind.AUTHZ_DENIED, username, detail);
    }

    private void record(AuditKind kind, String username, String detail) {
        RequestDetails request = requestDetails();
        String recordedUsername = "anonymousUser".equals(username) ? "anonymous" : username;
        log.record(new AuditEntry(Instant.now(), kind, recordedUsername, request.method, request.path, detail));
        LOGGER.info("SECURITY_AUDIT kind={} user={} method={} path={} detail={}", kind, recordedUsername,
                request.method, request.path, detail);
    }

    private static RequestDetails requestDetails() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return new RequestDetails(request.getMethod(), request.getRequestURI());
        }
        return new RequestDetails("-", "-");
    }

    private record RequestDetails(String method, String path) {
    }
}
