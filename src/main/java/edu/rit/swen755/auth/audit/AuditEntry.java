package edu.rit.swen755.auth.audit;

import java.time.Instant;

/**
 * One recorded security event.
 *
 * @param at       when it happened
 * @param kind     what happened
 * @param username who it happened to ({@code anonymous} for an unauthenticated caller)
 * @param method   the HTTP method, or {@code -} when no request is in scope
 * @param path     the request URI, or {@code -} when no request is in scope
 * @param detail   the reason: the exception name for a failed login, the required roles or rule
 *                 for a denial
 */
public record AuditEntry(Instant at, AuditKind kind, String username, String method, String path, String detail) {
}
