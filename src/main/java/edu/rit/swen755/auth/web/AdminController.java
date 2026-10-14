package edu.rit.swen755.auth.web;

import edu.rit.swen755.auth.audit.AuditEntry;
import edu.rit.swen755.auth.audit.AuditLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The administrator services. The URL rules admit only ADMIN.
 */
@RestController
public class AdminController {

    private final AuditLog auditLog;

    /**
     * @param auditLog the audit log to expose
     */
    public AdminController(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    /**
     * One element of the {@code GET /api/admin/users} body.
     *
     * @param username the login name
     * @param role     the role name, for example {@code DOCTOR}
     */
    public record UserSummary(String username, String role) {
    }

    /**
     * {@code GET /api/admin/users} (slice 3: Chase): one entry per user in
     * {@link edu.rit.swen755.auth.authn.DemoUsers#all()}, in that order, with username and role
     * only. Password hashes are never exposed.
     *
     * @return the users
     */
    @GetMapping("/api/admin/users")
    public List<UserSummary> users() {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }

    /**
     * {@code GET /api/admin/audit} (slice 3: Chase): {@link AuditLog#entries()}, oldest first,
     * with {@code at} written as ISO-8601 text.
     *
     * @return the audit entries
     */
    @GetMapping("/api/admin/audit")
    public List<AuditEntry> audit() {
        throw new UnsupportedOperationException("TODO(slice 3: Chase)");
    }
}
