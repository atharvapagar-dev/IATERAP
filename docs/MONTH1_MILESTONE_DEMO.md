# IATERAP - Month 1 Milestone Demo

**Story:** AI-157
**Covers:** Weeks 1-4 (project scaffolding, database/auth foundation, LRU/signal
domain CRUD, synthetic data seeding, and refresh-token session management).

This document is the single package a presenter needs to run the Month 1
milestone demo: a demo script, a dry-run checklist, a milestone walkthrough,
presenter notes, a verification checklist, three demonstration flows (CRUD,
authentication, synthetic data), and a final readiness checklist.

---

## 1. Milestone Walkthrough (what Month 1 delivered)

| Week | Delivered |
|------|-----------|
| 1 | Repository scaffolding: Spring Boot backend, React + Vite frontend, PostgreSQL + Flyway, Docker-based local setup. |
| 2 | Core domain schema (LRU, Interface, Connector, Signal Library, Signal Range) and baseline CRUD APIs. |
| 3 | Protocol Tag / Fault Template / FMEA branch mapping schema, composite indexes, audit log, hashed seed-user passwords, Test Block library schema. |
| 4 (this milestone - AI-155/156/157) | Full-domain synthetic data seeding, JWT refresh-token rotation + session revocation (logout and role-change), and this demo package. |

The Month 1 story, end to end: a test engineer logs in, browses realistic
synthetic LRU/FMEA/test-block data seeded automatically at startup, performs
CRUD against the domain APIs, and the session layer correctly rotates and
revokes tokens as they work.

---

## 2. Dry Run Checklist (complete 24-48h before the live demo)

Run through this once, start to finish, in an environment as close to the
live demo environment as possible.

- [ ] `git pull` on the `develop` branch; confirm `AI-154`, `AI-158`,
      `AI-155`, `AI-156` are all present (`git log --oneline -10`).
- [ ] `docker compose up -d` (or start local PostgreSQL) and confirm the
      `iaterap` database is reachable on `5432`.
- [ ] Backend starts cleanly with the `synthetic-data` profile active and
      Flyway reports all 9 migrations (`V1`...`V9`) applied with no errors.
- [ ] Confirm the seeder ran exactly once (no duplicate-key errors in the
      startup log) - re-run `mvn spring-boot:run` a second time and confirm
      row counts stay identical (idempotency check for AI-155).
- [ ] Frontend (`npm run dev`) starts and can reach the backend
      (check the network tab for a successful `/api/health` call).
- [ ] Log in with a seeded demo account; confirm both an access token and a
      refresh token come back.
- [ ] Call `/api/auth/refresh` with the refresh token from the previous
      step; confirm a **new** token pair is returned and the old refresh
      token is now rejected if reused (rotation check for AI-156).
- [ ] Call `/api/auth/logout`; confirm the (now-current) refresh token is
      rejected by `/api/auth/refresh` afterwards (revocation check).
- [ ] `PATCH /api/users/{id}/role` on a test account with an active
      session, then confirm that account's outstanding refresh token is
      rejected by `/api/auth/refresh` (role-change revocation check).
- [ ] Walk the CRUD flow below end to end once, exactly as it will be
      demoed, on the actual demo machine/screen-share setup.
- [ ] Charge laptop, close unrelated apps/notifications, confirm screen
      resolution/zoom level is legible for the audience.
- [ ] Have a fallback: a short screen recording of the dry run, in case of
      live environment issues on demo day.

---

## 3. Demo Script (~12-15 minutes)

**0:00 - 1:00 - Framing**
"This is the Month 1 milestone for IATERAP - the Integrated Aerospace Test
Engineering & Reliability Analytics Platform. Today I'll show three things:
CRUD against the core reliability-engineering domain, authentication with
session management, and the realistic synthetic dataset the whole demo runs
on."

**1:00 - 4:00 - Synthetic Data Generation Flow** (see Section 4.3)
Show the backend starting up with the `synthetic-data` profile, the seeded
LRUs/FMEA rows/test blocks, and point out the `(Synthetic)` labelling
convention and idempotent reseed.

**4:00 - 8:00 - Authentication Flow** (see Section 4.2)
Log in, show the two tokens returned, refresh, then log out, then show a
revoked refresh token being rejected. Briefly show the role-change
revocation call.

**8:00 - 13:00 - CRUD Flow** (see Section 4.1)
Create, read, update, and delete an LRU (or Project/Aircraft) through the
UI, pointing out validation and the audit log entry each write produces.

**13:00 - 15:00 - Wrap-up**
Recap what's implemented vs. what's next (Week 5+ stories), take questions.

---

## 4. Demonstration Flows

### 4.1 CRUD Demonstration Flow

1. Open the frontend and navigate to the LRU (or Project) list view -
   point out the existing seeded synthetic rows.
