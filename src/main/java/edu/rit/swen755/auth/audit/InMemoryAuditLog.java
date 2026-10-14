package edu.rit.swen755.auth.audit;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * An in-memory audit log, safe for concurrent requests, that keeps the newest 1000 entries.
 *
 * <p>When the log is full, recording a new entry drops the oldest one, so memory stays bounded
 * however long the server runs (a full demo run records on the order of a hundred entries).
 * Nothing is persisted; the entries live as long as the process and are shown through
 * {@code GET /api/admin/audit}.
 */
@Component
public class InMemoryAuditLog implements AuditLog {

    private static final int MAX_ENTRIES = 1000;

    private final Deque<AuditEntry> entries = new ArrayDeque<>();

    @Override
    public synchronized void record(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (entries.size() == MAX_ENTRIES) {
            entries.removeFirst();
        }
        entries.addLast(entry);
    }

    @Override
    public synchronized List<AuditEntry> entries() {
        return List.copyOf(entries);
    }
}
