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

<!-- TODO(slice 4: Mallikarjuna): write this section. -->
