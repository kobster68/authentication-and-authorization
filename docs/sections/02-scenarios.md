<!-- Owner: Mallikarjuna (slice 4) -->
<!--
What belongs in this section:
- The authentication scenario in six parts (source, stimulus, artifact, environment, response, response measure):
  an unidentified external human requests a protected service with missing or invalid credentials; normal operation on a
  trusted local network (the assumption that stands in for TLS, stated plainly); request refused with 401, no data disclosed,
  attempt recorded; 100% of such requests refused, 0 bytes of protected data returned.
- The authorization scenario in six parts, modelled on Table 19.1: an authenticated PATIENT asks for another patient's record;
  403, attempt recorded; 0 unauthorized grants across all cells of the access matrix.
- The plain statement that HTTP Basic without TLS sends reversible credentials on every request.
-->

# 2. Security scenarios

<!-- TODO(slice 4: Mallikarjuna): write this section. -->
