# Finance Data Processing and Access Control Backend

A production-grade REST API backend for the Zorvyn FinTech finance dashboard. Built with Java 21 and Spring Boot 3.4.x, implementing layered architecture, JWT-based authentication, role-based access control, and database-level analytics aggregations.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Setup and Running Locally](#setup-and-running-locally)
- [API Reference](#api-reference)
- [Access Control Matrix](#access-control-matrix)
- [Assumptions and Design Decisions](#assumptions-and-design-decisions)
- [Testing](#testing)

---

## Tech Stack

| Concern            | Technology                              |
|--------------------|-----------------------------------------|
| Language           | Java 21 (virtual threads ready)         |
| Framework          | Spring Boot 3.4.1                       |
| Security           | Spring Security 6 + JWT (JJWT 0.12.3)  |
| Persistence        | Spring Data JPA + Hibernate 6           |
| Database           | PostgreSQL 15+                          |
| Build              | Maven 3.9+                             |
| Boilerplate        | Project Lombok                          |
| Validation         | Jakarta Bean Validation 3               |
| Testing            | JUnit 5 + Mockito + AssertJ             |

---

## Architecture

The application follows a strict **three-layer architecture** enforced via Spring stereotypes:

```
HTTP Request
    │
    ▼
@RestController          ← Input validation, auth context, HTTP semantics
    │  (DTOs only)
    ▼
@Service                 ← Business logic, orchestration, mapping
    │  (Entities + DTOs)
    ▼
@Repository              ← Data access, JPQL queries, specifications
    │
    ▼
PostgreSQL
```

### Key Architectural Decisions

**1. DTO Pattern — Never expose entities**

Controllers accept `RequestDTO` objects and return `ResponseDTO` objects. JPA entities are internal implementation details — they are never serialised to JSON. This decouples the database schema from the API contract, allowing either to evolve independently.

**2. Lombok for zero-boilerplate code**

- `@Data` / `@Getter` / `@Setter` eliminate manual accessors on DTOs and entities.
- `@Builder` provides safe, readable object construction throughout.
- `@Slf4j` injects a logger at compile time into every service and controller.
- `@RequiredArgsConstructor` replaces `@Autowired` field injection with constructor injection (Spring's recommended approach).

> **Note on entities vs DTOs:** `@Data` is used for DTOs (no relationships, no circular refs). JPA entities use individual annotations (`@Getter`, `@Setter`, `@Builder`, `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`) to avoid Lombok-generated `hashCode`/`equals` that include mutable fields or trigger lazy-load in `toString`.

**3. Global Exception Handling**

A single `@RestControllerAdvice` (`GlobalExceptionHandler`) catches all exceptions and maps them to a consistent `ApiResponse<T>` envelope:

```json
{
  "success": false,
  "error": "An account with email 'x@y.com' already exists.",
  "timestamp": "2024-03-15T10:30:00.000Z"
}
```

This prevents raw Spring error pages, stack traces, or inconsistent error formats from leaking to API consumers.

**4. Soft Deletes via `@SQLRestriction`**

Financial records are never physically deleted. Setting `deleted_at` to a timestamp makes the record invisible to all repository queries automatically, because `@SQLRestriction("deleted_at IS NULL")` instructs Hibernate to append that predicate to every generated SQL query for `FinancialRecord`. The data is preserved for auditing and compliance.

**5. Database-level aggregations**

All dashboard analytics (totals, group-by, monthly trends) are computed with JPQL `SUM`/`GROUP BY` queries in the repository layer. The service layer only handles in-memory result pivoting (e.g., collapsing two rows into one object). Fetching all records into Java and reducing with Streams would be an O(n) memory bottleneck — this approach is O(1) in application memory regardless of record count.

**6. Dynamic filtering with JPA Specifications**

The `GET /records` endpoint supports combining four optional filters (type, category, date range). Rather than writing one repository method per filter combination (2^4 = 16 methods), `FinancialRecordSpecification` provides composable `Specification<T>` predicates that Hibernate assembles into a single optimised SQL `WHERE` clause.

**7. JWT Stateless Authentication**

No server-side sessions are maintained. Every request carries a self-contained signed JWT token. The `JwtAuthenticationFilter` validates the token and populates the `SecurityContext` before the request reaches any controller. Tokens expire after 24 hours (configurable via `jwt.expiration`).

---

## Project Structure

```
src/
├── main/
│   ├── java/com/zorvyn/finance/
│   │   ├── FinanceApplication.java           ← Spring Boot entry point
│   │   ├── config/
│   │   │   └── SecurityConfig.java           ← Filter chain, RBAC, password encoder
│   │   ├── controller/
│   │   │   ├── AuthController.java           ← /api/v1/auth/**
│   │   │   ├── DashboardController.java      ← /api/v1/dashboard
│   │   │   ├── FinancialRecordController.java ← /api/v1/records/**
│   │   │   └── UserController.java           ← /api/v1/users/**
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   │   ├── FinancialRecord.java       ← Soft-delete entity with @SQLRestriction
│   │   │   │   └── User.java
│   │   │   └── enums/
│   │   │       ├── RoleType.java             ← VIEWER | ANALYST | ADMIN
│   │   │       ├── TransactionType.java      ← INCOME | EXPENSE
│   │   │       └── UserStatus.java           ← ACTIVE | INACTIVE
│   │   ├── dto/
│   │   │   ├── request/                      ← Validated inbound payloads
│   │   │   └── response/                     ← Outbound payloads + ApiResponse<T>
│   │   ├── exception/
│   │   │   ├── BadRequestException.java
│   │   │   ├── GlobalExceptionHandler.java   ← @RestControllerAdvice
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── UnauthorizedException.java
│   │   ├── repository/
│   │   │   ├── FinancialRecordRepository.java ← JPQL aggregations + JpaSpecificationExecutor
│   │   │   ├── UserRepository.java
│   │   │   └── projection/                   ← Interface projections for aggregation results
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.java  ← OncePerRequestFilter
│   │   │   ├── JwtTokenProvider.java         ← Token generation + validation
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── DashboardService.java
│   │   │   ├── FinancialRecordService.java
│   │   │   └── UserService.java
│   │   └── specification/
│   │       └── FinancialRecordSpecification.java ← Composable JPA Specifications
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/zorvyn/finance/service/
        ├── AuthServiceTest.java
        ├── DashboardServiceTest.java
        └── FinancialRecordServiceTest.java
```

---

## Setup and Running Locally

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 15+

### 1. Create the database

```sql
CREATE DATABASE finance_db;
CREATE USER finance_user WITH PASSWORD 'finance_pass';
GRANT ALL PRIVILEGES ON DATABASE finance_db TO finance_user;
```

### 2. Configure environment variables (or edit `application.yml`)

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=finance_db
export DB_USERNAME=finance_user
export DB_PASSWORD=finance_pass
export JWT_SECRET=Wm9ydnluRmluYW5jZUJhY2tlbmRTZWNyZXRLZXkyMDI0Rg==
export JWT_EXPIRATION_MS=86400000
```

> **Important:** The `JWT_SECRET` must be a Base64-encoded string representing at least 32 bytes. The default value above is provided for local development only — **replace it in any deployed environment**.

### 3. Build and run

```bash
cd Data-Processing-and-Access-Control-Backend

# Run tests
./mvnw test

# Start the application (schema auto-created via ddl-auto=update)
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 4. Create your first admin user

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "System Admin",
    "email": "admin@zorvyn.com",
    "password": "SecurePass@123",
    "role": "ADMIN"
  }'
```

Copy the `token` from the response and use it as `Authorization: Bearer <token>` on subsequent requests.

---

## API Reference

All responses follow the standard envelope:

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2024-03-15T10:30:00Z"
}
```

### Authentication — `/api/v1/auth`

| Method | Path      | Auth | Description                     |
|--------|-----------|------|---------------------------------|
| POST   | `/signup` | None | Register new account            |
| POST   | `/login`  | None | Login, receive JWT token        |

**Signup request body:**
```json
{
  "fullName": "Liril Chandrawanshi",
  "email": "liril@zorvyn.com",
  "password": "SecurePass@123",
  "role": "ANALYST"
}
```

**Login response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "user": { "id": 1, "email": "liril@zorvyn.com", "role": "ANALYST", "status": "ACTIVE" }
  }
}
```

---

### Financial Records — `/api/v1/records`

| Method | Path   | Roles          | Description                               |
|--------|--------|----------------|-------------------------------------------|
| POST   | `/`    | ADMIN          | Create a new financial record             |
| GET    | `/`    | ADMIN, ANALYST | List records (paginated + filterable)     |
| GET    | `/{id}`| ADMIN, ANALYST | Get a single record by ID                 |
| PUT    | `/{id}`| ADMIN          | Partially update a record                 |
| DELETE | `/{id}`| ADMIN          | Soft-delete a record                      |

**Create record request:**
```json
{
  "amount": 75000.00,
  "type": "INCOME",
  "category": "Salary",
  "transactionDate": "2024-03-01",
  "notes": "March monthly salary"
}
```

**List records with filters:**
```
GET /api/v1/records?type=EXPENSE&category=travel&from=2024-01-01&to=2024-03-31&page=0&size=20&sort=transactionDate,desc
```

---

### Dashboard — `/api/v1/dashboard`

| Method | Path | Roles                      | Description              |
|--------|------|----------------------------|--------------------------|
| GET    | `/`  | ADMIN, ANALYST, VIEWER     | Full dashboard summary   |

**Response:**
```json
{
  "success": true,
  "data": {
    "totalIncome": 150000.00,
    "totalExpenses": 87500.50,
    "netBalance": 62499.50,
    "categoryTotals": [
      { "category": "Salary",   "type": "INCOME",  "total": 150000.00 },
      { "category": "Rent",     "type": "EXPENSE", "total": 25000.00  }
    ],
    "monthlyTrends": [
      { "month": "2024-03", "income": 50000.00, "expense": 29166.83 },
      { "month": "2024-02", "income": 50000.00, "expense": 29166.83 }
    ],
    "recentActivity": [ ... ]
  }
}
```

---

### Users (Admin only) — `/api/v1/users`

| Method  | Path   | Roles | Description             |
|---------|--------|-------|-------------------------|
| GET     | `/`    | ADMIN | List all users          |
| GET     | `/{id}`| ADMIN | Get user by ID          |
| PATCH   | `/{id}`| ADMIN | Update role/status/name |

**Update user (promote to ANALYST):**
```json
{ "role": "ANALYST" }
```

**Deactivate a user:**
```json
{ "status": "INACTIVE" }
```

---

## Access Control Matrix

| Endpoint                   | VIEWER | ANALYST | ADMIN |
|----------------------------|--------|---------|-------|
| POST /auth/signup          | ✓      | ✓       | ✓     |
| POST /auth/login           | ✓      | ✓       | ✓     |
| GET  /dashboard            | ✓      | ✓       | ✓     |
| GET  /records              | ✗      | ✓       | ✓     |
| GET  /records/{id}         | ✗      | ✓       | ✓     |
| POST /records              | ✗      | ✗       | ✓     |
| PUT  /records/{id}         | ✗      | ✗       | ✓     |
| DELETE /records/{id}       | ✗      | ✗       | ✓     |
| GET  /users                | ✗      | ✗       | ✓     |
| PATCH /users/{id}          | ✗      | ✗       | ✓     |

---

## Assumptions

1. **Self-signup with role assignment**: For simplicity, the signup endpoint accepts an optional `role` field. In a real production system, this would be restricted — users would self-register as VIEWERs only, and an admin would promote them.

2. **Single-table user model**: Roles are stored as an enum column on the `users` table rather than a separate `roles` table. A separate table with many-to-many relationships would be more flexible but adds complexity that this scope does not require.

3. **PostgreSQL-specific trend query**: The monthly trend JPQL query uses `FUNCTION('to_char', ...)` which calls a PostgreSQL-specific function. The application is explicitly designed for PostgreSQL; portability to other databases is not a goal.

4. **User soft-delete**: Users are deactivated via `status = INACTIVE` rather than a timestamp-based soft delete. Deactivated users are rejected at login by Spring Security (the `enabled` flag on `UserDetails`) without being hidden from admin listings.

5. **No pagination on dashboard**: The dashboard returns at most 10 recent activity items and 12 months of trends. These are hardcoded constants; an enhancement would make them configurable query parameters.

6. **`ddl-auto: update`**: Hibernate auto-creates/updates the schema on startup. This is acceptable for development and demos. A production deployment should use **Flyway** or **Liquibase** with `ddl-auto: validate`.

---

## Testing

The test suite uses JUnit 5 + Mockito + AssertJ and covers core service logic with unit tests. All dependencies are mocked — no database or Spring context is loaded, making tests fast and isolated.

```bash
./mvnw test
```

**Test classes:**

| Class                        | Covers                                                              |
|------------------------------|---------------------------------------------------------------------|
| `FinancialRecordServiceTest` | Create, soft delete, partial update, not-found handling             |
| `AuthServiceTest`            | Duplicate email guard, default role, password encoding, bad credentials |
| `DashboardServiceTest`       | Net balance arithmetic, monthly trend pivot, empty-database edge case |
