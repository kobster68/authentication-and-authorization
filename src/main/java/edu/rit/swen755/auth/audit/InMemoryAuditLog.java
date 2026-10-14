package edu.rit.swen755.auth.audit;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An in-memory, unbounded audit log, safe for concurrent requests.
 *
 * <p>It lives as long as the process; nothing is persisted. That is enough to show the attempts
 * made during a demo through {@code GET /api/admin/audit}.
 */
@Component
public class InMemoryAuditLog implements AuditLog {

    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public synchronized void record(AuditEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry"));
    }

    @Override
    public synchronized List<AuditEntry> entries() {
        return List.copyOf(entries);
    }
}
