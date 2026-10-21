# 9. Verification

The response measures of Section 2 are checked in two independent ways: by the JUnit tests that
`mvn verify` runs on every build, and by `scripts/demo.sh`, which sends real HTTP requests to the
packaged jar. Both read the access matrix from the same file, `scripts/demo-matrix.txt`, and CI runs
both on every pull request.

## Test summary (`mvn verify`)

| Test class | Tests | What it covers |
| --- | --- | --- |
| `authn.AuthenticationTest` | 25 | Scenario S1: a missing credential, a wrong password and an unknown user all get 401 with the `Basic` challenge and the JSON body on every protected URL; an unknown user is byte-for-byte identical to a wrong password in status, headers, body and failure event; anonymous requests to unmapped URLs and to writes are 401, not 403; a malformed or foreign `Authorization` header is 401, not 500; each request re-authenticates with no session; success and failure events are published |
| `authn.ApiAuthenticationEntryPointTest` | 2 | The 401 entry point on its own: the status, the `WWW-Authenticate: Basic` challenge and JSON content type, and a response that never reveals the failure reason |
| `access.AuthorizationTest` | 14 | Scenario S2 denials: per-role URL denials with the 403 JSON body, default deny on an unmapped URL and on a wrong method even for admin, per-actor own-record denials, a patient blocked from recording vitals before method security runs, URL- versus method-level denial events, admin admitted through the role hierarchy, and the permitted error dispatch |
| `access.OwnRecordGuardTest` | 7 | The own-record guard as a unit: admin, doctor and nurse may read any patient; a patient only their own id; a non-`ClinicUser` principal and a patient with no id are refused; ids are compared by value |
| `access.ApiAccessDeniedHandlerTest` | 1 | The 403 handler: the status, the JSON body with the path escaped, the content length, no challenge header, and no leak of the denial reason |
| `audit.AuditTest` | 6 | The three kinds of event and their detail; bare denials recorded as `authenticated` and as `denyAll` (including a listed path under an unlisted method); a forbidden request through the real chain logs a success then a denial; a crafted username with control characters and line separators cannot forge an extra log line; the entries list is an unmodifiable snapshot |
| `web.ServicesTest` | 11 | Each clinic service with the filters off: list patients, read a record and appointments, 201 echoing the stored vitals and prescription, 404 for an unknown patient id, `/api/me` carrying the patient id for a patient, null for staff and a fallback for a non-clinic principal, the admin user list without hashes, and the audit as an ISO-8601 JSON array |
| `web.ApiErrorTest` | 3 | `ApiError.write`: the exact body with JSON content type and length, the path escaped with the fields in order, and a clean body even when a stale response was already buffered |
| `ContractSmokeTest` | 8 | The shared contract: the context loads with the policy as a plain component, the nine access rows in order, the five seeded users with bcrypt hashes matching their passwords, a fresh `ClinicUser` per lookup, an unknown user throwing `UsernameNotFoundException`, anonymous public info 200 through the real chain, the matrix file parsing to 80 rows, and the audit log keeping the newest 1000 entries oldest-first |
| `docs.DocsConsistencyTest` | 5 | The README's URL and user tables match the code, every section file exists with its heading, and no `TODO` marker remains anywhere in the README or the section files |
| `AccessMatrixTest` | 82 | Every one of the 80 rows of `scripts/demo-matrix.txt` through the real filter chain with real HTTP Basic credentials, plus two audit checks: a failed login is recorded as `AUTHN_FAILURE`, and a denial is recorded as `AUTHN_SUCCESS` then `AUTHZ_DENIED` |

Totals: 164 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`.

## Demo transcript (`scripts/demo.sh` against the packaged jar)

The jar was started with `java -jar target/authentication-and-authorization.jar --server.port=18731`
and the demo was run with `scripts/demo.sh http://localhost:18731`. All 80 rows matched:

