package edu.rit.swen755.auth.access;

import edu.rit.swen755.auth.access.Requirement.AnyRole;
import edu.rit.swen755.auth.access.Requirement.Authenticated;
import edu.rit.swen755.auth.access.Requirement.PermitAll;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Set;

/**
 * The access table: every service the portal offers and who may call it, as data.
 *
 * <p>This is the single source of the URL rules. {@link AccessPolicy} generates the Spring
 * Security rules from it, the tests assert against it, and the README's URL table is checked
 * against it, so the table is written once and cannot drift. Anything not listed here is denied
 * to everyone (default deny); that final rule is added by {@link AccessPolicy}, not listed here.
 *
 * <p>"NURSE+" below means NURSE or any role above it in the hierarchy (DOCTOR, ADMIN).
 *
 * <table>
 *   <caption>The access table, in order</caption>
 *   <tr><th>#</th><th>Method</th><th>URL pattern</th><th>Requirement</th><th>Own-record rule</th></tr>
 *   <tr><td>1</td><td>GET</td><td>/api/public/info</td><td>permitAll</td><td></td></tr>
 *   <tr><td>2</td><td>GET</td><td>/api/me</td><td>authenticated</td><td></td></tr>
 *   <tr><td>3</td><td>GET</td><td>/api/patients/{id}/record</td><td>PATIENT or NURSE+</td><td>yes</td></tr>
 *   <tr><td>4</td><td>GET</td><td>/api/patients/{id}/appointments</td><td>PATIENT or NURSE+</td><td>yes</td></tr>
 *   <tr><td>5</td><td>GET</td><td>/api/patients</td><td>NURSE+</td><td></td></tr>
 *   <tr><td>6</td><td>POST</td><td>/api/patients/{id}/vitals</td><td>NURSE+</td><td></td></tr>
 *   <tr><td>7</td><td>POST</td><td>/api/patients/{id}/prescriptions</td><td>DOCTOR+</td><td></td></tr>
 *   <tr><td>8</td><td>GET</td><td>/api/admin/users</td><td>ADMIN</td><td></td></tr>
 *   <tr><td>9</td><td>GET</td><td>/api/admin/audit</td><td>ADMIN</td><td></td></tr>
 * </table>
 */
public final class AccessRules {

    private static final List<AccessRule> ALL = List.of(
            new AccessRule(HttpMethod.GET, "/api/public/info", new PermitAll(), false),
            new AccessRule(HttpMethod.GET, "/api/me", new Authenticated(), false),
            new AccessRule(HttpMethod.GET, "/api/patients/{id}/record",
                    new AnyRole(Set.of(Role.PATIENT, Role.NURSE)), true),
            new AccessRule(HttpMethod.GET, "/api/patients/{id}/appointments",
                    new AnyRole(Set.of(Role.PATIENT, Role.NURSE)), true),
            new AccessRule(HttpMethod.GET, "/api/patients", new AnyRole(Set.of(Role.NURSE)), false),
            new AccessRule(HttpMethod.POST, "/api/patients/{id}/vitals", new AnyRole(Set.of(Role.NURSE)), false),
            new AccessRule(HttpMethod.POST, "/api/patients/{id}/prescriptions", new AnyRole(Set.of(Role.DOCTOR)), false),
            new AccessRule(HttpMethod.GET, "/api/admin/users", new AnyRole(Set.of(Role.ADMIN)), false),
            new AccessRule(HttpMethod.GET, "/api/admin/audit", new AnyRole(Set.of(Role.ADMIN)), false));

    private AccessRules() {
    }

    /**
     * The nine rows of the access table, in the order the URL rules must be applied.
     *
     * @return an unmodifiable list
     */
    public static List<AccessRule> all() {
        return ALL;
    }
}
