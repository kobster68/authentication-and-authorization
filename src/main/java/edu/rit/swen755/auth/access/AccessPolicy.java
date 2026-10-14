package edu.rit.swen755.auth.access;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * Turns the access table into Spring Security URL rules (the per-role half of Authorize Actors,
 * and Limit Access through default deny; SAiP 4th ed., p. 174).
 *
 * <p>This is a plain component, not a {@code Customizer<HttpSecurity>} bean. Spring Security 7
 * applies every {@code Customizer} bean to {@code HttpSecurity} automatically, so a customizer
 * here would add the rules a second time and the context would fail to start.
 * {@link edu.rit.swen755.auth.config.SecurityConfig} calls {@link #configure} exactly once.
 */
@Component
public class AccessPolicy {

    /**
     * Adds the URL rules to the registry, in order.
     *
     * <p>Required behaviour (slice 2: Kobe):
     * <ol>
     *   <li>{@code dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()} first, so the
     *       container's forward to the error page is not itself denied (otherwise every denial is
     *       published twice and the response body is lost);</li>
     *   <li>then, for each {@link AccessRule} in {@link AccessRules#all()}, in order, a
     *       {@code requestMatchers(rule.method(), rule.pattern())} rule: {@link Requirement.PermitAll}
     *       → {@code permitAll()}, {@link Requirement.Authenticated} → {@code authenticated()},
     *       {@link Requirement.AnyRole} → {@code hasAnyRole(role names)} (the role hierarchy bean
     *       is honoured automatically);</li>
     *   <li>finally {@code anyRequest().denyAll()} (default deny).</li>
     * </ol>
     *
     * <p>Step 0 body (a no-op policy that keeps every other slice's request path working): ERROR
     * dispatch permitted, {@code GET /api/public/info} permitted, everything else denied. Slice 2
     * replaces this body with the table-driven rules described above.
     *
     * @param registry the registry passed in by {@code HttpSecurity.authorizeHttpRequests}
     */
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/info").permitAll()
                .anyRequest().denyAll();
    }
}
