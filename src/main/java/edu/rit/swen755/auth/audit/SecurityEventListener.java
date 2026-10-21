package edu.rit.swen755.auth.audit;

import edu.rit.swen755.auth.access.AccessRule;
import edu.rit.swen755.auth.access.AccessRules;
import edu.rit.swen755.auth.access.Requirement;
import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns Spring Security's authentication and authorization events into audit entries, so no
 * controller has to record anything itself.
 *
 * <p>Common rules for every entry:
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
 */
@Component
public class SecurityEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityEventListener.class);

    private final AuditLog log;
    private final List<RuleMatcher> ruleMatchers;

    /**
     * @param log where entries are recorded
     */
    public SecurityEventListener(AuditLog log) {
        this.log = log;
        this.ruleMatchers = AccessRules.all().stream()
                .map(rule -> new RuleMatcher(rule,
                        PathPatternRequestMatcher.pathPattern(rule.method(), rule.pattern())))
                .toList();
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
     * decision, the reason read from the access-table row whose matcher matches the request
     * ({@code authenticated} when that row's requirement is {@code authenticated}, otherwise
     * {@code denyAll}, as for a request that matches no row); when the event object is a
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
            detail = bareDenialDetail(event.getObject());
        }
        record(AuditKind.AUTHZ_DENIED, username, detail);
    }

    /**
     * The reason a bare (non-role) denial is recorded with, read from the access table. The first
     * {@link AccessRule} whose matcher matches the request decides: {@code authenticated} when its
     * requirement is {@link Requirement.Authenticated}, otherwise {@code denyAll}. A request that
     * is not an {@link HttpServletRequest}, or that matches no row (for example {@code denyAll}'s
     * {@code anyRequest()}), is {@code denyAll}.
     *
     * @param object the denial event's object
     * @return {@code authenticated} or {@code denyAll}
     */
    private String bareDenialDetail(Object object) {
        if (object instanceof HttpServletRequest request) {
            for (RuleMatcher ruleMatcher : ruleMatchers) {
                if (ruleMatcher.matcher().matches(request)) {
                    return ruleMatcher.rule().requirement() instanceof Requirement.Authenticated
                            ? "authenticated" : "denyAll";
                }
            }
        }
        return "denyAll";
    }

    /**
     * Records one entry and writes it to the application log. After the {@code anonymousUser}
     * mapping, the username is passed through {@link #sanitize(String)} so control characters and
     * line separators are escaped and a crafted username cannot forge a second audit line in the
     * console log; the one sanitised value is used for both the stored entry and the log line.
     *
     * @param kind     the kind of event
     * @param username the raw username
     * @param detail   the reason detail
     */
    private void record(AuditKind kind, String username, String detail) {
        RequestDetails request = requestDetails();
        String recordedUsername = sanitize("anonymousUser".equals(username) ? "anonymous" : username);
        log.record(new AuditEntry(Instant.now(), kind, recordedUsername, request.method, request.path, detail));
        LOGGER.info("SECURITY_AUDIT kind={} user={} method={} path={} detail={}", kind, recordedUsername,
                request.method, request.path, detail);
    }

    /**
     * Escapes a value so a crafted username cannot forge a log line: every backslash is doubled,
     * then each control character and Unicode line separator is rewritten so the value always
     * stays on one line. The set escaped is {@code Character.isISOControl} plus NEL (U+0085), LINE
     * SEPARATOR (U+2028) and PARAGRAPH SEPARATOR (U+2029); each becomes {@code \n}, {@code \r},
     * {@code \t}, or a backslash-u escape with four uppercase hex digits. Every other character is
     * kept, so no information is lost.
     *
     * @param value the raw value
     * @return the value with backslashes, control characters and line separators escaped
     */
    private static String sanitize(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                escaped.append("\\\\");
            } else if (Character.isISOControl(c) || c == '\u0085' || c == '\u2028' || c == '\u2029') {
                switch (c) {
                    case '\n' -> escaped.append("\\n");
                    case '\r' -> escaped.append("\\r");
                    case '\t' -> escaped.append("\\t");
                    default -> escaped.append(String.format("\\u%04X", (int) c));
                }
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
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

    private record RuleMatcher(AccessRule rule, RequestMatcher matcher) {
    }
}
