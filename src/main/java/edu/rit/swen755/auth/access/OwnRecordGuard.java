package edu.rit.swen755.auth.access;

import edu.rit.swen755.auth.authn.ClinicUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The per-actor half of Authorize Actors (SAiP 4th ed., p. 174): a patient may read only their
 * own record and appointments.
 *
 * <p>The bean is named {@code ownRecord} because {@code PatientController} calls it from
 * {@code @PreAuthorize("@ownRecord.allows(authentication, #id)")}. Method security runs before
 * the controller body, so a patient asking for another patient's data is refused with 403 before
 * the repository is touched (and therefore before a missing id could produce a 404).
 */
@Component("ownRecord")
public class OwnRecordGuard {

    /**
     * Decides whether the caller may see the given patient's data.
     *
     * <p>Required behaviour (slice 2: Kobe):
     * <ul>
     *   <li>the caller does not hold {@code ROLE_PATIENT} → {@code true} (the URL rule already
     *       admitted only PATIENT or NURSE and above, so any non-patient here is staff);</li>
     *   <li>the caller holds {@code ROLE_PATIENT} and the principal is a
     *       {@link edu.rit.swen755.auth.authn.ClinicUser} whose {@code patientId()} equals
     *       {@code patientId} → {@code true};</li>
     *   <li>any other PATIENT caller (another patient's id, no patient id, or a principal that is
     *       not a {@code ClinicUser}) → {@code false}.</li>
     * </ul>
     *
     * @param auth      the current authentication
     * @param patientId the patient id from the request path
     * @return whether access is allowed
     */
    public boolean allows(Authentication auth, long patientId) {
        boolean patient = auth.getAuthorities().stream()
                .anyMatch(authority -> Role.PATIENT.authority().equals(authority.getAuthority()));
        if (!patient) {
            return true;
        }
        return auth.getPrincipal() instanceof ClinicUser user
                && user.patientId().isPresent()
                && user.patientId().getAsLong() == patientId;
    }
}
