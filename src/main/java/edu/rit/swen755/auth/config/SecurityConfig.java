package edu.rit.swen755.auth.config;

import edu.rit.swen755.auth.access.AccessPolicy;
import edu.rit.swen755.auth.access.ApiAccessDeniedHandler;
import edu.rit.swen755.auth.authn.ApiAuthenticationEntryPoint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Wires the single security filter chain and the shared security beans.
 *
 * <p>This class only connects beans; every behaviour lives in a class owned by one slice:
 * the URL rules in {@link AccessPolicy}, the 401 response in
 * {@link ApiAuthenticationEntryPoint}, the 403 response in {@link ApiAccessDeniedHandler}, the
 * user store in {@link edu.rit.swen755.auth.authn.DemoUserDetailsService}, and the per-actor
 * check in {@link edu.rit.swen755.auth.access.OwnRecordGuard} (reached through method
 * security). The chain is the textbook's Intercepting Filter (SAiP 4th ed., p. 194): every
 * request crosses it before any controller runs.
 *
 * <p>The class is part of the frozen contract. A change to it goes through its own pull request.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * The one filter chain for the API.
     *
     * <ul>
     *   <li>URL authorization is delegated to {@link AccessPolicy#configure}. {@code AccessPolicy}
     *       is deliberately not a {@code Customizer} bean: Spring Security 7 applies every
     *       {@code Customizer<HttpSecurity>} bean automatically, so the rules would be added twice.</li>
     *   <li>HTTP Basic authenticates every request; failures go to the 401 entry point. The entry
     *       point is also set on exception handling so that an anonymous caller refused by a URL
     *       rule gets the same 401 (set twice on purpose, for explicitness).</li>
     *   <li>CSRF protection is off and no session is created. That is safe only because there is no
     *       browser client: the API is called with curl and test code, which do not send ambient
     *       credentials (https://docs.spring.io/spring-security/reference/features/exploits/csrf.html).</li>
     * </ul>
     *
     * @param http       the builder supplied by Spring Security
     * @param policy     the URL rules
     * @param entryPoint the 401 response
     * @param denied     the 403 response
     * @return the built chain
     * @throws Exception if the builder rejects the configuration
     */
    @Bean
    SecurityFilterChain api(HttpSecurity http,
                            AccessPolicy policy,
                            ApiAuthenticationEntryPoint entryPoint,
                            ApiAccessDeniedHandler denied) throws Exception {
        http.authorizeHttpRequests(policy::configure)
                .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * The staff role hierarchy: ADMIN includes DOCTOR, and DOCTOR includes NURSE. PATIENT sits
     * outside it. Each URL rule can therefore name its minimum staff role, and the same bean is
     * honoured by method security. Declared {@code static} as the Spring Security reference does,
     * so it is available before the configuration class itself is processed.
     *
     * @return the hierarchy
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_DOCTOR\nROLE_DOCTOR > ROLE_NURSE");
    }

    /**
     * A delegating encoder: it reads the {@code {id}} prefix of a stored hash, so the
     * {@code {bcrypt}} hashes in {@link edu.rit.swen755.auth.authn.DemoUsers} are checked with
     * BCrypt (https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
     *
     * @return the encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Publishes authorization decisions as application events. Spring Security does not publish
     * them unless this bean exists; with it, URL denials and method-security denials both arrive
     * as {@code AuthorizationDeniedEvent}s for the audit listener.
     *
     * @param publisher the application event publisher
     * @return the authorization event publisher
     */
    @Bean
    AuthorizationEventPublisher authorizationEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringAuthorizationEventPublisher(publisher);
    }
}
