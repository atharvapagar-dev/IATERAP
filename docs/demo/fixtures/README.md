# Synthetic validation fixtures

These files are fictitious demonstration inputs created because no real-time
AdTech data is available to the project team. They contain no customer,
aircraft, or production information.

Before uploading, replace the example `testStepId` values (`101`, `102`, and
`103`) with IDs belonging to the selected procedure revision. The validation
API intentionally rejects readings that are not part of that immutable
revision.

| File | Purpose |
| --- | --- |
| `validation-complete.csv` | All three example steps have matching evidence. |
| `validation-gaps.csv` | One mismatch, one outlier, and one missing reading for the remediation demo. |
| `validation-complete.json` | JSON equivalent of the complete CSV fixture. |

Upload through **Validation Reports** using the selected revision ID and the
matching CSV or JSON format.
