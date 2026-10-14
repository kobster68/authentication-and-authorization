package edu.rit.swen755.auth.audit;

/**
 * The kinds of security event the audit log records.
 */
public enum AuditKind {
    /** A request carried valid credentials. Under stateless Basic this is every authenticated request. */
    AUTHN_SUCCESS,
    /** A request carried credentials that were rejected (wrong password or unknown user). */
    AUTHN_FAILURE,
    /** A request was refused by a URL rule or by the own-record check. */
    AUTHZ_DENIED
}
