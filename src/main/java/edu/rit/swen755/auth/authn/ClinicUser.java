package edu.rit.swen755.auth.authn;

import edu.rit.swen755.auth.access.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.OptionalLong;

/**
 * The authenticated principal: a Spring Security {@link User} that also carries the user's role
 * and, for a patient, their patient id.
 *
 * <p>The own-record rule needs the patient id of the caller, which a plain {@code User} does not
 * have; carrying it on the principal means no second lookup is needed during authorization.
 */
public class ClinicUser extends User {

    private final Role role;
    private final Long patientId;

    /**
     * Builds the principal for a seeded user. Its only authority is the role's
     * ({@code ROLE_<name>}).
     *
     * @param user the seeded user
     */
    public ClinicUser(DemoUser user) {
        super(user.username(), user.passwordHash(), List.of(new SimpleGrantedAuthority(user.role().authority())));
        this.role = user.role();
        this.patientId = user.patientId().isPresent() ? user.patientId().getAsLong() : null;
    }

    /**
     * The user's role.
     *
     * @return the role
     */
    public Role role() {
        return role;
    }

    /**
     * The patient this user is.
     *
     * @return the patient id for a PATIENT; empty for staff
     */
    public OptionalLong patientId() {
        return patientId == null ? OptionalLong.empty() : OptionalLong.of(patientId);
    }
}
