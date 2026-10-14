# authentication-and-authorization

SWEN-755 group assignment: an implementation of the Authenticate Actors and Authorize Actors
security tactics in Java, with Spring Security.

**Repository:** https://github.com/kobster68/authentication-and-authorization

## Summary

The program is a small **clinic portal**: an HTTP API whose services are identified by method and
URL (reading a patient's record, recording vitals, writing a prescription, listing users, reading
the audit log). Five demo users of four types (ADMIN, DOCTOR, NURSE, PATIENT) log in with HTTP
Basic. Every request passes through one Spring Security filter chain, which first **authenticates**
the caller against bcrypt-hashed passwords and then **authorizes** the request against a fixed
access table: per role (with the hierarchy ADMIN > DOCTOR > NURSE) and, for patients, per actor
(a patient may read only their own record). Anything not in the table is denied to everyone.
Failed logins and denied requests are recorded in an audit log that an administrator can read.
The tactics are described in *Software Architecture in Practice*, 4th ed. (Bass, Clements, and
Kazman), Chapter 11, p. 174.

<!-- TODO(slice 4: Mallikarjuna): expand the summary (domain, the two tactics, where each lives). -->

## Prerequisites

- JDK 21 (the build targets Java 21).
- Maven 3.9 or newer.
- `curl` and a POSIX shell (bash) for the scripts.

## Build and run

```bash
scripts/run.sh                       # builds the jar and starts the server on port 8080
scripts/run.sh --server.port=9090    # any Spring Boot argument passes through
```

Without the script:

```bash
mvn -q -DskipTests package
java -jar target/authentication-and-authorization.jar --server.port=8080
```

`mvn package` produces the executable jar `target/authentication-and-authorization.jar`. To build
and run the tests:

```bash
mvn verify
```

Check that the server is up:

```bash
curl -i http://localhost:8080/api/public/info
curl -i -u nurse.patel:patel-pass http://localhost:8080/api/me
```

## Demo

With the server running, `scripts/demo.sh` sends one request per row of
`scripts/demo-matrix.txt` and prints `EXPECTED ACTUAL METHOD PATH USER OK|MISMATCH`, then the
audit log as admin. It exits 1 on any mismatch and 2 if the server is not reachable.

```bash
scripts/demo.sh                          # against http://localhost:8080
scripts/demo.sh http://localhost:9090    # another port
```

<!-- TODO(slice 4: Mallikarjuna): add a short sample of the demo output. -->

## Demo users

| Username | Password | Type (role) | Patient id |
| --- | --- | --- | --- |
| `admin` | `admin-pass` | ADMIN | – |
| `dr.chen` | `chen-pass` | DOCTOR | – |
| `nurse.patel` | `patel-pass` | NURSE | – |
| `alice` | `alice-pass` | PATIENT | 1 |
| `bob` | `bob-pass` | PATIENT | 2 |

These credentials are for evaluation only. The code stores only `{bcrypt}` hashes of the
passwords (`DemoUsers`); the plaintext is published here on purpose so that each user type can be
tried, so the committed hashes protect nothing secret. They show how passwords are stored, not a
secret store. A real deployment would keep users outside the code and force a password change
(Change Credential Settings, SAiP 4th ed., p. 175).

## Services (URL table)

<!-- Keep every row in the form `| <#> | <METHOD> | `<pattern>` | ... |`, with the pattern exactly as in
     AccessRules: DocsConsistencyTest looks for `| <METHOD> | `<pattern>` |` for each service. -->

"NURSE+" means NURSE or any role above it in the hierarchy (DOCTOR, ADMIN). The own-record rule
means a PATIENT may call the service only for their own patient id.

| # | Method | URL | Who may call it | Own-record rule |
| --- | --- | --- | --- | --- |
| 1 | GET | `/api/public/info` | anyone, no login needed | |
| 2 | GET | `/api/me` | any logged-in user | |
| 3 | GET | `/api/patients/{id}/record` | PATIENT or NURSE+ | yes |
| 4 | GET | `/api/patients/{id}/appointments` | PATIENT or NURSE+ | yes |
| 5 | GET | `/api/patients` | NURSE+ | |
| 6 | POST | `/api/patients/{id}/vitals` | NURSE+ | |
| 7 | POST | `/api/patients/{id}/prescriptions` | DOCTOR+ | |
| 8 | GET | `/api/admin/users` | ADMIN | |
| 9 | GET | `/api/admin/audit` | ADMIN | |
| – | any | anything else | nobody (default deny) | |

## Access matrix

Expected status for every user against every service. Patient ids: 1 is alice, 2 is bob, 99 does
not exist. "anon" is a request without credentials.

The source of this matrix is `scripts/demo-matrix.txt`: `scripts/demo.sh` and `AccessMatrixTest`
both read it. This table must mirror that file; change the file first.

