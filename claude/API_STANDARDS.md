# LeaseFlow API Standards

# Versioning

All APIs must be versioned.

Base URL:

/api/v1

Example:

/api/v1/auth/login

---

# Naming Standards

Use plural resource names.

Correct:

/properties

/residents

/leases

Incorrect:

/property

/resident

/lease

---

# HTTP Methods

GET

Read

POST

Create

PUT

Full Update

PATCH

Partial Update

DELETE

Delete

---

# Authentication

JWT Bearer Token

Authorization:

Bearer <token>

---

# Response Format

Success:

{
"success": true,
"data": {},
"timestamp": "2026-06-06T10:00:00Z"
}

---

# Error Format

{
"success": false,
"error": {
"code": "LEASE_NOT_FOUND",
"message": "Lease not found"
},
"timestamp": "2026-06-06T10:00:00Z"
}

---

# Status Codes

200 OK

201 CREATED

204 NO_CONTENT

400 BAD_REQUEST

401 UNAUTHORIZED

403 FORBIDDEN

404 NOT_FOUND

409 CONFLICT

500 INTERNAL_SERVER_ERROR

---

# Pagination

Request:

GET /api/v1/properties?page=0&size=20

Response:

{
"content": [],
"page": 0,
"size": 20,
"totalElements": 100,
"totalPages": 5
}

---

# Sorting

GET /api/v1/properties?sort=name,asc

GET /api/v1/properties?sort=createdAt,desc

---

# Validation

Use Jakarta Validation.

Examples:

@NotBlank

@NotNull

@Email

@Size

---

# OpenAPI Documentation

Every endpoint must contain:

* Summary
* Description
* Response Codes
* Example Requests

---

# Idempotency

POST requests involving payments, signatures, or document generation should support idempotency keys.

Header:

Idempotency-Key

---

# Correlation ID

Every request should contain:

X-Correlation-ID

If absent, generate one.

Include it in logs.

---

# Audit Requirements

Audit the following actions:

* Login
* User Creation
* Property Creation
* Lease Creation
* Lease Modification
* Lease Signing

---

# Security Requirements

* No sensitive data in logs
* No passwords in responses
* JWT expiration required
* Refresh token rotation

---

# DTO Rules

Never expose entities directly.

Controller

↓

Request DTO

↓

Service

↓

Response DTO

---

# Exception Handling

Centralized Exception Handling only.

No controller-level try/catch blocks.

Use:

@RestControllerAdvice

for all exceptions.

---

# API Quality Standards

* Backward compatible
* Consistent naming
* Proper status codes
* Validation first
* Documented endpoints
* Unit tested
* Integration tested
