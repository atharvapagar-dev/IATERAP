# IATERAP

> Aircraft Test Procedure Engineering & Fleet Reliability Analytics Platform  
> ADTECH Software Development Internship Project

---

# Overview

IATERAP is an enterprise-grade engineering platform being developed during the ADTECH Software Development Internship.

The platform is designed to assist aerospace engineering teams in creating, validating, approving and maintaining aircraft test procedures while providing fleet reliability analytics and AI-assisted engineering recommendations.

The system follows a modular full-stack architecture consisting of:

- Spring Boot Backend
- React Frontend
- PostgreSQL Database
- FastAPI AI Service
- Docker Deployment
- GitHub Version Control
- Jira Agile Project Management

Development follows Agile methodology with weekly sprint deliverables and industry-standard software engineering practices.

---

# Project Objectives

The project aims to:

- Build a scalable engineering management platform
- Standardize aircraft test procedure authoring
- Support engineering validation workflows
- Implement version-controlled approval pipelines
- Compute engineering coverage using deterministic rules
- Provide fleet reliability analytics
- Integrate AI-assisted engineering recommendations
- Deliver a production-ready containerized application

---

# Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Spring Security
- JWT Authentication
- Maven

---

## Frontend

- React
- Vite
- React Router
- Axios
- Recharts

---

## Database

- PostgreSQL
- Flyway

---

## AI Service

- Python
- FastAPI
- Pandas
- Scikit-learn

---

## DevOps

- Docker
- Docker Compose

---

## Tools

- Git
- GitHub
- Jira
- Postman
- VS Code
- IntelliJ IDEA

---

# Repository Structure

```
IATERAP
│
├── backend/
│
├── frontend/
│
├── database/
│
├── docker/
│
├── docs/
│
├── scripts/
│
├── .github/
│
└── README.md
```

---

# System Architecture

```
                React Frontend
                       │
                       │ REST API
                       ▼
             Spring Boot Backend
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
 PostgreSQL      FastAPI AI      Authentication
   Database        Service         (JWT/RBAC)
```

---

# Development Phases

## Phase 1

Foundation & Domain Model

- Environment setup
- Spring Boot project
- React project
- PostgreSQL
- Docker
- Domain schema

---

## Phase 2

Test Procedure Engineering

- Procedure Builder
- Test Step Engine
- Approval Workflow
- Version Management

---

## Phase 3

Rule Engine & Coverage Engine

- Engineering Rule Engine
- Coverage Scoring
- Heatmaps
- Gap Detection

---

## Phase 4

Fleet Reliability Analytics

- Fault Injection
- MTBF
- MTTR
- HFRA
- Pareto Analysis
- Trend Analysis
- AI Advisory

---

## Phase 5

Deployment & Finalization

- Docker
- CI/CD
- Security Hardening
- Documentation
- Testing
- Final Deployment

---

# Project Modules

## Engineering Domain

- Project
- Aircraft
- Subsystem
- LRU
- Interface
- Connector
- Signal
- Protocol Tag
- Fault Template
- FMEA

---

## Test Procedure Module

- Procedure Builder
- Test Steps
- Test Blocks
- Approval Workflow
- Version History

---

## Coverage Engine

- Rule Evaluation
- Coverage Score
- Heatmaps
- Gap Detection

---

## Reliability Analytics

- Fault Injection
- Event Processing
- MTBF
- MTTR
- HFRA
- Pareto Analysis
- Trend Analysis

---

## AI Advisory

- Engineering Suggestions
- Recommendation Engine
- Confidence Scoring

---

# Branch Strategy

```
main
│
└── develop
      │
      ├── feature/*
      ├── bugfix/*
      └── hotfix/*
```

---

# Git Workflow

1. Pull latest changes from `develop`
2. Create a feature branch
3. Implement the assigned Jira task
4. Commit using the project convention
5. Push the feature branch
6. Raise a Pull Request
7. Complete code review
8. Merge into `develop`

No direct commits are allowed to the `main` branch.

---

# Commit Message Convention

Examples

```
AI-9: Initialize Spring Boot backend project

AI-10: Add development standards documentation

AI-11: Initialize React frontend

AI-12: Configure PostgreSQL

AI-13: Configure Docker environment
```

---

# Coding Standards

- Follow SOLID principles.
- Follow package-by-feature architecture.
- Keep controllers lightweight.
- Business logic belongs in services.
- Use constructor injection.
- Use DTOs between layers.
- Validate request objects.
- Follow Java naming conventions.
- Write meaningful commit messages.
- One Jira task per feature branch.

---

# Development Workflow

```
Requirement

↓

Jira Task

↓

Feature Branch

↓

Development

↓

Testing

↓

Pull Request

↓

Code Review

↓

Merge into develop

↓

Release
```

---

# Current Sprint

## Sprint 1 – Foundation & Domain Model

Current objectives:

- Backend Project Initialization
- Frontend Project Initialization
- Database Setup
- Docker Environment
- Initial Documentation

---

# Team

## Mentor

Atharva Pagar

Responsibilities

- Architecture
- Project Setup
- Git Workflow
- Code Reviews
- Sprint Planning
- Documentation

---

## Interns

### Intern A

Engineering Data & Validation Track

Responsibilities

- Engineering Domain
- Rule Engine
- Coverage Engine
- AI Advisory Logic

---

### Intern B

Fleet Reliability, Platform Security & Infrastructure Track

Responsibilities

- Authentication
- Security
- Reliability Analytics
- Deployment
- Version Management

---

# Documentation

Project documentation is available under the `docs/` directory.

Future documentation includes:

- Architecture Documentation
- API Documentation
- Database Design
- Developer Guide
- Deployment Guide
- User Manual

---

# License

This repository is developed as part of the ADTECH Software Development Internship.

Distribution or commercial usage is subject to organizational guidelines.
