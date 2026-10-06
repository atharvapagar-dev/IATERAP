"""Regression tests for the synthetic validation-fixture generator."""

from __future__ import annotations

import csv
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).parent
GENERATOR = ROOT / "generate_fixture.py"


class GenerateFixtureTest(unittest.TestCase):
    def test_generates_upload_ready_csv(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "fixture.csv"
            self._run("--format", "csv", "--output", str(output), "--reading",
                      "101,4.20,VOLT,Synthetic nominal voltage")
            with output.open(encoding="utf-8", newline="") as file:
                rows = list(csv.DictReader(file))
            self.assertEqual(rows, [{
                "testStepId": "101",
                "observedValue": "4.20",
                "observedUnit": "VOLT",
                "observedBehaviour": "Synthetic nominal voltage",
            }])

    def test_generates_upload_ready_json_and_rejects_duplicate_ids(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "fixture.json"
            self._run("--format", "json", "--output", str(output), "--reading",
                      "101,12.5,MILLIAMPERE,Synthetic nominal current")
            self.assertEqual(json.loads(output.read_text(encoding="utf-8"))[0]["testStepId"], 101)

            duplicate = subprocess.run(
                [sys.executable, str(GENERATOR), "--format", "json", "--output", str(output),
                 "--reading", "101,12.5,MILLIAMPERE,First", "--reading", "101,12.6,MILLIAMPERE,Second"],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertNotEqual(duplicate.returncode, 0)
            self.assertIn("unique testStepId", duplicate.stderr)

    @staticmethod
    def _run(*arguments: str) -> None:
        subprocess.run([sys.executable, str(GENERATOR), *arguments], check=True)


if __name__ == "__main__":
    unittest.main()
