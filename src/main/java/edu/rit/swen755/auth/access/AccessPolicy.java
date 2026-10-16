package edu.rit.swen755.auth.access;

import jakarta.servlet.DispatcherType;
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
     * @param registry the registry passed in by {@code HttpSecurity.authorizeHttpRequests}
     */
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
        for (AccessRule rule : AccessRules.all()) {
            var matcher = registry.requestMatchers(rule.method(), rule.pattern());
            switch (rule.requirement()) {
                case Requirement.PermitAll ignored -> matcher.permitAll();
                case Requirement.Authenticated ignored -> matcher.authenticated();
                case Requirement.AnyRole requirement -> matcher.hasAnyRole(
                        requirement.roles().stream().map(Role::name).sorted().toArray(String[]::new));
            }
        }
        registry.anyRequest().denyAll();
    }
}
