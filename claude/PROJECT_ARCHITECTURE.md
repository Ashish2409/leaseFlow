# LeaseFlow - Architecture & Engineering Blueprint

# System Overview

LeaseFlow is a multi-tenant SaaS platform that manages the complete residential leasing lifecycle.

Primary workflow:

Prospect
→ Application
→ Approval
→ Lease Generation
→ Document Package
→ E-Signature
→ Active Lease
→ Renewal

---

# Architectural Style

Phase 1:

Modular Monolith

Reason:

* Faster development
* Easier debugging
* Easier deployment
* Lower operational cost

Future:

Microservice extraction based on domain boundaries.

---

# High-Level Modules

leaseflow
├── auth
├── tenant
├── property
├── prospect
├── application
├── resident
├── lease
├── template
├── document
├── signature
├── notification
├── audit
├── reporting
└── shared

Each module owns:

* Controller
* Service
* Repository
* DTOs
* Domain entities

No module should directly access another module's repository.

Communication occurs through services and domain events.

---

# Multi-Tenancy Design

Every business entity must contain:

tenant_id

Example:

properties
units
residents
leases
applications

Rules:

* Tenant A can never access Tenant B data.
* All queries filtered by tenant_id.
* Security layer enforces tenant isolation.

Future:

PostgreSQL Row-Level Security.

---

# Domain Model

## Tenant

Represents a property management company.

Fields:

id
name
subscription_plan
status
created_at

---

## User

Fields:

id
tenant_id
email
password_hash
status

Relationships:

User -> Roles

---

## Property

Fields:

id
tenant_id
name
address
city
state
zip_code

---

## Unit

Fields:

id
property_id
unit_number
bedrooms
bathrooms
monthly_rent
status

Status:

AVAILABLE
RESERVED
LEASED
MAINTENANCE

---

## Prospect

Fields:

id
tenant_id
first_name
last_name
email
phone

Status:

NEW
CONTACTED
TOUR_SCHEDULED
APPLIED
APPROVED
REJECTED

---

## Application

Fields:

id
tenant_id
prospect_id
status
submitted_at

Status:

SUBMITTED
UNDER_REVIEW
APPROVED
REJECTED

---

## Resident

Fields:

id
tenant_id
first_name
last_name
email
phone

---

## Lease

Fields:

id
tenant_id
resident_id
unit_id
lease_start
lease_end
monthly_rent
deposit

Status:

DRAFT
GENERATED
SENT
SIGNED
ACTIVE
EXPIRED
TERMINATED

---

## Lease Template

Fields:

id
tenant_id
name
version
content
active

---

## Lease Document

Fields:

id
lease_id
document_type
file_path
version

---

## Signature Request

Fields:

id
lease_document_id
status
sent_at
completed_at

Status:

PENDING
VIEWED
SIGNED
DECLINED

---

# PostgreSQL Schema

Core Tables:

tenants
users
roles
user_roles

properties
units

prospects
applications

residents

leases
lease_templates
lease_documents

signature_requests

notifications

audit_logs

---

# Package Structure

com.ashish.leaseflow

├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── property
├── prospect
├── resident
├── lease
├── template
├── document
├── notification
│
├── common
│   ├── config
│   ├── security
│   ├── exception
│   ├── util
│   └── audit

---

# Security Architecture

Authentication:

JWT Access Token

JWT Refresh Token

Authorization:

ROLE_PLATFORM_ADMIN
ROLE_PROPERTY_MANAGER
ROLE_LEASING_AGENT
ROLE_RESIDENT

Requirements:

* BCrypt hashing
* Token rotation
* Session revocation
* Login audit logs

---

# API Standards

Base URL:

/api/v1

Examples:

POST /api/v1/auth/login

POST /api/v1/properties

GET /api/v1/properties

POST /api/v1/leases

GET /api/v1/leases/{id}

---

# Error Response Standard

{
"timestamp": "2026-06-05T10:30:00Z",
"status": 404,
"error": "NOT_FOUND",
"message": "Lease not found",
"path": "/api/v1/leases/123"
}

---

# Audit Architecture

Track:

* Login
* Lease Creation
* Lease Modification
* Lease Signing
* Lease Renewal

Audit Fields:

id
tenant_id
entity_type
entity_id
action
performed_by
performed_at

---

# Document Generation Architecture

Template Storage:

Database

Rendering Engine:

Template
→ Variable Mapping
→ HTML
→ PDF

Flow:

Lease
→ Template Engine
→ HTML Renderer
→ PDF Generator
→ Storage Service

---

# Storage Architecture

Development:

Local Storage

Production:

AWS S3

Structure:

tenant-id/
property-id/
lease-id/
documents/

---

# Event-Driven Design

Internal Domain Events

Examples:

ApplicationApprovedEvent

LeaseGeneratedEvent

LeaseSignedEvent

ResidentCreatedEvent

Listeners:

Notification
Audit
Reporting

---

# Notification Architecture

Events trigger notifications.

Channels:

EMAIL
SMS
IN_APP

Future:

WhatsApp

---

# Testing Strategy

Unit Tests

Target:

80%+ service coverage

Tools:

JUnit 5
Mockito

---

# Integration Tests

Tools:

Testcontainers

Containers:

PostgreSQL

Tests:

Repository
Controller
Security

---

# CI/CD Pipeline

GitHub Actions

Pipeline:

Build
→ Unit Tests
→ Integration Tests
→ Sonar Analysis
→ Docker Build
→ Deploy

---

# Logging Standards

JSON Structured Logging

Fields:

timestamp
tenantId
userId
requestId
module
action

Use correlation IDs on every request.

---

# Production Readiness Checklist

* JWT Authentication
* Role-Based Access Control
* Global Exception Handling
* Validation
* Audit Logging
* Integration Testing
* API Documentation
* Dockerization
* CI/CD
* Observability

---

# Future Enhancements

V2

* E-Signature Integrations
* Credit Screening
* Background Checks
* Resident Portal
* Payment Processing
* Lease Renewals

V3

* AI Lease Assistant
* AI Compliance Validation
* AI Clause Suggestions
* Lease Risk Scoring
* Workflow Automation

---

# Success Criteria

A property manager should be able to:

1. Create Property
2. Create Unit
3. Add Resident
4. Create Lease
5. Generate Lease PDF
6. Send for Signature
7. Activate Lease

Without leaving LeaseFlow.