```
demo: http://localhost:18731
EXPECTED ACTUAL METHOD PATH                              USER               RESULT
200      200    GET    /api/public/info                  -                  OK
200      200    GET    /api/public/info                  admin              OK
200      200    GET    /api/public/info                  dr.chen            OK
200      200    GET    /api/public/info                  nurse.patel        OK
200      200    GET    /api/public/info                  alice              OK
200      200    GET    /api/public/info                  bob                OK
401      401    GET    /api/me                           -                  OK
200      200    GET    /api/me                           admin              OK
200      200    GET    /api/me                           dr.chen            OK
200      200    GET    /api/me                           nurse.patel        OK
200      200    GET    /api/me                           alice              OK
200      200    GET    /api/me                           bob                OK
401      401    GET    /api/patients/1/record            -                  OK
200      200    GET    /api/patients/1/record            admin              OK
200      200    GET    /api/patients/1/record            dr.chen            OK
200      200    GET    /api/patients/1/record            nurse.patel        OK
200      200    GET    /api/patients/1/record            alice              OK
403      403    GET    /api/patients/1/record            bob                OK
401      401    GET    /api/patients/2/record            -                  OK
200      200    GET    /api/patients/2/record            admin              OK
200      200    GET    /api/patients/2/record            dr.chen            OK
200      200    GET    /api/patients/2/record            nurse.patel        OK
403      403    GET    /api/patients/2/record            alice              OK
200      200    GET    /api/patients/2/record            bob                OK
401      401    GET    /api/patients/1/appointments      -                  OK
200      200    GET    /api/patients/1/appointments      admin              OK
200      200    GET    /api/patients/1/appointments      dr.chen            OK
200      200    GET    /api/patients/1/appointments      nurse.patel        OK
200      200    GET    /api/patients/1/appointments      alice              OK
403      403    GET    /api/patients/1/appointments      bob                OK
401      401    GET    /api/patients/99/record           -                  OK
404      404    GET    /api/patients/99/record           admin              OK
404      404    GET    /api/patients/99/record           dr.chen            OK
404      404    GET    /api/patients/99/record           nurse.patel        OK
403      403    GET    /api/patients/99/record           alice              OK
403      403    GET    /api/patients/99/record           bob                OK
401      401    GET    /api/patients                     -                  OK
200      200    GET    /api/patients                     admin              OK
200      200    GET    /api/patients                     dr.chen            OK
200      200    GET    /api/patients                     nurse.patel        OK
403      403    GET    /api/patients                     alice              OK
403      403    GET    /api/patients                     bob                OK
401      401    POST   /api/patients/1/vitals            -                  OK
201      201    POST   /api/patients/1/vitals            admin              OK
201      201    POST   /api/patients/1/vitals            dr.chen            OK
201      201    POST   /api/patients/1/vitals            nurse.patel        OK
403      403    POST   /api/patients/1/vitals            alice              OK
403      403    POST   /api/patients/1/vitals            bob                OK
401      401    POST   /api/patients/1/prescriptions     -                  OK
201      201    POST   /api/patients/1/prescriptions     admin              OK
201      201    POST   /api/patients/1/prescriptions     dr.chen            OK
403      403    POST   /api/patients/1/prescriptions     nurse.patel        OK
403      403    POST   /api/patients/1/prescriptions     alice              OK
403      403    POST   /api/patients/1/prescriptions     bob                OK
401      401    GET    /api/admin/users                  -                  OK
200      200    GET    /api/admin/users                  admin              OK
403      403    GET    /api/admin/users                  dr.chen            OK
403      403    GET    /api/admin/users                  nurse.patel        OK
403      403    GET    /api/admin/users                  alice              OK
403      403    GET    /api/admin/users                  bob                OK
401      401    GET    /api/admin/audit                  -                  OK
200      200    GET    /api/admin/audit                  admin              OK
403      403    GET    /api/admin/audit                  dr.chen            OK
403      403    GET    /api/admin/audit                  nurse.patel        OK
403      403    GET    /api/admin/audit                  alice              OK
403      403    GET    /api/admin/audit                  bob                OK
401      401    GET    /api/nothing                      -                  OK
403      403    GET    /api/nothing                      admin              OK
403      403    GET    /api/nothing                      dr.chen            OK
403      403    GET    /api/nothing                      nurse.patel        OK
403      403    GET    /api/nothing                      alice              OK
403      403    GET    /api/nothing                      bob                OK
401      401    GET    /api/public/info                  nurse.patel:wrong  OK
401      401    GET    /api/me                           nurse.patel:wrong  OK
401      401    GET    /api/patients/1/record            nurse.patel:wrong  OK
401      401    GET    /api/admin/audit                  nurse.patel:wrong  OK
401      401    GET    /api/public/info                  mallory:x          OK
401      401    GET    /api/me                           mallory:x          OK
401      401    GET    /api/patients/1/record            mallory:x          OK
401      401    GET    /api/admin/audit                  mallory:x          OK

demo: 80 rows checked, 0 mismatch(es)
```

These results meet the response measures of Section 2:

- **S1:** every request with missing or bad credentials (the 11 anonymous rows to protected URLs and the 8 bad-credential
  rows) got 401, and none returned protected data.
- **S2:** each of the 72 cells of the access matrix returned the expected status, so there were 0
  unauthorized grants. Both patients got 403, never 404, for ids other than their own.

## Audit log excerpt

The demo prints the audit log as admin when it finishes. Representative entries from this run, one
of each kind of event:

```json
{"at":"2026-10-20T23:59:14.512126Z","kind":"AUTHZ_DENIED","username":"anonymous","method":"GET","path":"/api/me","detail":"authenticated"}
{"at":"2026-10-20T23:59:15.337073Z","kind":"AUTHN_SUCCESS","username":"bob","method":"GET","path":"/api/patients/1/record","detail":"-"}
{"at":"2026-10-20T23:59:15.338095Z","kind":"AUTHZ_DENIED","username":"bob","method":"GET","path":"/api/patients/1/record","detail":"record"}
{"at":"2026-10-20T23:59:17.609182Z","kind":"AUTHZ_DENIED","username":"nurse.patel","method":"POST","path":"/api/patients/1/prescriptions","detail":"ROLE_DOCTOR"}
{"at":"2026-10-20T23:59:18.579959Z","kind":"AUTHZ_DENIED","username":"anonymous","method":"GET","path":"/api/nothing","detail":"denyAll"}
{"at":"2026-10-20T23:59:19.356676Z","kind":"AUTHN_FAILURE","username":"mallory","method":"GET","path":"/api/public/info","detail":"BadCredentialsException"}
```

The detail field shows which check refused a request. `record` is the own-record check on
`PatientController.record`, a per-actor denial. `ROLE_DOCTOR` is the URL rule's required role, a
per-role denial. `denyAll` is default deny, and `authenticated` is an anonymous caller refused by
the "any logged-in user" rule. Every authenticated request leaves an `AUTHN_SUCCESS` entry, because
under stateless HTTP Basic each request is a new authentication.
