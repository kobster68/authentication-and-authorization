package edu.rit.swen755.auth;

import edu.rit.swen755.auth.audit.AuditEntry;
import edu.rit.swen755.auth.audit.AuditKind;
import edu.rit.swen755.auth.audit.AuditLog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static edu.rit.swen755.auth.ContractSmokeTest.password;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every cell of the access matrix, through the real filter chain with real HTTP Basic
 * credentials, plus the audit entries for a failed login and a denial.
 *
 * <p>This is the cross-slice evidence: it needs authentication, authorization and the services
 * together, so it is enabled by whichever of slices 1 to 3 merges last.
 */
@Disabled("TODO(last slice to merge) — enable when slices 1–3 are all on main")
@SpringBootTest
@AutoConfigureMockMvc
class AccessMatrixTest {

    private static final String VITALS = "{\"systolic\":120,\"diastolic\":80,\"pulse\":72}";
    private static final String PRESCRIPTION = "{\"drug\":\"amoxicillin\",\"dose\":\"500mg\"}";

    /** Column order of the matrix: anonymous, then the five seeded users. */
    private static final List<String> ACTORS = List.of("-", "admin", "dr.chen", "nurse.patel", "alice", "bob");

    /**
     * The access matrix: method, path, body, then the expected status for each actor in
     * {@link #ACTORS} order.
     */
    private static final Object[][] MATRIX = {
            {"GET", "/api/public/info", "-", 200, 200, 200, 200, 200, 200},
            {"GET", "/api/me", "-", 401, 200, 200, 200, 200, 200},
            {"GET", "/api/patients/1/record", "-", 401, 200, 200, 200, 200, 403},
            {"GET", "/api/patients/2/record", "-", 401, 200, 200, 200, 403, 200},
            {"GET", "/api/patients/1/appointments", "-", 401, 200, 200, 200, 200, 403},
            {"GET", "/api/patients/99/record", "-", 401, 404, 404, 404, 403, 403},
            {"GET", "/api/patients", "-", 401, 200, 200, 200, 403, 403},
            {"POST", "/api/patients/1/vitals", VITALS, 401, 201, 201, 201, 403, 403},
            {"POST", "/api/patients/1/prescriptions", PRESCRIPTION, 401, 201, 201, 403, 403, 403},
            {"GET", "/api/admin/users", "-", 401, 200, 403, 403, 403, 403},
            {"GET", "/api/admin/audit", "-", 401, 200, 403, 403, 403, 403},
            {"GET", "/api/nothing", "-", 401, 403, 403, 403, 403, 403},
    };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuditLog auditLog;

    static Stream<Arguments> cells() {
        List<Arguments> cells = new ArrayList<>();
        for (Object[] row : MATRIX) {
            for (int a = 0; a < ACTORS.size(); a++) {
                String actor = ACTORS.get(a);
                String credentials = actor.equals("-") ? "-" : actor + ":" + password(actor);
                cells.add(Arguments.of(row[3 + a], row[0], row[1], credentials, row[2]));
            }
            // Bad credentials are refused on every URL, the public one included.
            cells.add(Arguments.of(401, row[0], row[1], "nurse.patel:wrong", row[2]));
            cells.add(Arguments.of(401, row[0], row[1], "mallory:x", row[2]));
        }
        return cells.stream();
    }

    @ParameterizedTest(name = "{0} {1} {2} as {3}")
    @MethodSource("cells")
    void cell(int expected, String method, String path, String credentials, String body) throws Exception {
        assertEquals(expected, perform(method, path, credentials, body),
                method + " " + path + " as " + credentials.replaceAll(":.*", ""));
    }

    @Test
    void failedLoginIsAudited() throws Exception {
        int baseline = auditLog.entries().size();
        assertEquals(401, perform("GET", "/api/me", "mallory:x", "-"));

        List<AuditEntry> recorded = after(baseline);
        assertTrue(recorded.stream().anyMatch(e -> e.kind() == AuditKind.AUTHN_FAILURE
                        && "mallory".equals(e.username())
                        && "/api/me".equals(e.path())
                        && "BadCredentialsException".equals(e.detail())),
                "expected an AUTHN_FAILURE entry for mallory on /api/me, got " + recorded);
    }

    @Test
    void denialIsAuditedAfterTheSuccessfulAuthentication() throws Exception {
        int baseline = auditLog.entries().size();
        assertEquals(403, perform("GET", "/api/patients/1/record", "bob:" + password("bob"), "-"));

        List<AuditKind> kinds = after(baseline).stream()
                .filter(e -> "bob".equals(e.username()) && "/api/patients/1/record".equals(e.path()))
                .map(AuditEntry::kind)
                .toList();
        int success = kinds.indexOf(AuditKind.AUTHN_SUCCESS);
        int denied = kinds.indexOf(AuditKind.AUTHZ_DENIED);
        assertTrue(success >= 0 && denied > success, "expected AUTHN_SUCCESS then AUTHZ_DENIED for bob, got " + kinds);
    }

    private int perform(String method, String path, String credentials, String body) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path);
        if (!credentials.equals("-")) {
            int colon = credentials.indexOf(':');
            builder.with(httpBasic(credentials.substring(0, colon), credentials.substring(colon + 1)));
        }
        if (!body.equals("-")) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(builder).andReturn().getResponse().getStatus();
    }

    private List<AuditEntry> after(int baseline) {
        List<AuditEntry> all = auditLog.entries();
        return all.subList(baseline, all.size());
    }
}
