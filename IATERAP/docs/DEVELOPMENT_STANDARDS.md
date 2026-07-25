# IATERAP – Development Standards

> **Project:** Intelligent Aircraft Test Engineering & Reliability Analysis Platform (IATERAP)

| Document Information | Details |
|---|---|
| Document Type | Engineering Standards |
| Version | 1.0 |
| Status | Approved |
| Prepared By | Project Management Team |
| Maintained By | Project Management Team |

---

## Revision History

| Version | Date | Description |
|---|---|---|
|1.0|06 July 2026|Initial Release|

---

# 1. Purpose

This document defines the mandatory engineering, development, documentation, Git, testing, and review standards for the IATERAP project. All contributors shall follow these standards.

# 2. Scope

Applies to backend, frontend, AI service, database, infrastructure, documentation and DevOps.

# 3. Engineering Principles

- SOLID
- Clean Code
- Separation of Concerns
- Package-by-Feature
- REST-first design
- Documentation-first
- Security by Design
- Testable software
- Reusable components

# 4. Technology Stack

| Layer | Technology |
|---|---|
|Backend|Spring Boot|
|Frontend|React|
|Database|PostgreSQL|
|AI|FastAPI|
|Migration|Flyway|
|Containers|Docker & Docker Compose|
|Version Control|Git & GitHub|
|Project Management|Jira|

# 5. Repository Structure

```text
IATERAP/
├── backend/
├── frontend/
├── ai-service/
├── database/
├── docker/
├── docs/
├── scripts/
├── postman/
└── .github/
```

# 6. Development Workflow

1. Select Jira Story.
2. Create branch from develop.
3. Implement feature.
4. Add/update tests.
5. Update documentation.
6. Commit using Conventional Commits.
7. Open Pull Request.
8. Peer review.
9. Merge into develop.
10. Mark Jira story Done.

# 7. Git Standards

## Branches

- main
- develop
- feature/ai-xx-name
- docs/ai-xx-name
- bugfix/ai-xx-name
- hotfix/ai-xx-name

## Commit Convention

```text
feat(module): description
fix(module): description
docs(ai-10): description
refactor(module): description
test(module): description
chore(module): description
```

## Pull Requests

Must include:
- Jira ID
- Summary
- Testing
- Reviewer
- Checklist

# 8. Code Review

Verify:
- Correctness
- Readability
- Naming
- Security
- Performance
- Tests
- Documentation

# 9. Coding Standards

## Java
- Constructor injection
- DTOs
- Layered architecture
- Validation
- Meaningful names
- No duplicated logic

## React
- Functional components
- Hooks
- Reusable components
- API abstraction

## Python
- PEP-8
- Type hints
- Modular routers
- Service separation

# 10. Database Standards

- Flyway migrations only.
- No manual schema edits.
- Foreign keys required.
- Consistent naming.

# 11. API Standards

- RESTful endpoints
- JSON payloads
- Proper HTTP status codes
- Input validation
- Standard error responses
- Version prefix `/api/v1`

# 12. Logging & Exception Handling

- Structured logging.
- No sensitive data in logs.
- Global exception handling.
- User-friendly error messages.

# 13. Security Standards

- JWT authentication.
- Never commit secrets.
- Use environment variables.
- Validate all input.

# 14. Testing Standards

- Unit tests
- Integration tests
- API tests
- Smoke tests

Failing tests block merges.

# 15. Documentation Standards

Update documentation whenever functionality changes.

Required documents include:
- README
- Development Standards
- Architecture
- API
- ADRs
- Sprint Reports
- Weekly Reports

# 16. Definition of Ready

A task is Ready when:
- Jira story exists
- Acceptance criteria defined
- Dependencies identified
- Assignee selected

# 17. Definition of Done

A task is Done when:
- Code complete
- Tests passing
- Documentation updated
- PR approved
- Merged into develop
- Jira updated

# 18. Repository Rules

- No direct commits to main.
- No direct commits to develop.
- Every change via PR.
- Every branch linked to Jira.
- Peer review mandatory.

# 19. References

- Project Proposal
- Implementation Blueprint
- PM Playbook
- Intern Execution Plans
