# Month 4 Milestone Demo Rehearsal Checklist

Use this checklist with the other intern before the demo. Record the environment, presenter, and duration beside each item; this is a rehearsal aid, not production data.

| Segment | Expected result | Pass |
|---|---|---|
| Login and procedure selection | Authenticated Test Engineer opens a versioned procedure. | [ ] |
| Advisory request | AI Suggestions creates a pending, labelled advisory item. | [ ] |
| Explicit acceptance | Only the requesting engineer accepts the item; the audit identity is visible. | [ ] |
| Builder application | Accepted item appears in Procedure Builder and changes only the unsaved draft. | [ ] |
| Versioned save | Saving creates the normal attributable procedure revision. | [ ] |
| Synthetic reliability evidence | Aircraft generation creates labelled synthetic sortie/failure/repair events. | [ ] |
| Reporting and dashboard | Current reporting projection, Pareto ranking, and HFRA flags load. | [ ] |
| Safety statement | Presenter states that AI is advisory and all fleet evidence is synthetic. | [ ] |

## Recovery notes

- If FastAPI is unavailable, demonstrate the controlled advisory error and continue all core procedure/coverage/reliability actions.
- If no report rows appear, generate synthetic events and wait for or invoke the reporting refresh in the development environment.
- Do not claim real aircraft telemetry, physical maintenance results, or automatic AI procedure edits.