| Service | anon | admin | dr.chen | nurse.patel | alice (P1) | bob (P2) |
| --- | --- | --- | --- | --- | --- | --- |
| GET /api/public/info | 200 | 200 | 200 | 200 | 200 | 200 |
| GET /api/me | 401 | 200 | 200 | 200 | 200 | 200 |
| GET /api/patients/1/record | 401 | 200 | 200 | 200 | 200 | 403 |
| GET /api/patients/2/record | 401 | 200 | 200 | 200 | 403 | 200 |
| GET /api/patients/1/appointments | 401 | 200 | 200 | 200 | 200 | 403 |
| GET /api/patients/99/record | 401 | 404 | 404 | 404 | 403 | 403 |
| GET /api/patients | 401 | 200 | 200 | 200 | 403 | 403 |
| POST /api/patients/1/vitals | 401 | 201 | 201 | 201 | 403 | 403 |
| POST /api/patients/1/prescriptions | 401 | 201 | 201 | 403 | 403 | 403 |
| GET /api/admin/users | 401 | 200 | 403 | 403 | 403 | 403 |
| GET /api/admin/audit | 401 | 200 | 403 | 403 | 403 | 403 |
| GET /api/nothing (unmapped) | 401 | 403 | 403 | 403 | 403 | 403 |

A wrong password (`nurse.patel:wrong`) or an unknown user (`mallory:x`) gets 401 on every URL,
the public one included.

## What the status codes mean

- **401 Unauthorized**: not authenticated. No credentials, a wrong password, or an unknown user.
  The response carries `WWW-Authenticate: Basic realm="clinic"` and a JSON body. A wrong password
  and an unknown user get identical responses, so usernames cannot be discovered.
- **403 Forbidden**: authenticated, but this user may not call this service (wrong role, another
  patient's data, or a URL that is not in the table).
- **404 Not Found**: a staff member asked for a patient id that does not exist. A patient asking
  for another id gets 403 first, so a patient never learns whether an id exists.
- **200 / 201**: success (reads / writes).

Every authenticated request is recorded in the audit log (HTTP Basic sends credentials on each
request, so there is no separate "login"), together with every failed authentication and every
denial. The log is held in memory and keeps the newest 1000 entries.

<!-- TODO(slice 4: Mallikarjuna): refine the audit explanation and add an example entry. -->

## Libraries and frameworks

The assignment allows JAAS or the Spring Framework. This project uses **Spring Security** (with
Spring Boot); JAAS is not used anywhere.

<!-- TODO(slice 4: Mallikarjuna): say why Spring Security was chosen over JAAS. -->

| Component | Version | Use |
| --- | --- | --- |
| Java / JDK | 21 | Language and runtime |
| Spring Boot | 4.1.1 | Application framework, embedded server, executable jar |
| Spring Security | 7.1.1 | Authentication (HTTP Basic, bcrypt) and authorization (URL rules, method security) |
| Spring Framework (Spring MVC) | 7.0.9 | The REST controllers |
| Apache Tomcat (embedded) | 11.0.24 | HTTP server inside the jar |
| Jackson | 3.1.5 | JSON |
| JUnit Jupiter | 6.0.3 | Tests (test scope) |
| Spring Security Test, Mockito | 7.1.1, 5.23.0 | Test support (test scope) |
| Maven | 3.9+ | Build |
| spring-boot-maven-plugin / Surefire / Compiler / Jar | 4.1.1 / 3.5.6 / 3.15.0 / 3.5.1 | Executable jar, test run, compilation, packaging |
| PlantUML | – | UML diagrams under `uml/` |
| pandoc | – | `scripts/export-pdf.sh`, the design document PDF |

All library versions are managed by the Spring Boot parent POM.

## Design document

The design document is written one section per file under `docs/sections/` and exported to
`docs/design-document.pdf` with `scripts/export-pdf.sh` (needs pandoc).

<!-- TODO(slice 4: Mallikarjuna): link the PDF and list the sections. -->

## Team and slice ownership

The work is split into four slices built in parallel against a shared contract of compiling stubs.

| Slice | Area | Owner |
| --- | --- | --- |
| 0 | Scaffold, shared contract, user store, CI | Godson Umoren |
| 1 | Authentication: 401 entry point, authentication events, login diagrams | Godson Umoren |
| 2 | Authorization: URL rules, default deny, own-record check, 403 handler | Kobe LaPrade |
| 3 | Clinic services, audit log, demo script | Chase Michael |
| 4 | Design document and README | Mallikarjuna |

## Changing the shared contract

The public types and signatures in `config`, `access`, `authn`, `audit`, `clinic` and `web`, the
access table in `AccessRules`, and the seeded users in `DemoUsers` are a frozen contract. Any change
to them goes through its own small pull request, reviewed by Godson, so the seam between slices
does not drift while four people are working at once.
