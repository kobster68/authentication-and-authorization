package edu.rit.swen755.auth.access;

/**
 * The four user types of the clinic portal.
 *
 * <p>ADMIN, DOCTOR and NURSE form a hierarchy (ADMIN includes DOCTOR, DOCTOR includes NURSE),
 * declared once in {@link edu.rit.swen755.auth.config.SecurityConfig#roleHierarchy()}. PATIENT
 * sits outside the hierarchy: a patient is never treated as staff, and staff never inherit the
 * patient's own-record access (they have their own, broader rule instead).
 */
public enum Role {
    ADMIN,
    DOCTOR,
    NURSE,
    PATIENT;

    /**
     * The Spring Security authority for this role.
     *
     * @return {@code "ROLE_" + name()}, for example {@code ROLE_NURSE}
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
