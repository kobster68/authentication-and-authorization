#!/usr/bin/env bash
#
# run.sh -- build the executable jar and start the server, passing every argument through.
#
# Works from any directory: it resolves the repository root from this script's location, builds
# target/authentication-and-authorization.jar (tests skipped for a fast start), and runs it.
# The server listens on port 8080 unless told otherwise. Examples:
#   scripts/run.sh
#   scripts/run.sh --server.port=9090
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"
mvn -q -DskipTests package
exec java -jar target/authentication-and-authorization.jar "$@"
