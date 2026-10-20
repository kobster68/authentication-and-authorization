<!-- Owner: Mallikarjuna (slice 4) -->
<!--
What belongs in this section:
- A table in the style of Table 22.4 with the columns Design Decisions and Location | Rationale and Assumptions
  (Include Discarded Alternatives), with a discarded alternative in every row:
  Spring Security over JAAS (Security Manager removed by JEP 486; no URL model); HTTP Basic over form login;
  stateless with CSRF off (valid only because there is no browser client) over sessions with CSRF;
  role hierarchy over a flat hasAnyRole on every rule; bcrypt over {noop}; a custom UserDetailsService returning ClinicUser
  over InMemoryUserDetailsManager.
- Trade-offs: bcrypt runs on every request (about 70 ms) under stateless Basic; the intermediary cost of the filter chain (p. 179).
- The Basic-without-TLS limitation, stated plainly.
-->

# 8. Design decisions

The table uses the layout of SAiP Table 22.4. Each row names the decision and where it lives in the
code, then gives the rationale, the assumptions it depends on, and the alternative we discarded.

| # | Design Decisions and Location | Rationale and Assumptions (Include Discarded Alternatives) |
| --- | --- | --- |
| D1 | **Spring Security (with Spring Boot) implements both tactics.** `pom.xml`; `SecurityConfig` | The assignment allows JAAS or Spring. Spring Security gives a filter chain in front of every HTTP request, URL-based authorization rules, method security, password encoders and security events, which match the tactics directly. *Discarded: JAAS.* JAAS provides pluggable login modules but has no model of HTTP URLs, so the URL-to-role rules would have to be written by hand. Its authorization side relied on the Java Security Manager, which JEP 486 permanently disabled in JDK 24. |
| D2 | **HTTP Basic authentication on every request.** `SecurityConfig.api` (`httpBasic`); `ApiAuthenticationEntryPoint` | The clients are curl, the demo script and the tests, none of them browsers. Basic puts the whole authentication exchange in one header, so each request shows Identify Actors and Authenticate Actors end to end, and 401 versus 403 is easy to show. Assumes a trusted network (see the limitation below). *Discarded: form login,* which needs a login page, a session cookie and CSRF tokens. These suit a browser, add steps that are not part of either tactic, and would be unused by our clients. |
| D3 | **Stateless sessions with CSRF protection off.** `SecurityConfig.api` (`SessionCreationPolicy.STATELESS`, `csrf.disable()`) | No session is created, so the server holds no login state and every request is authenticated again. CSRF exploits credentials a browser sends automatically (cookies). Our clients send credentials only when told to, so turning CSRF off is safe **only because there is no browser client**. A browser front end would make this decision wrong. *Discarded: server sessions with CSRF protection,* which would avoid repeating bcrypt on each request but add session fixation, timeout and CSRF concerns that the API does not otherwise have. |
| D4 | **A role hierarchy ADMIN > DOCTOR > NURSE, with PATIENT outside it.** `SecurityConfig.roleHierarchy`; rules in `AccessRules` name only the lowest staff role | Each rule names its minimum role ("NURSE+"), and the hierarchy adds the roles above it. Method security honours the same bean. PATIENT is kept out so that a patient is never treated as staff. *Discarded: a flat `hasAnyRole("NURSE","DOCTOR","ADMIN")` on every rule,* which repeats the ordering in every rule. Adding a role or changing the order would then mean editing every rule, and one missed rule would silently give wrong access. |
| D5 | **bcrypt password hashes through the delegating encoder.** `DemoUsers` (`{bcrypt}` hashes); `SecurityConfig.passwordEncoder` | Only hashes are stored. BCrypt is salted and deliberately slow (cost 10), which slows offline guessing if the store leaks. The `{id}` prefix lets a later change of algorithm coexist with old hashes. *Discarded: `{noop}` plaintext passwords,* which are simpler but store the secret itself and show nothing about credential storage. |
| D6 | **A custom `UserDetailsService` that returns a `ClinicUser` principal.** `DemoUserDetailsService`, `ClinicUser` | The own-record rule needs the caller's patient id. Putting it on the principal lets `OwnRecordGuard` read it with no second lookup. The service builds a new principal on every call because Spring erases the password on the returned object. *Discarded: Spring's `InMemoryUserDetailsManager`,* which returns plain `User` objects that have only a name and authorities, so the patient id would need a second, separate mapping. |
| D7 | **The access table is data, and the URL rules are generated from it, ending in default deny.** `AccessRules`, `AccessPolicy` | One source of truth: `AccessPolicy` builds the Spring rules from it, the tests assert against it, and `DocsConsistencyTest` checks the README against it, so the policy cannot drift from its documentation. The last rule, `anyRequest().denyAll()`, is Limit Access. *Discarded: hand-written `requestMatchers` chains in the configuration,* which are just as correct on the day they are written but give the tests and documentation nothing to check against. |
| D8 | **The per-actor check runs in method security, before the controller body.** `@PreAuthorize("@ownRecord.allows(authentication, #id)")` on `PatientController`; `OwnRecordGuard` | The check runs before the repository is read, so a patient gets 403 for any id other than their own, existing or not, and cannot learn which ids exist. *Discarded: checking inside the controller after loading the record,* which would answer 404 for a missing id and 403 for an existing one, and so leak which ids exist. It would also spread security checks into domain code. |
| D9 | **Audit through Spring Security events.** `SecurityConfig.authorizationEventPublisher`; `SecurityEventListener`; `InMemoryAuditLog` | The listener records every authentication success and failure and every authorization denial, without any controller or filter of ours calling the log. The log is in memory and bounded to the newest 1000 entries. *Discarded: calling the audit log from each controller,* which misses requests that never reach a controller, and those are exactly the rejected ones. |

## Trade-offs

**bcrypt on every request.** Under stateless HTTP Basic, the expensive part of authentication runs
on every authenticated request, not once per login. In our demo run, authenticated requests took
about 60 to 80 ms each, almost all of it BCrypt, against a few milliseconds for an anonymous call to
the public URL. That is a deliberate trade of performance for security and simplicity: no session
state, no session attacks, and a slow hash that also slows password guessing. A system with heavy
traffic would exchange the password once for a short-lived token or session, which brings back the
state that D3 avoids.

**The intermediary cost of the filter chain.** SAiP notes that an intermediary adds processing to
every message that passes through it (p. 179). Every request, including the public one, crosses the
whole chain. Apart from bcrypt, the filters cost little. In return, all security decisions are made
in one place that can be read, tested and audited, and a controller cannot be reached around it.

**Hierarchy and generation add indirection.** A reader has to look in three places (`AccessRules`,
`AccessPolicy` and the hierarchy bean) to see why a request was admitted. The `AccessRules` javadoc,
Section 4 and the README all show the resulting table, so this indirection does not hide the policy.

## Limitation: HTTP Basic without TLS

HTTP Basic sends the username and password with every request, encoded in Base64, which anyone who
sees the traffic can decode. The prototype serves plain HTTP, so on an untrusted network every
request would expose the caller's password. This is the largest risk in the design (Section 7,
Encrypt Data, risk High). The design is acceptable only under the trusted-network assumption of
Section 2. Before any real deployment, the server would have to be put behind TLS. Ideally the
password would also be exchanged for an expiring token, so that it crosses the network once rather
than on every request.
