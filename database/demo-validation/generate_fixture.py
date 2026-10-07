"""Generate a synthetic CSV or JSON validation-reading fixture.

This script is intentionally dependency-free and does not connect to IATERAP
or a database. The caller supplies step IDs from the selected approved revision
and independent synthetic readings for demo use.
"""

from __future__ import annotations

import argparse
import csv
import json
from decimal import Decimal, InvalidOperation
from pathlib import Path

HEADERS = ("testStepId", "observedValue", "observedUnit", "observedBehaviour")


def parse_reading(raw: str) -> dict[str, object]:
    """Parse STEP_ID,VALUE,UNIT,BEHAVIOUR, permitting commas in behaviour."""
    pieces = raw.split(",", 3)
    if len(pieces) != 4:
        raise argparse.ArgumentTypeError(
            "--reading must be STEP_ID,VALUE,UNIT,OBSERVED_BEHAVIOUR"
        )
    step_id, value, unit, behaviour = (piece.strip() for piece in pieces)
    try:
        parsed_id = int(step_id)
        if parsed_id <= 0:
            raise ValueError
    except ValueError as exc:
        raise argparse.ArgumentTypeError("testStepId must be a positive integer") from exc
    try:
        Decimal(value)
    except InvalidOperation as exc:
        raise argparse.ArgumentTypeError("observedValue must be numeric") from exc
    if not unit:
        raise argparse.ArgumentTypeError("observedUnit is required for numeric readings")
    return {
        "testStepId": parsed_id,
        "observedValue": value,
        "observedUnit": unit.upper(),
        "observedBehaviour": behaviour,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Create a synthetic IATERAP validation fixture")
    parser.add_argument("--format", choices=("csv", "json"), required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--reading", action="append", type=parse_reading, required=True)
    args = parser.parse_args()

    step_ids = [reading["testStepId"] for reading in args.reading]
    if len(step_ids) != len(set(step_ids)):
        parser.error("each --reading must use a unique testStepId")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.format == "csv":
        with args.output.open("w", newline="", encoding="utf-8") as file:
            writer = csv.DictWriter(file, fieldnames=HEADERS)
            writer.writeheader()
            writer.writerows(args.reading)
    else:
        with args.output.open("w", encoding="utf-8") as file:
            json.dump(args.reading, file, indent=2)
            file.write("\n")


if __name__ == "__main__":
    main()
