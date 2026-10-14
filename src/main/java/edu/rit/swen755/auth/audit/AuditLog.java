package edu.rit.swen755.auth.audit;

import java.util.List;

/**
 * Where security events are recorded (the Audit tactic, SAiP 4th ed., p. 176).
 */
public interface AuditLog {

    /**
     * Appends an entry.
     *
     * @param entry the entry to record
     */
    void record(AuditEntry entry);

    /**
     * Every entry recorded so far.
     *
     * @return an unmodifiable snapshot, oldest first
     */
    List<AuditEntry> entries();
}
