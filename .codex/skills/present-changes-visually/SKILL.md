---
name: present-changes-visually
description: Generate a self-contained, GitHub-style split-view HTML page for changes in this repository. Use when asked to visually review, inspect, share, or compare code changes, revisions, branches, commits, or the worktree; do not use for ordinary text-only change summaries.
---

# Present Changes Visually

Create one interactive HTML page that presents each changed file as a side-by-side before/after diff. The page lets readers filter files, highlights modified words, folds long unchanged sections, and keeps unchanged files in collapsed panels.

## Generate the page

1. Use this repository as the target unless the user identifies a different Git repository.
2. Use `HEAD` as the before point and `WORKTREE` as the after point unless the user specifies comparison points. `WORKTREE` includes staged, unstaged, and untracked files, excluding ignored files.
3. Write to `_temp/visual-diff.html` unless the user supplies an output path.
4. From the target repository's root, run:

   ```powershell
   python .codex/skills/present-changes-visually/scripts/generate-split-view-diff.py `
     . HEAD WORKTREE _temp/visual-diff.html
   ```

   Replace the comparison points and output path as requested. Comparison points may be Git commit-ish values such as `HEAD~1`, a tag, branch, or commit SHA.
5. Confirm the command succeeded, verify that the page exists, and report its absolute path. Do not open it in a browser unless the user asks.

## Resource

`scripts/generate-split-view-diff.py` is the bundled standard-library-only generator. It creates a self-contained page, except for optional syntax-highlighting resources loaded by the page.
