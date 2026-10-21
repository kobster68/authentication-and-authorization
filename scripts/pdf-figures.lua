-- pdf-figures.lua -- make every diagram legible in the PDF export.
--
-- pandoc 3 wraps a paragraph that is a single image with alt text into a Figure.
-- For LaTeX output this filter replaces each such Figure with a raw LaTeX block
-- that places the image on its own landscape page, scaled to fill the page while
-- keeping its aspect ratio, with the caption underneath. Non-LaTeX output (for
-- example GitHub's Markdown rendering, which never runs this filter) is untouched.
--
-- Used by scripts/export-pdf.sh via --lua-filter.

-- Escape the LaTeX special characters so plain caption words render verbatim.
local function latex_escape(s)
  local map = {
    ["\\"] = "\\textbackslash{}",
    ["{"] = "\\{",
    ["}"] = "\\}",
    ["$"] = "\\$",
    ["&"] = "\\&",
    ["#"] = "\\#",
    ["^"] = "\\textasciicircum{}",
    ["_"] = "\\_",
    ["~"] = "\\textasciitilde{}",
    ["%"] = "\\%",
  }
  return (s:gsub("[\\{}%$&#%^_~%%]", map))
end

-- Return the first Image found anywhere inside the figure's content blocks.
local function first_image(blocks)
  local found
  pandoc.walk_block(pandoc.Div(blocks), {
    Image = function(img)
      found = found or img
      return nil
    end,
  })
  return found
end

function Figure(fig)
  if not FORMAT:match("latex") then
    return nil
  end

  local img = first_image(fig.content)
  if not img then
    return nil
  end

  -- The Markdown references images relative to docs/sections (for example
  -- ../../uml/sequence/login-success.png). pandoc and pdflatex both run from the
  -- repository root, so rewrite a leading ../../ to a root-relative path that
  -- \includegraphics can resolve directly.
  local src = img.src:gsub("^%.%./%.%./", "")

  -- Prefer the figure caption; fall back to the image's alt text.
  local caption = pandoc.utils.stringify(fig.caption.long)
  if caption == "" then
    caption = pandoc.utils.stringify(img.caption)
  end

  local latex = table.concat({
    "\\begin{landscape}",
    "\\begin{figure}[p]",
    "\\centering",
    "\\includegraphics[width=\\linewidth,height=0.92\\textheight,keepaspectratio]{" .. src .. "}",
    "\\caption{" .. latex_escape(caption) .. "}",
    "\\end{figure}",
    "\\end{landscape}",
  }, "\n")

  return pandoc.RawBlock("latex", latex)
end
