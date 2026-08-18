---
name: test-ui
description: Run the project's console UI test plan and compare each case's expected output with the program's actual output. Use when asked to execute, verify, or update UI/CLI interaction tests; do not use for unit tests that do not exercise console input and output.
---

# Test UI

Run the test cases in `test/ui-test-plan.md`. Each case starts a fresh Stockie process, supplies its listed input lines through standard input, and compares standard output exactly after normalizing line endings. Test case inputs are recorded separately from output because terminal echo is not part of the program's standard output.

## Test plan format

Keep the test plan in this structure:

````markdown
## Test Case: short descriptive name
### Aim
What behaviour this case verifies.

### Inputs
```text
one input line
another input line
```

### Expected Output
```text
Exact program output
```
````

Each case must include an aim, input block, and expected-output block. Use the exact output the program should produce; do not include the user's typed input in the expected-output block.

## Run tests

1. Update `test/ui-test-plan.md` if the requested scenarios or expected output have changed.
2. Run the test runner from the repository root:

   ```powershell
   python .codex/skills/test-ui/scripts/run-ui-tests.py
   ```

   The runner requires Java 25, compiles all files in `src/main/java` into `_temp/ui-test-classes`, and runs the `Stockie` entry point for every case.
3. Review `test/ui-test-results.md`, which records the supplied console input and actual console output for each session.
4. If any case fails, stop immediately. Report the failing case and its actual and expected output; do not continue to later cases.

Use `--validate-plan` to check the Markdown structure without compiling or running the program.
