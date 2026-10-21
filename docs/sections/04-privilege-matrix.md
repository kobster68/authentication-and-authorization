# 4. Privilege matrix

## Access table

The portal offers nine services, each identified by an HTTP method and a URL pattern. The table is
kept as data in `AccessRules`, and `AccessPolicy` generates the Spring Security URL rules from it in
this order. "NURSE+" means NURSE or any role above it in the hierarchy ADMIN > DOCTOR > NURSE.
PATIENT is outside the hierarchy. A "yes" in the last column means the per-actor own-record rule
also applies: a PATIENT may call the service only for their own patient id.

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

The expected status for each demo user against each service. Patient ids: 1 is alice, 2 is bob,
99 does not exist. "anon" is a request without credentials. The source of this matrix is
`scripts/demo-matrix.txt`, which both `scripts/demo.sh` and `AccessMatrixTest` read, so the
documented, tested and demonstrated matrices are the same file.

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

Bad credentials are refused before any rule is consulted: a wrong password (`nurse.patel:wrong`)
or an unknown user (`mallory:x`) gets 401 on every URL, including the public one.

The status codes mean:

- **401**: not authenticated (no credentials, wrong password, or unknown user).
- **403**: authenticated, but this user may not call this service.
- **404**: a staff member asked for a patient id that does not exist.
- **200 / 201**: success (read / write).

## Per-role and per-actor access control

SAiP describes Authorize Actors as controlling access by the actor's identity, either per actor or
per class of actor, such as a role (p. 174). The matrix shows both.

**Per role.** Most cells are decided by the caller's role alone, at the URL rule. The
`POST /api/patients/1/prescriptions` row is the clearest: dr.chen gets 201, nurse.patel gets 403,
because DOCTOR+ admits DOCTOR and ADMIN (through the hierarchy) but not NURSE. The
`GET /api/admin/*` rows show the same thing at the top of the hierarchy, and the
`GET /api/patients` and `POST .../vitals` rows show that a PATIENT is never treated as staff. The
`GET /api/nothing` row shows Limit Access: the URL is in no rule, so even admin gets 403.

**Per actor.** In the `GET /api/patients/{id}/record` and `.../appointments` rows, alice and bob
hold the same role, yet their cells differ. The URL rule admits both as PATIENT; `OwnRecordGuard`
then compares the requested id with the caller's own patient id. So alice gets 200 for patient 1
and 403 for patient 2, and bob the reverse. The `GET /api/patients/99/record` row shows why the
per-actor check runs before the data is read: staff get 404 because the id does not exist, but both
patients get 403, so a patient cannot use the difference between 403 and 404 to discover which
patient ids exist.
