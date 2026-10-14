package edu.rit.swen755.auth.web;

import edu.rit.swen755.auth.access.OwnRecordGuard;
import edu.rit.swen755.auth.audit.AuditEntry;
import edu.rit.swen755.auth.audit.AuditKind;
import edu.rit.swen755.auth.audit.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for the clinic services (slice 3).
 *
 * <p>The security filters are switched off and the own-record check is stubbed to allow, so these
 * tests exercise only the controllers and the repository and are green with slice 3 and Step 0
 * alone. {@code @WithUserDetails} installs a real {@code ClinicUser} principal from the user store.
 */
@Disabled("TODO(slice 3: Chase) — enable when implementing")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ServicesTest {

    private static final String VITALS = "{\"systolic\":120,\"diastolic\":80,\"pulse\":72}";
    private static final String PRESCRIPTION = "{\"drug\":\"amoxicillin\",\"dose\":\"500mg\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuditLog auditLog;

    @MockitoBean
    private OwnRecordGuard ownRecord;

    @BeforeEach
    void allowOwnRecord() {
        given(ownRecord.allows(any(), anyLong())).willReturn(true);
    }

    @Test
    @WithUserDetails("nurse.patel")
    void listsTheThreePatients() throws Exception {
        mvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(1, 2, 3)));
    }

    @Test
    @WithUserDetails("nurse.patel")
    void readsARecord() throws Exception {
        mvc.perform(get("/api/patients/1/record"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(1))
                .andExpect(jsonPath("$.notes.length()", greaterThan(0)));
    }

    @Test
    @WithUserDetails("nurse.patel")
    void readsAppointments() throws Exception {
        mvc.perform(get("/api/patients/1/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[0].patientId").value(1));
    }

    @Test
    @WithUserDetails("nurse.patel")
    void recordingVitalsAnswers201WithTheStoredVitals() throws Exception {
        mvc.perform(post("/api/patients/1/vitals").contentType(MediaType.APPLICATION_JSON).content(VITALS))
                .andExpect(status().isCreated())
                .andExpect(content().json(VITALS, JsonCompareMode.STRICT));
    }

    @Test
    @WithUserDetails("dr.chen")
    void prescribingAnswers201WithTheStoredPrescription() throws Exception {
        mvc.perform(post("/api/patients/1/prescriptions").contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isCreated())
                .andExpect(content().json(PRESCRIPTION, JsonCompareMode.STRICT));
    }

    @Test
    @WithUserDetails("nurse.patel")
    void unknownPatientIs404() throws Exception {
        mvc.perform(get("/api/patients/99/record")).andExpect(status().isNotFound());
        mvc.perform(get("/api/patients/99/appointments")).andExpect(status().isNotFound());
        mvc.perform(post("/api/patients/99/vitals").contentType(MediaType.APPLICATION_JSON).content(VITALS))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("alice")
    void meForAPatientCarriesTheirPatientId() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.patientId").value(1));
    }

    @Test
    @WithUserDetails("admin")
    void meForStaffHasANullPatientId() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.patientId").value(nullValue()));
    }

    @Test
    @WithUserDetails("admin")
    void usersListsTheFiveUsersWithoutHashes() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("admin", "dr.chen", "nurse.patel", "alice", "bob")))
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("bcrypt"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    @WithUserDetails("admin")
    void auditIsAJsonArrayWithIso8601Times() throws Exception {
        auditLog.record(new AuditEntry(Instant.parse("2026-10-14T12:00:00Z"), AuditKind.AUTHN_FAILURE,
                "mallory", "GET", "/api/me", "BadCredentialsException"));

        mvc.perform(get("/api/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[-1].at").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")))
                .andExpect(jsonPath("$[-1].kind").value("AUTHN_FAILURE"))
                .andExpect(jsonPath("$[-1].username").value("mallory"));
    }
}
