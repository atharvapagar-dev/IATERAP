# Synthetic external-validation fixture pack

This package contains **synthetic-only** data and tooling for demonstrating
IATERAP's external-reading validation workflow. It contains no Adtech product,
customer, aircraft, tester, or operational data.

## What this proves

The validation workflow compares an independently supplied CSV or JSON reading
file against the expected contract of a selected **immutable procedure
revision**. The source file is not generated from the procedure by IATERAP.
The result can therefore be `MATCH`, `OUTLIER`, `MISMATCH`, or `MISSING`.

The importer rejects a `testStepId` that does not belong to the selected
revision. This prevents a reading for one LRU/procedure from being submitted
against another one.

## Prepare the synthetic scenario

1. Run the backend with the `synthetic-data` Spring profile. It seeds clearly
   labelled synthetic LRUs, interfaces, signals, protocols, and fault templates.
2. In the Procedure Builder, create a procedure for one synthetic LRU, add
   numeric test steps with expected range/unit values, submit it, and approve it.
3. Open the approved revision in **Validation Reports** and note its actual
   `testStepId` values and expected values. IDs are database-generated; never
   assume that IDs in one local database exist in another.
4. Generate an upload-ready fixture with the supplied script. For example:

   ```powershell
   python .\generate_fixture.py --format csv --output .\generated\nominal.csv `
     --reading '101,4.20,VOLT,Voltage remains within the nominal band' `
     --reading '102,12.50,MILLIAMPERE,Current remains within the nominal band'
   ```

   Use IDs and values obtained from the selected approved revision. Generate
   JSON by replacing `--format csv` with `--format json`.

5. Upload the generated file from **Validation Reports**, selecting the same
   procedure revision. A nominal fixture should return `MATCH` for each
   supplied in-range reading.

## Expected input contract

CSV headers, in this exact spelling:

```text
testStepId,observedValue,observedUnit,observedBehaviour
```

JSON is an array of objects with the same field names. `observedValue` and
`observedBehaviour` are optional individually, but a usable numeric comparison
requires both an observed value and observed unit. Supported unit identifiers
are the canonical service names such as `VOLT`, `MILLIVOLT`, `AMPERE`, and
`MILLIAMPERE`.

## Included negative fixtures

- `fixtures/reject-unknown-step-id.csv` — a validly structured file with a
  deliberately impossible step ID. Uploading it to any normal demo revision
  must be rejected before comparison.
- `fixtures/reject-unknown-column.csv` — invalid CSV schema. It must be
  rejected by the parser.
- `fixtures/reject-duplicate-step-id.json` — duplicate step IDs. It must be
  rejected by the parser.

Do not upload the same file twice to the same procedure revision: source hashes
are intentionally deduplicated. Generate a fresh file if a repeat run is
needed.

## Safety boundary

These files are demonstration evidence only. They must never be described as
hardware measurements, telemetry, or fleet data.
