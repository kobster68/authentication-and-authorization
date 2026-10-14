#!/usr/bin/env bash
#
# demo.sh -- check the access matrix against a running server with curl.
#
#   scripts/demo.sh [base-url]        (default http://localhost:8080)
#
# Reads every enabled (uncommented) row of scripts/demo-matrix.txt, sends that request, and prints
#   EXPECTED ACTUAL METHOD PATH USER OK|MISMATCH
# Then it prints the audit log as admin, on a best-effort basis (its status never counts as a
# mismatch).
#
# Exit status: 0 when every row matches; 1 on any mismatch or malformed row; 2 when the server is
# not reachable (GET /api/public/info does not return 200), so a wrong port is reported once
# instead of as a mismatch on every row.
#
# Start the server first, for example:
#   scripts/run.sh --server.port=9090 &
#   scripts/demo.sh http://localhost:9090
set -u
set -f

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
MATRIX="${SCRIPT_DIR}/demo-matrix.txt"
BASE_URL="${1:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"

VITALS_JSON='{"systolic":120,"diastolic":80,"pulse":72}'
PRESCRIPTION_JSON='{"drug":"amoxicillin","dose":"500mg"}'

# The demo passwords. They are published in the README on purpose (evaluation only); the server
# stores only their bcrypt hashes. The Java tests keep the same table in
# src/test/java/edu/rit/swen755/auth/TestFixtures.java (PASSWORDS); keep the two in step.
password_for() {
  case "$1" in
    admin) echo "admin-pass" ;;
    dr.chen) echo "chen-pass" ;;
    nurse.patel) echo "patel-pass" ;;
    alice) echo "alice-pass" ;;
    bob) echo "bob-pass" ;;
    *) return 1 ;;
  esac
}

if ! command -v curl >/dev/null 2>&1; then
  echo "demo: curl is required but was not found on PATH" >&2
  exit 2
fi
if [ ! -f "$MATRIX" ]; then
  echo "demo: matrix file not found: $MATRIX" >&2
  exit 2
fi

reachable=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "${BASE_URL}/api/public/info" </dev/null)
if [ "$reachable" != "200" ]; then
  echo "demo: server not reachable at ${BASE_URL} (GET /api/public/info returned ${reachable:-nothing}, expected 200)" >&2
  exit 2
fi

rows=0
mismatches=0
print_row() {
  printf '%-8s %-6s %-6s %-33s %-18s %s\n' "$1" "$2" "$3" "$4" "$5" "$6"
}

echo "demo: ${BASE_URL}"
print_row EXPECTED ACTUAL METHOD PATH USER RESULT

while IFS= read -r line || [ -n "$line" ]; do
  line="${line%%#*}"
  # shellcheck disable=SC2086 # deliberate word splitting into the five columns (globbing is off)
  set -- $line
  [ "$#" -eq 0 ] && continue
  if [ "$#" -ne 5 ]; then
    echo "demo: malformed row (expected 5 columns): $line" >&2
    mismatches=$((mismatches + 1))
    continue
  fi
  expected="$1"; method="$2"; path="$3"; user="$4"; body="$5"

  args=(-s -o /dev/null -w '%{http_code}' --max-time 30 -X "$method")
  case "$user" in
    -) ;;
    *:*) args+=(-u "$user") ;;
    *)
      if ! password=$(password_for "$user"); then
        echo "demo: no password known for user '$user' (write it as user:password): $line" >&2
        mismatches=$((mismatches + 1))
        continue
      fi
      args+=(-u "${user}:${password}")
      ;;
  esac
  case "$body" in
    -) ;;
    vitals) args+=(-H 'Content-Type: application/json' --data "$VITALS_JSON") ;;
    prescription) args+=(-H 'Content-Type: application/json' --data "$PRESCRIPTION_JSON") ;;
    *)
      echo "demo: unknown body '$body' (use -, vitals or prescription): $line" >&2
      mismatches=$((mismatches + 1))
      continue
      ;;
  esac

  actual=$(curl "${args[@]}" "${BASE_URL}${path}" </dev/null)
  rows=$((rows + 1))
  if [ "$actual" = "$expected" ]; then
    result="OK"
  else
    result="MISMATCH"
    mismatches=$((mismatches + 1))
  fi
  print_row "$expected" "$actual" "$method" "$path" "$user" "$result"
done < "$MATRIX"

echo
echo "Audit log (GET /api/admin/audit as admin; best effort, not part of the check):"
audit=$(curl -s --max-time 30 -w '\n%{http_code}' -u "admin:$(password_for admin)" "${BASE_URL}/api/admin/audit" </dev/null)
audit_status="${audit##*$'\n'}"
if [ "$audit_status" = "200" ]; then
  printf '%s\n' "${audit%$'\n'*}"
else
  echo "  (not available: HTTP ${audit_status:-none})"
fi

echo
echo "demo: ${rows} rows checked, ${mismatches} mismatch(es)"
if [ "$mismatches" -ne 0 ]; then
  exit 1
fi
exit 0
