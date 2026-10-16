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

All the security in the portal lives in one place: a single Spring Security filter chain, declared
in `SecurityConfig`, that every HTTP request crosses before any controller runs. This is the
Intercepting Filter pattern that SAiP describes (p. 194). The controllers contain no security code
of their own, apart from one `@PreAuthorize` annotation on each of the two patient read services,
and they cannot be reached except through the chain. A new controller therefore cannot skip
authentication or authorization by accident: if nobody adds a row for it to the access table, the
chain's last rule denies it.

The filters that matter here run in this order for each request:

1. `BasicAuthenticationFilter` reads the `Authorization` header and authenticates the caller
   (Identify Actors, Authenticate Actors).
2. `ExceptionTranslationFilter` turns any later security exception into a response: 401 through
   `ApiAuthenticationEntryPoint` for an anonymous caller, 403 through `ApiAccessDeniedHandler` for an
   authenticated one.
3. `AuthorizationFilter` applies the URL rules generated from `AccessRules`, including the final
   default deny (Authorize Actors per role, Limit Access).
4. Method security (`@PreAuthorize`) runs `OwnRecordGuard` just before the two patient read methods
   (Authorize Actors per actor).

Spring Security publishes an event at each decision point, and `SecurityEventListener` turns those
events into audit entries (Audit). Nothing in the request path calls the audit log directly.

An intermediary has a cost (SAiP, p. 179): every request pays for each filter it passes. The
largest part of that cost here is not the filters themselves but the bcrypt comparison that
stateless HTTP Basic repeats on every authenticated request, about 60 to 80 ms on a development
laptop against a few milliseconds for an anonymous call. We accept that cost in exchange for a
single, auditable place where security decisions are made (Section 8, decisions D3 and D5).

The subsections below were written by the slice that built each tactic.

## Identify Actors and Authenticate Actors

<!-- Slice 1 (Godson) writes this subsection. -->

**Identify Actors.** The textbook describes this tactic as identifying "the source of any external
input to the system", and notes that "users are typically identified through user IDs" (Bass,
Clements, and Kazman, *Software Architecture in Practice*, 4th ed., p. 174). In the clinic portal
the actor is identified by the username sent in the HTTP Basic `Authorization` header on every
request (`admin`, `dr.chen`, `nurse.patel`, `alice`, `bob`). Nothing else identifies a caller: there
is no session cookie and no token. A request without the header is treated as anonymous.

**Authenticate Actors.** The textbook defines authentication as "ensuring that an actor is actually
who or what it purports to be", with passwords as the first example of a means to do it (p. 174).
We authenticate with a username and password over HTTP Basic, following the Spring Security
reference for Basic authentication
(https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html).
The work is done by these classes, in the order a request meets them:

1. `BasicAuthenticationFilter` (Spring Security) is the filter in the chain that reads the
   `Authorization: Basic ...` header, decodes the username and password, and asks the
   authentication manager to check them. A request without the header passes through it
   unauthenticated.
2. `ProviderManager` (Spring Security) is the authentication manager. It hands the credentials to
   its provider and publishes an `AuthenticationSuccessEvent` when the provider accepts them, or a
   subclass of `AbstractAuthenticationFailureEvent` when it rejects them. These events are how the audit listener learns about each
   attempt; nothing in our code calls the audit log directly. There is one gap: a malformed
   `Authorization: Basic` header (not Base64, or no `username:password` inside) is rejected by
   Spring's `BasicAuthenticationConverter` before `ProviderManager` runs, so the caller gets a 401
   but no failure event is published and the attempt never reaches the audit log; a header with a
   scheme other than Basic is simply ignored, and the caller is treated as anonymous. Only attempts
   that present a username and password that can be read are recorded.
3. `DaoAuthenticationProvider` (Spring Security) loads the user by name and compares the password.
4. `DemoUserDetailsService` (our `authn` package) is the user store. It looks the name up among the
   five seeded users and returns a new `ClinicUser`, which carries the user's role and, for a
   patient, the patient id that the own-record rule needs later. An unknown name throws
   `UsernameNotFoundException` and nothing else.
5. `DelegatingPasswordEncoder` (Spring Security, created by
   `PasswordEncoderFactories.createDelegatingPasswordEncoder()` in `SecurityConfig`) checks the
   password. Stored passwords are `{bcrypt}` hashes; the prefix tells the delegating encoder to use
   BCrypt (cost 10). No plaintext password appears in the application code.
6. `ApiAuthenticationEntryPoint` (our `authn` package) writes the response when authentication
   fails.

**How failure is surfaced.** Every authentication failure ends in `ApiAuthenticationEntryPoint`,
which answers `401 Unauthorized` with the challenge header `WWW-Authenticate: Basic realm="clinic"`
and the JSON body `{"status":401,"error":"Unauthorized","path":"<request URI>"}`. That covers a
wrong password, an unknown user, a malformed `Authorization` header, and an anonymous caller on any
URL except the public one. Bad credentials are refused even on the public URL, because the Basic
filter runs before any URL rule. The body never says why authentication failed. A wrong password and an
unknown user produce byte-identical responses: `DaoAuthenticationProvider` reports an unknown user
as the same `BadCredentialsException` as a wrong password, and still runs a BCrypt comparison
against a dummy hash, so the response time does not give the difference away either. This is
Spring's default behaviour; we did not configure it, so `AuthenticationTest` checks it (identical
status, headers, body, and failure event for `nurse.patel:wrong` and `mallory:x`) rather than
assuming it.

**Every request is authenticated again.** The filter chain is stateless: no HTTP session is
created, so the server remembers nothing between requests. Each request carries the credentials
and goes through the whole sequence above, including a BCrypt comparison. On a development laptop
that costs roughly 70 to 80 ms per authenticated request, against about 5 ms for an anonymous call
to the public URL. We accept that cost to keep the server free of session state. It also means that
"authenticated" in the audit log means "this request was authenticated", not "this user logged in
once". The sequence diagrams in Section 6 show the success and failure paths message by message.

## Authorize Actors and Limit Access

<!-- Slice 2 (Kobe) writes this subsection. -->

## Audit

<!-- Slice 3 (Chase) writes this subsection. -->
