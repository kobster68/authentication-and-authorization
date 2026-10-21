#!/usr/bin/env bash
#
# export-pdf.sh -- concatenate the design document sections into docs/design-document.pdf.
#
# The sections are docs/sections/01-*.md ... 09-*.md, joined in file-name order by pandoc.
# Images are resolved relative to the repository root, docs/sections and uml/.
# The Lua filter scripts/pdf-figures.lua puts each diagram on its own landscape page so the
# large figures stay legible; it only affects LaTeX output, so the Markdown is untouched.
# pandoc needs a PDF engine; set PDF_ENGINE to choose one (for example xelatex, tectonic or
# wkhtmltopdf), otherwise pandoc's default is used.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

if ! command -v pandoc >/dev/null 2>&1; then
  echo "export-pdf: pandoc is not installed or not on PATH." >&2
  echo "            Install it from https://pandoc.org/installing.html (macOS: brew install pandoc;" >&2
  echo "            Ubuntu: sudo apt-get install pandoc texlive-latex-recommended) and run again." >&2
  exit 1
fi

shopt -s nullglob
sections=(docs/sections/[0-9][0-9]-*.md)
if [ "${#sections[@]}" -eq 0 ]; then
  echo "export-pdf: no section files found under docs/sections/" >&2
  exit 1
fi

engine_args=()
if [ -n "${PDF_ENGINE:-}" ]; then
  engine_args=(--pdf-engine="${PDF_ENGINE}")
fi

out="docs/design-document.pdf"
pandoc "${sections[@]}" \
  --from=markdown \
  --resource-path=".:docs/sections:uml" \
  --lua-filter=scripts/pdf-figures.lua \
  --metadata title="Authentication and Authorization: Design Document" \
  -V header-includes='\usepackage{graphicx}' \
  -V header-includes='\usepackage{pdflscape}' \
  ${engine_args[@]+"${engine_args[@]}"} \
  -o "$out"
echo "export-pdf: wrote $out from ${#sections[@]} section file(s)"
