package edu.rit.swen755.auth;

import edu.rit.swen755.auth.TestFixtures.AuditBaseline;
import edu.rit.swen755.auth.TestFixtures.MatrixRow;
import edu.rit.swen755.auth.audit.AuditEntry;
import edu.rit.swen755.auth.audit.AuditKind;
import edu.rit.swen755.auth.audit.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static edu.rit.swen755.auth.TestFixtures.after;
import static edu.rit.swen755.auth.TestFixtures.baseline;
import static edu.rit.swen755.auth.TestFixtures.password;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Every cell of the access matrix, through the real filter chain with real HTTP Basic
 * credentials, plus the audit entries for a failed login and a denial.
 *
 * <p>The cells are read from {@code scripts/demo-matrix.txt}, every row including the
 * commented-out ones, so the demo script and this test check the same matrix. This is the
 * cross-cutting evidence: it needs authentication, authorization and the services together.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessMatrixTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuditLog auditLog;

    static List<MatrixRow> cells() {
        return TestFixtures.demoMatrix();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cells")
    void cell(MatrixRow row) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(row.method()), row.path());
        if (row.username() != null) {
            builder.with(httpBasic(row.username(), row.password()));
        }
        if (row.body() != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(row.body());
        }
        int actual = mvc.perform(builder).andReturn().getResponse().getStatus();
        assertEquals(row.expected(), actual, row.method() + " " + row.path() + " as " + row.who()
                + " (" + TestFixtures.DEMO_MATRIX + ":" + row.line() + ")");
    }

    @Test
    void failedLoginIsAudited() throws Exception {
        AuditBaseline before = baseline(auditLog);
        assertEquals(401, mvc.perform(request(HttpMethod.GET, "/api/me").with(httpBasic("mallory", "x")))
                .andReturn().getResponse().getStatus());

        List<AuditEntry> recorded = after(auditLog, before);
        assertTrue(recorded.stream().anyMatch(e -> e.kind() == AuditKind.AUTHN_FAILURE
                        && "mallory".equals(e.username())
                        && "/api/me".equals(e.path())
                        && "BadCredentialsException".equals(e.detail())),
                "expected an AUTHN_FAILURE entry for mallory on /api/me, got " + recorded);
    }

    @Test
    void denialIsAuditedAfterTheSuccessfulAuthentication() throws Exception {
        AuditBaseline before = baseline(auditLog);
        assertEquals(403, mvc.perform(request(HttpMethod.GET, "/api/patients/1/record")
                        .with(httpBasic("bob", password("bob"))))
                .andReturn().getResponse().getStatus());

        List<AuditKind> kinds = after(auditLog, before).stream()
                .filter(e -> "bob".equals(e.username()) && "/api/patients/1/record".equals(e.path()))
                .map(AuditEntry::kind)
                .toList();
        int success = kinds.indexOf(AuditKind.AUTHN_SUCCESS);
        int denied = kinds.indexOf(AuditKind.AUTHZ_DENIED);
        assertTrue(success >= 0 && denied > success, "expected AUTHN_SUCCESS then AUTHZ_DENIED for bob, got " + kinds);
    }
}
