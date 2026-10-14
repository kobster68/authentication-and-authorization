package edu.rit.swen755.auth;

import edu.rit.swen755.auth.audit.AuditEntry;
import edu.rit.swen755.auth.audit.AuditLog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared test fixtures: the demo passwords, the JSON request bodies, the audit-log baseline
 * helper, and the parser for {@code scripts/demo-matrix.txt}.
 */
public final class TestFixtures {

    /**
     * The demo passwords, by username: the only place in the Java code where they are spelled out.
     * {@code scripts/demo.sh} keeps its own copy of this table (a shell script cannot read Java);
     * keep the two in step. They are published in the README on purpose (evaluation only) and never
     * appear under {@code src/main}.
     */
    public static final Map<String, String> PASSWORDS = Map.of(
            "admin", "admin-pass",
            "dr.chen", "chen-pass",
            "nurse.patel", "patel-pass",
            "alice", "alice-pass",
            "bob", "bob-pass");

    /** The {@code vitals} request body, as sent by {@code scripts/demo.sh}. */
    public static final String VITALS = "{\"systolic\":120,\"diastolic\":80,\"pulse\":72}";

    /** The {@code prescription} request body, as sent by {@code scripts/demo.sh}. */
    public static final String PRESCRIPTION = "{\"drug\":\"amoxicillin\",\"dose\":\"500mg\"}";

    /** The demo matrix, relative to the project root (Maven runs tests from there). */
    public static final Path DEMO_MATRIX = Path.of("scripts", "demo-matrix.txt");

    private static final Pattern ROW = Pattern.compile("^(\\d{3})\\s+(.*?)\\s*#\\s*needs:\\s*(\\S+)\\s*$");

    private TestFixtures() {
    }

    /**
     * The demo password of a seeded user.
     *
     * @param username a seeded username
     * @return its password
     * @throws IllegalArgumentException if the name is not a seeded user
     */
    public static String password(String username) {
        String password = PASSWORDS.get(username);
        if (password == null) {
            throw new IllegalArgumentException("not a seeded user: " + username);
        }
        return password;
    }

    // ---------------------------------------------------------------- audit log

    /**
     * A point in the audit log, taken before a test acts. All Spring tests share one context and
     * therefore one log, and the log keeps only its most recent entries, so a plain size is not a
     * safe baseline; the last entry itself is.
     *
     * @param last the newest entry at the time, or {@code null} if the log was empty
     */
    public record AuditBaseline(AuditEntry last) {
    }

    /**
     * Marks the current end of the audit log.
     *
     * @param log the audit log
     * @return the baseline
     */
    public static AuditBaseline baseline(AuditLog log) {
        List<AuditEntry> entries = log.entries();
        return new AuditBaseline(entries.isEmpty() ? null : entries.get(entries.size() - 1));
    }

    /**
     * The entries recorded after the baseline, oldest first. If the baseline entry has since been
     * dropped from the log, every remaining entry is newer than it, so all are returned.
     *
     * @param log      the audit log
     * @param baseline taken with {@link #baseline(AuditLog)}
     * @return the newer entries
     */
    public static List<AuditEntry> after(AuditLog log, AuditBaseline baseline) {
        List<AuditEntry> entries = log.entries();
        if (baseline.last() == null) {
            return entries;
        }
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i) == baseline.last()) {
                return entries.subList(i + 1, entries.size());
            }
        }
        return entries;
    }

    // ---------------------------------------------------------------- demo matrix

    /**
     * One row of {@code scripts/demo-matrix.txt}.
     *
     * @param expected the expected status
     * @param method   the HTTP method
     * @param path     the request path
     * @param username the username, or {@code null} for an anonymous request
     * @param password the password ({@code user:password} as written, else the demo password), or
     *                 {@code null} for an anonymous request
     * @param bodyKey  {@code -}, {@code vitals} or {@code prescription}
     * @param needs    the slices the row depends on ({@code 0} = Step 0 alone)
     * @param enabled  whether the row is uncommented
     * @param line     the 1-based line number in the file
     */
    public record MatrixRow(int expected, String method, String path, String username, String password,
                            String bodyKey, Set<String> needs, boolean enabled, int line) {

        /** @return the JSON body to send, or {@code null} for none */
        public String body() {
            return switch (bodyKey) {
                case "-" -> null;
                case "vitals" -> VITALS;
                case "prescription" -> PRESCRIPTION;
                default -> throw new IllegalStateException("unknown body key " + bodyKey);
            };
        }

        /** @return the user column as a display name ({@code -} for anonymous) */
        public String who() {
            return username == null ? "-" : username;
        }
    }

    /**
     * Parses every row of the demo matrix, the commented-out ones included. A row is a line that,
     * after an optional leading {@code #}, starts with a three-digit status; the header comment,
     * section comments and blank lines are skipped. Any row that does not have the five columns,
     * a known body key, a known user (or {@code user:password}) and a {@code needs:} field fails.
     *
     * @return the rows in file order
     */
    public static List<MatrixRow> demoMatrix() {
        List<String> lines;
        try {
            lines = Files.readAllLines(DEMO_MATRIX);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<MatrixRow> rows = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i).strip();
            boolean enabled = !text.startsWith("#");
            if (!enabled) {
                text = text.substring(1).strip();
            }
            if (text.isEmpty() || !Character.isDigit(text.charAt(0))) {
                continue;
            }
            int lineNo = i + 1;
            Matcher m = ROW.matcher(text);
            if (!m.matches()) {
                throw new IllegalStateException(DEMO_MATRIX + ":" + lineNo + ": row without a '# needs:' field: " + lines.get(i));
            }
            String[] cols = m.group(2).split("\\s+");
            if (cols.length != 4) {
                throw new IllegalStateException(DEMO_MATRIX + ":" + lineNo + ": expected 5 columns: " + lines.get(i));
            }
            String user = cols[2];
            String username = null;
            String password = null;
            if (!user.equals("-")) {
                int colon = user.indexOf(':');
                username = colon < 0 ? user : user.substring(0, colon);
                password = colon < 0 ? password(user) : user.substring(colon + 1);
            }
            if (!Set.of("-", "vitals", "prescription").contains(cols[3])) {
                throw new IllegalStateException(DEMO_MATRIX + ":" + lineNo + ": unknown body key " + cols[3]);
            }
            Set<String> needs = Set.copyOf(Arrays.asList(m.group(3).split(",")));
            rows.add(new MatrixRow(Integer.parseInt(m.group(1)), cols[0], cols[1], username, password,
                    cols[3], needs, enabled, lineNo));
        }
        return rows;
    }
}
