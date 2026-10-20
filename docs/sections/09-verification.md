<!-- Owner: Mallikarjuna (slice 4), filled at integration -->
<!--
What belongs in this section:
- The test summary from mvn verify (test classes, counts, what each covers).
- The scripts/demo.sh transcript against the packaged jar (every matrix row OK) and the audit log it prints.
-->

# 9. Verification

<!-- TODO(integration): the figures below come from a trial merge of slices 2 and 3 onto main (2026-10-16). Re-run
     mvn verify and scripts/demo.sh on main after the integration pull request, update the counts and transcript,
     and remove this marker. -->

The response measures of Section 2 are checked in two independent ways: by the JUnit tests that
`mvn verify` runs on every build, and by `scripts/demo.sh`, which sends real HTTP requests to the
packaged jar. Both read the access matrix from the same file, `scripts/demo-matrix.txt`, and CI runs
both on every pull request.

## Test summary (`mvn verify`)

| Test class | Tests | What it covers |
| --- | --- | --- |
| `authn.AuthenticationTest` | 25 | Scenario S1: 401 with the challenge for no credentials, a wrong password and an unknown user on every URL; byte-identical responses for `nurse.patel:wrong` and `mallory:x`; success and failure events published |
| `authn.ApiAuthenticationEntryPointTest` | 2 | The 401 status, `WWW-Authenticate` header and JSON body |
| `access.AuthorizationTest` | 14 | Scenario S2 per role: hierarchy, PATIENT outside it, default deny, 401 before 403 for anonymous callers, denial events |
| `access.OwnRecordGuardTest` | 7 | Scenario S2 per actor: own id allowed, other id refused, staff allowed, missing patient id and foreign principal refused |
| `access.ApiAccessDeniedHandlerTest` | 1 | The 403 status and JSON body |
| `audit.AuditTest` | 4 | `AUTHN_SUCCESS`, `AUTHN_FAILURE` and `AUTHZ_DENIED` entries and their details; a forbidden request through the real chain logs a success then a denial; unmodifiable snapshots |
| `web.ServicesTest` | 11 | Each service's response body, 201 for writes, 404 for an unknown patient id for staff |
| `web.ApiErrorTest` | 3 | Error body format, property order and JSON escaping |
| `ContractSmokeTest` | 8 | The shared contract: the nine-row access table, the five seeded users and their hashes, a fresh `ClinicUser` per lookup, the matrix file parses, the audit log keeps the newest 1000 entries |
| `docs.DocsConsistencyTest` | 5 | The README URL and user tables match the code; every section exists with its heading; no TODO marker remains |
| `AccessMatrixTest` | 3 (skipped) | Every one of the 80 rows of `scripts/demo-matrix.txt` against the application, plus that failed logins and denials are audited. Still `@Disabled` in the trial run; it is enabled by the last slice to merge |

Trial run: 83 tests, 0 failures, 0 errors, 8 skipped (`AccessMatrixTest`, and `DocsConsistencyTest`, which this slice enables), `BUILD SUCCESS`.

## Demo transcript (`scripts/demo.sh` against the packaged jar)

The jar was started with `java -jar target/authentication-and-authorization.jar --server.port=18731`
and the demo was run with `scripts/demo.sh http://localhost:18731`. All 80 rows matched:

```
demo: http://localhost:18731
EXPECTED ACTUAL METHOD PATH                              USER               RESULT
200      200    GET    /api/public/info                  -                  OK
401      401    GET    /api/me                           -                  OK
200      200    GET    /api/me                           nurse.patel        OK
200      200    GET    /api/patients/1/record            alice              OK
403      403    GET    /api/patients/1/record            bob                OK
403      403    GET    /api/patients/2/record            alice              OK
404      404    GET    /api/patients/99/record           dr.chen            OK
403      403    GET    /api/patients/99/record           alice              OK
201      201    POST   /api/patients/1/vitals            nurse.patel        OK
403      403    POST   /api/patients/1/prescriptions     nurse.patel        OK
201      201    POST   /api/patients/1/prescriptions     dr.chen            OK
403      403    GET    /api/admin/audit                  dr.chen            OK
200      200    GET    /api/admin/audit                  admin              OK
403      403    GET    /api/nothing                      admin              OK
401      401    GET    /api/public/info                  nurse.patel:wrong  OK
401      401    GET    /api/public/info                  mallory:x          OK
...                                                   (80 rows in total, every one OK)

demo: 80 rows checked, 0 mismatch(es)
```

These results meet the response measures of Section 2:

- **S1:** every request with missing or bad credentials (the 11 anonymous rows to protected URLs and the 8 bad-credential
  rows) got 401, and none returned protected data.
- **S2:** each of the 72 cells of the access matrix returned the expected status, so there were 0
  unauthorized grants. Both patients got 403, never 404, for ids other than their own.

## Audit log excerpt

The demo prints the audit log as admin when it finishes. Representative entries from the trial run,
one of each kind of event:

```json
{"at":"2026-10-16T18:14:43.233423Z","kind":"AUTHZ_DENIED","username":"anonymous","method":"GET","path":"/api/me","detail":"authenticated"}
{"at":"2026-10-16T18:14:43.878916Z","kind":"AUTHN_SUCCESS","username":"bob","method":"GET","path":"/api/patients/1/record","detail":"-"}
{"at":"2026-10-16T18:14:43.879830Z","kind":"AUTHZ_DENIED","username":"bob","method":"GET","path":"/api/patients/1/record","detail":"record"}
{"at":"2026-10-16T18:14:45.694282Z","kind":"AUTHZ_DENIED","username":"nurse.patel","method":"POST","path":"/api/patients/1/prescriptions","detail":"ROLE_DOCTOR"}
{"at":"2026-10-16T18:14:46.509379Z","kind":"AUTHZ_DENIED","username":"admin","method":"GET","path":"/api/nothing","detail":"denyAll"}
{"at":"2026-10-16T18:14:47.081394Z","kind":"AUTHN_FAILURE","username":"mallory","method":"GET","path":"/api/public/info","detail":"BadCredentialsException"}
```

The detail field shows which check refused a request. `record` is the own-record check on
`PatientController.record`, a per-actor denial. `ROLE_DOCTOR` is the URL rule's required role, a
per-role denial. `denyAll` is default deny, and `authenticated` is an anonymous caller refused by
the "any logged-in user" rule. Every authenticated request leaves an `AUTHN_SUCCESS` entry, because
under stateless HTTP Basic each request is a new authentication.
