#!/usr/bin/env bash
#
# lint.sh -- repository hygiene checks, run locally and in CI.
#
#   scripts/lint.sh [revision-range]
#
# (a) No tracked file (other than this script) may reference private working notes. The
#     forbidden strings are assembled from fragments, so this script does not contain them
#     literally either.
# (b) No commit in the range may carry a Co-authored-by trailer naming Claude or Anthropic.
#     The range defaults to the whole history reachable from HEAD.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")/.." && pwd)"
cd "$REPO_ROOT" || exit 2

RANGE="${1:-HEAD}"
status=0

echo "==> Hygiene: no references to private working notes"
hygiene_patterns=(
  "gr""ill"
  "gr""ill-decisions"
  "spec-""audit"
  "decision ""0"
)
grep_args=()
for p in "${hygiene_patterns[@]}"; do grep_args+=(-e "$p"); done

# git grep exit codes: 0 = hits (fail), 1 = no hits (ok), >= 2 = git could not run (fall back).
hits=$(git grep -niI "${grep_args[@]}" -- . ':(exclude)scripts/lint.sh' 2>&1)
rc=$?
if [ "$rc" -ge 2 ]; then
  echo "    note: git grep exited $rc; falling back to a recursive grep" >&2
  hits=$(grep -rniI "${grep_args[@]}" . --exclude-dir=.git --exclude-dir=target --exclude=lint.sh 2>&1)
  rc=$?
fi
if [ "$rc" -ge 2 ]; then
  echo "LINT FAIL: could not run the hygiene check (exit $rc):" >&2
  printf '%s\n' "$hits" >&2
  status=1
elif [ "$rc" -eq 0 ]; then
  echo "LINT FAIL: a tracked file references private working notes:" >&2
  printf '%s\n' "$hits" >&2
  status=1
else
  echo "    ok"
fi

echo "==> Commits: no AI co-author trailers in ${RANGE}"
if ! commits=$(git rev-list "$RANGE" 2>&1); then
  echo "LINT FAIL: cannot list commits for range '${RANGE}':" >&2
  printf '%s\n' "$commits" >&2
  status=1
else
  trailer_ok=1
  for c in $commits; do
    if git show -s --format=%B "$c" | grep -qiE '^[[:space:]]*co-authored-by:.*(claude|anthropic)'; then
      echo "LINT FAIL: commit $(git show -s --format='%h %s' "$c") has an AI co-author trailer" >&2
      trailer_ok=0
      status=1
    fi
  done
  [ "$trailer_ok" -eq 1 ] && echo "    ok ($(printf '%s\n' "$commits" | grep -c .) commit(s) checked)"
fi

if [ "$status" -ne 0 ]; then
  echo "lint: FAILED" >&2
  exit 1
fi
echo "lint: passed"
