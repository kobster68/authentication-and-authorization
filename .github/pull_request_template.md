## Slice

<!-- Which slice or change is this? e.g. "Slice 2: Authorization", or "Contract change: <what and why>". -->

## Reviewer

<!-- The named reviewer for this slice (slice 1: Kobe, slice 2: Chase, slice 3: Mallikarjuna, slice 4 and contract changes: Godson). -->

## Tests enabled

<!-- Which @Disabled test classes or methods this PR enables, and any finer tests added. -->

- [ ] 

## Demo rows enabled

<!-- Which rows of scripts/demo-matrix.txt this PR uncomments (by "needs:" set), or "none". -->

- [ ] 

## Design document sections touched

<!-- Which docs/sections/*.md files and uml/ diagrams changed, or "none". -->

- [ ] 

## Checklist

- [ ] `mvn -B verify` passes locally
- [ ] `scripts/lint.sh` passes
- [ ] `scripts/demo.sh` against a running jar prints every enabled row OK
- [ ] No change to the frozen contract (or this PR is the contract change itself, reviewed by Godson)
