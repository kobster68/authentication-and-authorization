package edu.rit.swen755.auth;

import edu.rit.swen755.auth.access.AccessPolicy;
import edu.rit.swen755.auth.access.AccessRule;
import edu.rit.swen755.auth.access.AccessRules;
import edu.rit.swen755.auth.access.Requirement.AnyRole;
import edu.rit.swen755.auth.access.Requirement.Authenticated;
import edu.rit.swen755.auth.access.Requirement.PermitAll;
import edu.rit.swen755.auth.access.Role;
import edu.rit.swen755.auth.authn.ClinicUser;
import edu.rit.swen755.auth.authn.DemoUser;
import edu.rit.swen755.auth.authn.DemoUserDetailsService;
import edu.rit.swen755.auth.authn.DemoUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the shared contract that every slice builds on: the application starts, the access
 * table and the seeded users are exactly as frozen, the user store works, and the public service
 * answers through the real filter chain. This class is enabled from the first commit.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ContractSmokeTest {

    /**
     * The demo passwords, by username. This is the one place in the test code where they are
     * spelled out; other test classes read them from here. They are published in the README on
     * purpose (evaluation only) and never appear under {@code src/main}.
     */
    public static final Map<String, String> PASSWORDS = Map.of(
            "admin", "admin-pass",
            "dr.chen", "chen-pass",
            "nurse.patel", "patel-pass",
            "alice", "alice-pass",
            "bob", "bob-pass");

    /**
     * The demo password of a seeded user.
     *
     * @param username a seeded username
     * @return its password
     */
    public static String password(String username) {
        String password = PASSWORDS.get(username);
        if (password == null) {
            throw new IllegalArgumentException("not a seeded user: " + username);
        }
        return password;
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoUserDetailsService users;

    @Autowired
    private AccessPolicy accessPolicy;

    @Test
    void contextLoadsWithThePolicyAsAPlainComponent() {
        // Security 7 applies every Customizer bean to HttpSecurity on its own, so the policy must
        // not be one or its rules would be added twice.
        assertFalse(accessPolicy instanceof Customizer<?>, "AccessPolicy must not be a Customizer bean");
    }

    @Test
    void accessTableHasTheNineRowsInOrder() {
        AnyRole patientOrNurse = new AnyRole(Set.of(Role.PATIENT, Role.NURSE));
        List<AccessRule> expected = List.of(
                new AccessRule(HttpMethod.GET, "/api/public/info", new PermitAll(), false),
                new AccessRule(HttpMethod.GET, "/api/me", new Authenticated(), false),
                new AccessRule(HttpMethod.GET, "/api/patients/{id}/record", patientOrNurse, true),
                new AccessRule(HttpMethod.GET, "/api/patients/{id}/appointments", patientOrNurse, true),
                new AccessRule(HttpMethod.GET, "/api/patients", new AnyRole(Set.of(Role.NURSE)), false),
                new AccessRule(HttpMethod.POST, "/api/patients/{id}/vitals", new AnyRole(Set.of(Role.NURSE)), false),
                new AccessRule(HttpMethod.POST, "/api/patients/{id}/prescriptions", new AnyRole(Set.of(Role.DOCTOR)), false),
                new AccessRule(HttpMethod.GET, "/api/admin/users", new AnyRole(Set.of(Role.ADMIN)), false),
                new AccessRule(HttpMethod.GET, "/api/admin/audit", new AnyRole(Set.of(Role.ADMIN)), false));

        assertEquals(expected, AccessRules.all());
    }

    @Test
    void fiveSeededUsersWhoseHashesMatchTheirPasswords() {
        List<DemoUser> all = DemoUsers.all();
        assertEquals(List.of("admin", "dr.chen", "nurse.patel", "alice", "bob"),
                all.stream().map(DemoUser::username).toList());
        assertEquals(List.of(Role.ADMIN, Role.DOCTOR, Role.NURSE, Role.PATIENT, Role.PATIENT),
                all.stream().map(DemoUser::role).toList());
        assertEquals(List.of(OptionalLong.empty(), OptionalLong.empty(), OptionalLong.empty(),
                        OptionalLong.of(1), OptionalLong.of(2)),
                all.stream().map(DemoUser::patientId).toList());

        for (DemoUser user : all) {
            assertTrue(user.passwordHash().startsWith("{bcrypt}"), user.username() + " is not stored as bcrypt");
            assertTrue(passwordEncoder.matches(password(user.username()), user.passwordHash()),
                    user.username() + "'s hash does not match its password");
            assertFalse(passwordEncoder.matches("wrong", user.passwordHash()));
        }
    }

    @Test
    void userStoreReturnsAFreshClinicUserOnEveryCall() {
        UserDetails first = users.loadUserByUsername("alice");
        UserDetails second = users.loadUserByUsername("alice");

        assertNotSame(first, second, "a shared instance would have its credentials erased after the first login");
        ClinicUser alice = assertInstanceOf(ClinicUser.class, first);
        assertEquals("alice", alice.getUsername());
        assertEquals(Role.PATIENT, alice.role());
        assertEquals(OptionalLong.of(1), alice.patientId());
        assertEquals(List.of("ROLE_PATIENT"),
                alice.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertTrue(alice.getPassword().startsWith("{bcrypt}"));

        ClinicUser bob = assertInstanceOf(ClinicUser.class, users.loadUserByUsername("bob"));
        assertEquals(OptionalLong.of(2), bob.patientId());

        ClinicUser nurse = assertInstanceOf(ClinicUser.class, users.loadUserByUsername("nurse.patel"));
        assertEquals(Role.NURSE, nurse.role());
        assertEquals(OptionalLong.empty(), nurse.patientId());
    }

    @Test
    void unknownUserThrowsUsernameNotFound() {
        assertThrowsExactly(UsernameNotFoundException.class, () -> users.loadUserByUsername("nobody"));
    }

    @Test
    void anonymousPublicInfoIs200ThroughTheRealChain() throws Exception {
        mvc.perform(get("/api/public/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"clinic\":\"SWEN-755 Clinic\",\"status\":\"open\"}", JsonCompareMode.STRICT));
    }
}
