#!/usr/bin/env python3
"""Run Stockie's Markdown-defined console UI tests."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

PLAN_PATH = Path("test/ui-test-plan.md")
RESULTS_PATH = Path("test/ui-test-results.md")
SOURCE_DIRECTORY = Path("src/main/java")
CLASS_DIRECTORY = Path("_temp/ui-test-classes")
TEST_DATA_DIRECTORY = Path("_temp/ui-test-data")
ENTRY_POINT = "Stockie"
TEST_TIMEOUT_SECONDS = 10


@dataclass
class TestCase:
    """One console UI test parsed from the Markdown plan."""

    name: str
    aim: str
    inputs: str
    expected_output: str


CASE_PATTERN = re.compile(
    r"^## Test Case: (?P<name>.+?)\r?$"
    r"\s*^### Aim\r?$\s*(?P<aim>.*?)"
    r"\s*^### Inputs\r?$\s*^```(?:text)?\r?$\n(?P<inputs>.*?)^```\r?$"
    r"\s*^### Expected Output\r?$\s*^```(?:text)?\r?$\n(?P<expected>.*?)^```\r?$",
    re.MULTILINE | re.DOTALL,
)


def normalise_output(text: str) -> str:
    """Make line endings platform-independent while retaining visible whitespace."""
    return text.replace("\r\n", "\n").replace("\r", "\n").rstrip("\n")


def parse_plan(plan_path: Path) -> list[TestCase]:
    """Read the required test case fields from the UI test plan."""
    if not plan_path.is_file():
        raise ValueError(f"Test plan not found: {plan_path}")
    content = plan_path.read_text(encoding="utf-8")
    cases = [
        TestCase(
            name=match.group("name").strip(),
            aim=match.group("aim").strip(),
            inputs=match.group("inputs").rstrip("\r\n"),
            expected_output=normalise_output(match.group("expected")),
        )
        for match in CASE_PATTERN.finditer(content)
    ]
    if not cases:
        raise ValueError(
            "No valid test cases found. Each case needs Aim, Inputs, and Expected Output sections."
        )
    return cases


def require_java_25() -> None:
    """Fail early when the project is not being built with its required JDK."""
    version = subprocess.run(["javac", "-version"], capture_output=True, text=True, check=False)
    reported = (version.stdout + version.stderr).strip()
    if version.returncode != 0 or not re.search(r"\b25(?:[.\s]|$)", reported):
        raise RuntimeError(f"Java 25 is required; found: {reported or 'no javac executable'}")


def compile_program(repo: Path) -> None:
    """Compile application sources into the temporary test class directory."""
    sources = sorted(str(path) for path in SOURCE_DIRECTORY.rglob("*.java"))
    if not sources:
        raise RuntimeError(f"No Java source files found in {SOURCE_DIRECTORY}")
    classes = repo / CLASS_DIRECTORY
    classes.mkdir(parents=True, exist_ok=True)
    result = subprocess.run(
        ["javac", "--release", "25", "-d", str(classes), *sources],
        cwd=repo, capture_output=True, text=True, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError("Compilation failed:\n" + (result.stdout + result.stderr).strip())


def fenced_block(text: str) -> str:
    """Format test-session text as a Markdown code block."""
    return "```text\n" + (text if text else "(empty)") + "\n```"


def write_results(records: list[dict], termination: str | None = None) -> None:
    """Persist console input and output for every completed test case."""
    lines = ["# UI Test Results", "", f"Run: {datetime.now().isoformat(timespec='seconds')}", ""]
    for record in records:
        case = record["case"]
        lines += [
            f"## Test Case: {case.name}", f"**Aim:** {case.aim}", "",
            f"**Status:** {'PASS' if record['passed'] else 'FAIL'}", "",
            "### Console Input", fenced_block(case.inputs), "",
        ]
        if not record["passed"]:
            lines += ["### Expected Output", fenced_block(case.expected_output), ""]
        lines += ["### Actual Console Output", fenced_block(record["actual"]), ""]
    if termination:
        lines += ["## Session Terminated", "", termination, ""]
    RESULTS_PATH.parent.mkdir(parents=True, exist_ok=True)
    RESULTS_PATH.write_text("\n".join(lines), encoding="utf-8")


def prepare_test_data_file(repo: Path, index: int) -> Path:
    """Create a fresh persistence file that cannot overlap production storage."""
    data_directory = repo / TEST_DATA_DIRECTORY
    data_directory.mkdir(parents=True, exist_ok=True)
    data_file = data_directory / f"case-{index}.dat"
    production_file = (repo / "stockie-inventory.dat").resolve()
    if data_file.resolve() == production_file:
        raise RuntimeError("UI test persistence path must differ from production persistence path")
    data_file.unlink(missing_ok=True)
    data_file.touch()
    return data_file


def run_case(repo: Path, case: TestCase, data_file: Path) -> str:
    """Run one fresh Stockie process and return its standard output."""
    result = subprocess.run(
        ["java", f"-Dstockie.data.file={data_file}", "-cp",
         str(repo / CLASS_DIRECTORY), ENTRY_POINT],
        cwd=repo, input=case.inputs + "\n", capture_output=True, text=True,
        timeout=TEST_TIMEOUT_SECONDS, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError("Program exited with an error:\n" + (result.stdout + result.stderr).strip())
    return normalise_output(result.stdout)


def main() -> int:
    """Validate the plan or execute cases until the first failure."""
    parser = argparse.ArgumentParser(description="Run Stockie's console UI test plan.")
    parser.add_argument("--validate-plan", action="store_true", help="validate plan structure only")
    args = parser.parse_args()
    try:
        cases = parse_plan(PLAN_PATH)
        if args.validate_plan:
            print(f"Valid UI test plan with {len(cases)} case(s).")
            return 0

        repo = Path.cwd()
        require_java_25()
        compile_program(repo)
        records: list[dict] = []
        for index, case in enumerate(cases, start=1):
            test_data_file = prepare_test_data_file(repo, index)
            actual = run_case(repo, case, test_data_file)
            passed = actual == case.expected_output
            records.append({"case": case, "actual": actual, "passed": passed})
            write_results(records)

            print(f"=== Test {index}: {case.name} ===")
            print("Console input:")
            print(case.inputs or "(empty)")
            print("Console output:")
            print(actual)
            if passed:
                print("PASS\n")
                continue

            termination = f"Stopped after failed test case: {case.name}"
            write_results(records, termination)
            print("FAIL\nExpected output:\n" + case.expected_output)
            print("Actual output:\n" + actual)
            return 1
    except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
        print(f"Test session could not complete: {error}", file=sys.stderr)
        return 1

    print(f"All {len(cases)} UI test case(s) passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
