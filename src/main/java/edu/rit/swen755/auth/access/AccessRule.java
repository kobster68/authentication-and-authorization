package edu.rit.swen755.auth.access;

import org.springframework.http.HttpMethod;

/**
 * One row of the access table: a service (HTTP method and URL pattern), the requirement a caller
 * must meet, and whether the per-actor own-record rule also applies.
 *
 * <p>When {@code ownRecord} is true, the URL rule is only the first check. The controller method
 * behind the URL carries {@code @PreAuthorize("@ownRecord.allows(authentication, #id)")}, so a
 * patient can still be refused another patient's data by {@link OwnRecordGuard}.
 *
 * @param method      the HTTP method
 * @param pattern     the URL pattern in Spring path-pattern syntax, for example
 *                    {@code /api/patients/{id}/record}
 * @param requirement what the caller must satisfy
 * @param ownRecord   whether the own-record rule also applies
 */
public record AccessRule(HttpMethod method, String pattern, Requirement requirement, boolean ownRecord) {
}
