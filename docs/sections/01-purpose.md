# 1. Purpose and requirements

This document describes a prototype built to show two security tactics from Bass, Clements, and
Kazman, *Software Architecture in Practice*, 4th ed. (SAiP), Chapter 11: **Authenticate Actors**
and **Authorize Actors** (p. 174). Both belong to the "Resist Attacks" branch of the security
tactics tree. Authenticate Actors makes sure that a caller is who it claims to be. Authorize Actors
makes sure that an authenticated caller uses only the services it is entitled to.

The prototype is a small **clinic portal**: an HTTP API with nine services, such as reading a
patient's record, recording vitals, writing a prescription, and reading the audit log. Five demo
users of four types (ADMIN, DOCTOR, NURSE, PATIENT) call these services with HTTP Basic credentials.
A clinic was chosen because it needs both kinds of authorization the textbook names: per role
(a nurse may record vitals but not prescribe) and per actor (a patient may read their own record but
not another patient's). The data is fictional and held in memory.

Every request crosses one Spring Security filter chain. The chain first authenticates the caller
against bcrypt-hashed passwords, then authorizes the request against a fixed access table, and
denies anything the table does not list. Failed logins and denied requests are recorded in an audit
log (Audit, p. 176), so the two tactics leave evidence that can be inspected.

The security quality attribute is the subject of the whole document. Section 2 states it as two
concrete scenarios, Sections 3 to 6 show the architecture that meets them, Section 7 rates the
tactics we did not implement, Section 8 records the design decisions and their trade-offs, and
Section 9 shows the evidence that the scenarios' response measures are met.

## Requirements checklist

| # | Assignment requirement | Where it is satisfied |
| --- | --- | --- |
| R1 | Use JAAS or the Spring Framework for the security tactics | Spring Security 7 with Spring Boot 4; JAAS is not used. README "Libraries and frameworks"; decision D1 in Section 8 |
| R2 | More than one user | Five seeded users (`DemoUsers`). README "Demo users" |
| R3 | More than one user type | Four roles: ADMIN, DOCTOR, NURSE, PATIENT (`Role`). README "Demo users"; Section 4 |
| R4 | Different privileges for each user type | The access table and access matrix. Section 4; README "Services" and "Access matrix" |
| R5 | Services identified by URLs | Nine method-and-URL services in `AccessRules`. README "Services (URL table)"; Section 4 |
| R6 | Implement the Authenticate Actors tactic | HTTP Basic, `DemoUserDetailsService`, bcrypt. Section 3 "Identify Actors and Authenticate Actors"; Section 6 login diagrams |
| R7 | Implement the Authorize Actors tactic | URL rules generated from `AccessRules`, role hierarchy, `OwnRecordGuard`, default deny. Section 3 "Authorize Actors and Limit Access"; Section 6 request diagrams |
| R8 | Runnable source code and an executable | Maven project; `mvn package` builds `target/authentication-and-authorization.jar`. README "Build and run" |
| R9 | README with guidance to run the program | README "Prerequisites", "Build and run", "Demo" |
| R10 | README listing the libraries and frameworks used | README "Libraries and frameworks" |
| R11 | Design document on the security quality attribute, covering both tactics | This document: scenarios (Section 2), tactics (Section 3), privilege matrix (Section 4), security view (Section 5), sequence diagrams (Section 6), tactics questionnaire (Section 7), decisions (Section 8), verification (Section 9) |
