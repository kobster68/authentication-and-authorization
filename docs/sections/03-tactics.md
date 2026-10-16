<!-- Owner: Mallikarjuna (slice 4) for the framing; tactic paragraphs by Godson (slice 1), Kobe (slice 2), Chase (slice 3) -->
<!--
What belongs in this section:
- Framing (slice 4): the filter chain as the textbook's Intercepting Filter (p. 194) and its intermediary cost (p. 179).
- Identify Actors and Authenticate Actors (slice 1): what each tactic is (p. 174) and where it lives:
  BasicAuthenticationFilter -> DaoAuthenticationProvider -> DemoUserDetailsService, bcrypt through the PasswordEncoder.
- Authorize Actors per role and per actor, and Limit Access by default deny (slice 2): AuthorizationFilter over AccessPolicy
  with the role hierarchy; OwnRecordGuard through method security; why the rules are generated from AccessRules.
- Audit (slice 3, p. 176): SecurityEventListener, what is recorded and where, and that each authenticated request is recorded
  under stateless HTTP Basic.
-->

# 3. Tactics and where they live

<!-- TODO(slice 4: Mallikarjuna): framing paragraphs. -->

## Identify Actors and Authenticate Actors

<!-- Slice 1 (Godson) writes this subsection. -->

## Authorize Actors and Limit Access

<!-- Slice 2 (Kobe) writes this subsection. -->

**Authorize Actors, per role.** Authorize Actors determines which services an authenticated
actor may use (Bass, Clements, and Kazman, *Software Architecture in Practice*, 4th ed.,
p. 174). `AuthorizationFilter` checks the request's HTTP method and URL before a controller
runs. `AccessPolicy` generates these rules from `AccessRules.all()`, matching `PermitAll`,
`Authenticated`, and `AnyRole` requirements to Spring Security's corresponding checks.
Generating the rules keeps the runtime policy tied to the frozen access table instead of
maintaining a second copy. `SecurityConfig` supplies the hierarchy ADMIN > DOCTOR > NURSE;
each staff rule names its minimum role and inherits the higher roles. PATIENT is outside this
hierarchy. For example, a nurse may record vitals but cannot write prescriptions.

**Authorize Actors, per actor.** A role alone does not establish whose record a patient may
read. `PatientController.record` and `appointments` therefore carry
`@PreAuthorize("@ownRecord.allows(authentication, #id)")`. Method security calls `OwnRecordGuard`
before entering the controller body. A caller holding `ROLE_PATIENT` must have a `ClinicUser`
principal with a patient ID equal to the requested ID; a missing ID or another principal type
is refused. Staff callers pass this check because the URL rule has already restricted admission
to patients or staff. This ordering prevents patients from probing whether another patient's
ID exists: the guard refuses access before a repository lookup could return 404.

**Limit Access.** The final URL rule is `anyRequest().denyAll()`: a service absent from the
table, including an unlisted HTTP method, is unavailable even to ADMIN. This implements
Limit Access (p. 174), so adding a controller does not silently grant access. ERROR dispatches
are permitted first so rendering an existing error does not cause another authorization denial;
an ordinary request to an unlisted URL still meets default deny.

Both URL and method denials publish `AuthorizationDeniedEvent` through the publisher configured
in `SecurityConfig`. The event object identifies either the request or the method invocation.
For an authenticated caller, `ExceptionTranslationFilter` routes the exception to
`ApiAccessDeniedHandler`, which uses `ApiError` to write status 403 and
`{"status":403,"error":"Forbidden","path":"<request URI>"}`. Anonymous callers instead use the
401 entry point supplied by slice 1. Event persistence is the responsibility of slice 3.

## Audit

<!-- Slice 3 (Chase) writes this subsection. -->
