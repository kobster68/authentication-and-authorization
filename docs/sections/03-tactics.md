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

## Audit

The `SecurityEventListener` applies the Audit tactic (SAiP 4th ed., p. 176) by recording each authentication success and failure and each authorization denial in the bounded, in-memory `AuditLog`, and by writing the same event to the application log at INFO. Each entry includes the time, username, event kind, HTTP method and request path, plus a failure exception name or denial reason. With stateless HTTP Basic, authentication occurs independently on each request, so each authenticated request produces an `AUTHN_SUCCESS` entry rather than a one-time login entry.
