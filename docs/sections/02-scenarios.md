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

SAiP (Ch. 3 and Ch. 11) states a quality attribute requirement as a scenario with six parts:
source, stimulus, artifact, environment, response, and response measure. We wrote one scenario per
tactic. Each response measure is something the tests and the demo script can check, and Section 9
reports the result.

## Scenario S1: Authenticate Actors

| Part | Value |
| --- | --- |
| Source | An unidentified external human (or a script acting for one), outside the clinic's staff and patients, or a known user who mistypes a password |
| Stimulus | Requests a protected service, for example `GET /api/patients/1/record`, with no credentials, a wrong password, or a username that does not exist |
| Artifact | The clinic portal's HTTP API: the Spring Security filter chain in front of every controller |
| Environment | Normal operation, on a trusted local network; the server has just started or has been running for any time (no session state is kept) |
| Response | The request is refused with `401 Unauthorized` and the challenge `WWW-Authenticate: Basic realm="clinic"`. No controller runs and no protected data is disclosed. A wrong password and an unknown user receive identical responses, so the response does not reveal which usernames exist. A rejected username and password are recorded in the audit log as `AUTHN_FAILURE`; an anonymous request to a protected URL is recorded as `AUTHZ_DENIED` for `anonymous` |
| Response measure | 100% of such requests are refused with 401; 0 bytes of protected data are returned; every attempt that presents a readable username and password produces an audit entry |

## Scenario S2: Authorize Actors

The layout follows the security scenario in Table 19.1 of SAiP.

| Part | Value |
| --- | --- |
| Source | An authenticated internal actor: the PATIENT `bob` (patient 2), who holds valid credentials |
| Stimulus | Asks for another patient's data: `GET /api/patients/1/record` (Alice's record) |
| Artifact | The authorization rules: the URL rules generated from `AccessRules` and the per-actor check `OwnRecordGuard` |
| Environment | Normal operation, on a trusted local network |
| Response | The URL rule admits the request (a PATIENT may call the service), and the own-record check refuses it before the record is read. The caller receives `403 Forbidden` with a JSON body naming only the status and path. The denial is recorded in the audit log as `AUTHZ_DENIED` |
| Response measure | 0 unauthorized grants across all 72 user-by-service cells of the access matrix (Section 4); a patient never learns whether another patient id exists (403 comes before any 404) |

The same response applies to the per-role case: a NURSE posting a prescription, or any non-admin
reading the audit log, is refused with 403 by the URL rule itself.

## The trusted-network assumption

Both scenarios assume a **trusted local network**, and that assumption stands in for transport
encryption (TLS), which the prototype does not provide. HTTP Basic sends the username and password
on every request, only Base64-encoded. Base64 is an encoding, not encryption: anyone who can read
the traffic can recover the password. Over plain HTTP the credentials are therefore exposed on every
request, not only at login. The prototype is safe to run on a developer laptop or a closed lab
network; it is not safe to expose to an untrusted network as it stands. Section 7 rates this risk
(Encrypt Data: not supported, risk High) and Section 8 records the decision.
