<!-- Owner: Godson (slice 1) for the login diagrams; Kobe (slice 2) for the request diagrams -->
<!--
What belongs in this section, each diagram with a short narrative:
- uml/sequence/login-success.puml (slice 1): BasicAuthenticationFilter -> ProviderManager -> DaoAuthenticationProvider ->
  DemoUserDetailsService -> PasswordEncoder -> AuthenticationSuccessEvent -> AuthorizationFilter -> controller.
- uml/sequence/login-failure.puml (slice 1): the same chain ending in BadCredentialsException ->
  AbstractAuthenticationFailureEvent -> ApiAuthenticationEntryPoint -> 401.
- uml/sequence/authorized-request.puml (slice 2): nurse -> /api/patients/1/record through the role rule and OwnRecordGuard -> 200.
- uml/sequence/forbidden-request.puml (slice 2): bob -> /api/patients/1/record, OwnRecordGuard denies ->
  AuthorizationDeniedEvent -> ApiAccessDeniedHandler -> 403; side note on the pure role denial.
-->

# 6. Sequence diagrams

## Login success and login failure

<!-- Slice 1 (Godson) writes this subsection. -->

## Authorized request and forbidden request

<!-- Slice 2 (Kobe) writes this subsection. -->
