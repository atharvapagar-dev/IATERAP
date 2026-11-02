# Full Procedure → Coverage → Approval regression

This is the AI-278 Postman regression collection:

- Collection: `IATERAP_Full_Procedure_Coverage_Approval.postman_collection.json`
- Local environment: `IATERAP_Local.postman_environment.json`

## Preconditions

1. Start the backend with a clean synthetic-data profile and the seeded admin account (`admin` / `admin123`), or set equivalent credentials in the selected Postman environment.
2. Import the collection and environment into Postman.
3. Select `IATERAP Local`, then run the collection in its displayed order.

The run resolves IDs from the seeded data instead of hard-coding database identifiers. It creates a unique two-step procedure that covers both seeded interfaces and signals for the chosen LRU, verifies the live coverage contract, submits the procedure, checks the frozen coverage report, approves it, and proves that a second approval is rejected with `INVALID_APPROVAL_TRANSITION`.

The run writes one approved synthetic procedure. Reset/reseed the synthetic database before a repeatable clean-database run.

For Newman, after exporting the selected environment from Postman if required:

```bash
newman run postman/IATERAP_Full_Procedure_Coverage_Approval.postman_collection.json \
  -e postman/IATERAP_Local.postman_environment.json
```