2. **Create**: add a new LRU/Project through the UI form; show the
   validation error first (submit blank), then a successful create.
3. **Read**: open the newly created record's detail view.
4. **Update**: edit a field (e.g. description) and save; show the updated
   value persisted after a page refresh.
5. **Delete**: delete the record created in step 2; show it disappears
   from the list.
6. (Optional, if time allows) Open the audit log endpoint/table and show
   the CREATE/UPDATE/DELETE entries this flow just produced.

### 4.2 Authentication Demonstration Flow

1. **Login** - `POST /api/auth/login` with a seeded demo user's
   credentials. Show the response contains both `accessToken` and
   `refreshToken`.
2. **Authenticated call** - use the access token as a Bearer token against
   any protected endpoint (e.g. `GET /api/projects`); show it succeeds.
3. **Refresh (rotation)** - `POST /api/auth/refresh` with the refresh
   token. Show a brand-new token pair comes back.
4. **Reuse rejection** - call `/api/auth/refresh` again with the
   *original* (now-rotated) refresh token; show it is rejected
   (401). This is the rotation guarantee from AI-156.
5. **Logout (revocation)** - `POST /api/auth/logout` with the current
   refresh token; then call `/api/auth/refresh` again with it and show
   it is rejected.
6. **Role change (revocation)** - log in again to get a fresh session,
   then `PATCH /api/users/{id}/role` for that user; show their
   still-outstanding refresh token is now rejected by `/api/auth/refresh`.

### 4.3 Synthetic Data Generation Demonstration Flow

1. Start (or restart) the backend with `spring.profiles.active=synthetic-data`.
2. Point out the startup log running the seeder with no manual steps
   required.
3. Query the API (or DB) to show seeded rows across the full domain model:
   LRUs, Interfaces, Connectors, Signal Library/Range, Protocol Tags,
   Fault Templates, FMEA Branch Mappings, Projects, Aircraft, Subsystems,
   and Test Blocks.
4. Point out every seeded row is clearly labelled (`(Synthetic)` suffix,
   or a `SIM-`/`FLT-`/`FMEA-` prefixed code for join-style rows) so it can
   never be confused with real production data.
5. Restart the backend a second time; show the row counts are identical
   (idempotency) rather than doubling.

---

## 5. Presenter Notes

- Lead with the "why": this platform exists to make aerospace reliability
  engineering (FMEA, fault templates, test procedures) data-driven instead
  of spreadsheet-driven.
- If a live call fails during the demo, fall back to the dry-run recording
  rather than debugging live.
- Common Q&A to be ready for:
  - *"Is this real flight data?"* - No; every seeded row is synthetic and
    clearly labelled as such; see Section 4.3.
  - *"What happens to a user's other devices if I change their role?"* -
    Every active refresh-token session for that user is revoked; they'll
    need to log in again. That's intentional (see AI-156).
  - *"Why rotate refresh tokens instead of just letting them live for 7
    days?"* - Rotation means a stolen refresh token stops working the
    moment the legitimate client uses it again, closing the replay window.
- Keep the CRUD example small and visual (Projects or LRUs) rather than
  the deeper FMEA tables, which are harder to explain quickly on screen.

---

## 6. Verification Checklist (sign off before declaring the milestone done)

- [ ] AI-155: seeder covers the complete domain model, is idempotent, all
      synthetic rows are clearly labelled, and `SyntheticDataConstraintTest`
      + `SyntheticDataDomainCoverageTest` pass.
- [ ] AI-156: `/api/auth/refresh` rotates tokens and rejects reused
      (already-rotated) tokens; `/api/auth/logout` revokes the current
      session; `PATCH /api/users/{id}/role` revokes all of a user's active
      sessions; `RefreshTokenServiceTest`, `UserServiceTest`,
      `AuthServiceTest`, and `JwtServiceTest` all pass.
- [ ] AI-157: this document exists, is up to date, and every flow in
      Section 4 has been dry-run successfully within the last 48 hours.
- [ ] `mvn test` passes locally (or in CI) with no unrelated failures.
- [ ] No Week 1/2/3 functionality, package names, or API paths were
      changed.

---

## 7. Month 1 Milestone Readiness Checklist

- [ ] All Week 1-4 stories merged to `develop`.
- [ ] Backend builds (`mvn -q -DskipTests package`) with zero errors.
- [ ] Flyway migrations `V1` through `V9` apply cleanly against a fresh
      database.
- [ ] Frontend builds (`npm run build`) with zero errors.
- [ ] Demo dry run (Section 2) completed with no unresolved issues.
- [ ] Presenter has reviewed the demo script and presenter notes.
- [ ] Fallback recording captured in case of live environment issues.
- [ ] Stakeholder invite sent with the agenda from Section 3.
