package edu.rit.swen755.auth.authn;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * The user store that Spring Security's {@code DaoAuthenticationProvider} consults during HTTP
 * Basic authentication (Authenticate Actors, SAiP 4th ed., p. 174).
 *
 * <p>Declaring this bean also switches off Spring Boot's generated default user.
 */
@Service
public class DemoUserDetailsService implements UserDetailsService {

    /**
     * Looks up a seeded user.
     *
     * <p>A new {@link ClinicUser} is built on every call. Spring Security erases the credentials
     * on the principal it returns after a successful login, so handing out a shared instance
     * would make every later login of the same user fail.
     *
     * <p>An unknown name throws {@link UsernameNotFoundException} and nothing else.
     * {@code DaoAuthenticationProvider} turns that into the same {@code BadCredentialsException}
     * as a wrong password (and still runs a dummy BCrypt check), so a caller cannot tell an
     * unknown user from a wrong password.
     *
     * @param username the name sent by the caller
     * @return a fresh principal for that user
     * @throws UsernameNotFoundException if no seeded user has that name
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return DemoUsers.all().stream()
                .filter(u -> u.username().equals(username))
                .findFirst()
                .map(ClinicUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("unknown user"));
    }
}
