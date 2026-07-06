# IATERAP

## Project Overview

IATERAP is an AI-powered therapy and mental wellness platform being developed as part of the Software Development Internship.

The project follows Agile methodology with Jira for project management, GitHub for version control, and Spring Boot + React as the technology stack.

---

## Technology Stack

### Backend
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven

### Frontend
- React
- Node.js 22 LTS
- npm

### DevOps
- Docker
- Docker Compose

### Project Management
- Jira
- GitHub

---

## Repository Structure

```
IATERAP/
│
├── backend/
├── frontend/
├── database/
├── docker/
├── docs/
├── scripts/
├── .github/
└── README.md
```

---

## Git Workflow

```
main
│
└── develop
      │
      ├── feature/database-setup
      ├── feature/frontend-init
      ├── feature/docker-setup
      └── ...
```

Development is performed on feature branches.

No direct commits should be made to `main`.

---

## Branch Naming Convention

```
feature/<feature-name>

bugfix/<issue-name>

hotfix/<issue-name>
```

Examples

```
feature/database-setup

feature/frontend-project-init

feature/docker-environment
```

---

## Commit Message Convention

Examples

```
AI-9: Initialize Spring Boot backend project

AI-10: Add development standards documentation

AI-11: Initialize React frontend

AI-12: Configure PostgreSQL

AI-13: Add Docker environment
```

---

## Coding Standards

- Follow Java naming conventions.
- Keep business logic inside services.
- Controllers should remain thin.
- Use meaningful commit messages.
- One feature per branch.
- Raise Pull Requests to merge into `develop`.

---

## Current Sprint

Sprint 1 – Foundation & Domain Model
