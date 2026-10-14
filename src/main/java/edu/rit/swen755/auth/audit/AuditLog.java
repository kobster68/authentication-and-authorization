package edu.rit.swen755.auth.audit;

import java.util.List;

/**
 * Where security events are recorded (the Audit tactic, SAiP 4th ed., p. 176).
 *
 * <p>The log is bounded: it keeps the newest 1000 entries and drops the oldest when a new entry
 * arrives on a full log.
 */
public interface AuditLog {

    /**
     * Appends an entry, dropping the oldest entry if the log already holds 1000.
     *
     * @param entry the entry to record
     */
    void record(AuditEntry entry);

    /**
     * The entries currently held (at most the newest 1000).
     *
     * @return an unmodifiable snapshot, oldest first
     */
    List<AuditEntry> entries();
}
