<!-- Owner: Mallikarjuna (slice 4); catalog paragraphs from Godson (slice 1), Kobe (slice 2), Chase (slice 3) -->
<!--
What belongs in this section:
- The class diagram uml/class/security-view.puml (+ .png, .utxt) of config, access, authn, audit and the filter chain.
- The element catalog: one entry per class in the shared contract, assembled from the paragraphs each slice sends
  (authn from slice 1; access from slice 2; clinic, web and audit from slice 3).
-->

# 5. Security view

<!-- TODO(slice 4: Mallikarjuna): write this section. -->

<!-- Access-package catalog paragraph from Kobe, for Mallikarjuna to incorporate. -->

**`access` package (Authorize Actors, Limit Access).** `AccessRules` holds the ordered service
table as immutable `AccessRule` records. Each row supplies its HTTP method, path pattern,
`Requirement` (`PermitAll`, `Authenticated`, or `AnyRole`), and own-record marker. `Role` names
the four actor types; the staff hierarchy is supplied by `config.SecurityConfig`. `AccessPolicy`
turns the table into URL authorization rules, permits ERROR dispatch first, and denies unlisted
requests last. `OwnRecordGuard`, named `ownRecord`, supplies the per-actor check called by the
two patient read methods' `@PreAuthorize` annotations: patients must match their principal's
patient ID, while staff already admitted by URL authorization may read any patient.
`ApiAccessDeniedHandler` writes a consistent JSON 403 response using `web.ApiError` for an
authenticated caller who fails either check. The configured Spring authorization event publisher
reports both kinds of denial for the audit listener.

<!-- Element catalog paragraph for the authn package, from Godson (slice 1). -->

**`authn` package (Identify Actors, Authenticate Actors).** These classes are the application's
side of HTTP Basic authentication; the filter, the authentication manager and the provider that call
them come from Spring Security. `DemoUser` is a record for one seeded user: username, `{bcrypt}`
password hash, role, and, for a patient, the patient id. `DemoUsers` holds the five seeded users;
plaintext passwords never appear in the application code. `DemoUserDetailsService` is the
`UserDetailsService` that Spring's `DaoAuthenticationProvider` consults on every request that
carries Basic credentials: it returns a new `ClinicUser` for a known name and throws only
`UsernameNotFoundException` for an unknown one, so that Spring can report an unknown user exactly
like a wrong password. `ClinicUser` extends Spring's `User` and is the authenticated principal;
besides the single `ROLE_*` authority it carries the role and patient id, which the own-record rule
reads during authorization without a second lookup. `ApiAuthenticationEntryPoint` is the
`AuthenticationEntryPoint` installed on both HTTP Basic and exception handling; it writes every 401
with the challenge `WWW-Authenticate: Basic realm="clinic"` and the JSON body
`{"status":401,"error":"Unauthorized","path":...}` (built by the shared `web.ApiError` record), and
never reveals why authentication failed.
