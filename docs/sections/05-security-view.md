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
