# Month 3 validation and coverage demo

This demo run uses the synthetic-data profile only. It deliberately exercises
three outcomes so the reviewer can see the complete workflow:

1. Upload an externally authored CSV/JSON fixture against an immutable procedure
   revision from **Validation Reports**.
2. Review matched, mismatched, outlier, and missing readings in the report.
3. Use **Gap remediation loop** to mark each discrepancy addressed, open the
   Procedure Builder, update the revision, and upload the corrected fixture.
4. Open **Coverage Dashboard** to show the updated category heatmap and
   composite score before returning to the approval queue.
5. From **Validation Reports**, select an approved revision and run either
   **Run nominal simulation** or **Inject synthetic fault**. These controls
   generate a labelled JSON fixture and send it through the same validation
   comparison and remediation path as an external upload.

Synthetic injection is approval-gated: draft and in-review revisions are
rejected. The generated run is synthetic evidence only; it is not a hardware
measurement and must not be presented as fleet telemetry.

Use the `synthetic-data` Spring profile for a repeatable, clearly labelled
dataset. The profile resets and reseeds the synthetic domain at startup; it is
not production data and must never be enabled for a production deployment.

## Fixture contract

CSV columns are `testStepId,observedValue,observedUnit,observedBehaviour`.
JSON uses the same fields as an array of objects. `testStepId` values must
belong to the selected procedure revision. Keep one fixture per revision and
source checksum; duplicate uploads are rejected intentionally.
