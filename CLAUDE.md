# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

**Build and compile:**
```bash
./gradlew build
```

**Run application:**
```bash
./gradlew bootRun
```

**Run tests:**
```bash
./gradlew test                    # All tests
./gradlew test --tests ClassName  # Single test class
```

**Database migrations:**
```bash
./gradlew update  # Apply Liquibase migrations
```

**Start local services:**
```bash
docker-compose up -d  # PostgreSQL and Redis
```

## Architecture Overview

### Feature-Based Structure

This codebase uses **feature-based architecture** where each feature module contains its own layers:

```
feature/
├── domain/      # JPA entities and domain models
├── data/        # Repositories (Spring Data JPA)
├── service/     # Business logic
├── controller/  # REST controllers
└── dto/         # Request/response objects
```

**CRITICAL:** When creating new features or modifying existing ones, always maintain this structure. Do NOT use layer-based organization (all controllers together, all services together, etc.).

### Module Organization

The project has **two main top-level modules**:

1. **`internal/`** - Internal control plane for Liyaqa team
   - `employee/` - Internal team member management with RBAC
   - `tenant/` - Customer organization (sports facility) management
   - `facility/` - Sport facility and branch management (managed by internal team)
   - `auth/` - Authentication and session management
   - `audit/` - Comprehensive audit logging for compliance
   - `shared/` - Cross-cutting concerns (config, security, exceptions, utilities)

2. **`facility/`** - Tenant-facing features (for sports facility employees)
   - `employee/` - Facility employee management (different from internal employees)
   - `membership/` - Member management and membership plans
   - `booking/` - Court/facility booking system
   - `trainer/` - Personal trainer booking system (see below)
   - More features to be added (schedules, payments, analytics, etc.)

3. **`api/`** - Public API for external integrations (see Public API section below)
   - `domain/` - API key management entities
   - `service/` - API key generation and validation
   - `security/` - Bearer token authentication filter
   - `controller/` - Public API endpoints (versioned as v1)
   - `dto/` - Public-facing request/response DTOs

**Key Distinction:**
- **Internal employees** (`internal/employee/`) = Liyaqa team members (support, sales, engineering)
- **Facility employees** (`facility/employee/`) = Sports facility staff (coaches, receptionists, managers)

Both have separate permission systems and authentication flows.

### Multi-Tenancy Pattern

**Row-Level Isolation:**
- Most entities have a `tenantId: String` field for tenant isolation
- Tenant context is managed via `TenantContext` and `TenantContextHolder`
- Internal team operates across all tenants; facility employees are scoped to their facility

**Key Entities:**
- `Tenant` - Customer organization (sports facility company)
- `SportFacility` - A facility owned by a tenant
- `FacilityBranch` - Physical location of a facility
- Facilities can have multiple branches; employees can be assigned to specific branches

## Database Conventions

### Liquibase Migrations

All schema changes go through Liquibase:

1. Create changeset file in `src/main/resources/db/changelog/`
2. Add reference to `db.changelog-master.xml`
3. Run `./gradlew update` to apply

**Changeset naming:**
- Use sequential IDs: `001-`, `002-`, etc.
- Author should be `liyaqa`
- Always add indexes for foreign keys and frequently queried columns

**Example:**
```xml
<changeSet id="016-add-branch-assignments" author="liyaqa">
    <createTable tableName="facility_employee_branches">
        <column name="employee_id" type="UUID"/>
        <column name="branch_id" type="UUID"/>
    </createTable>
    <addForeignKeyConstraint .../>
</changeSet>
```

**Recent Changesets:**
- **032**: Public API system - `api_keys` table for external integrations
- **033**: Personal trainer booking - 4 tables (trainers, trainer_availabilities, trainer_bookings, trainer_reviews)

### Entity Conventions

- All entities extend `BaseEntity` (provides `id`, `createdAt`, `updatedAt`)
- Use `@EntityListeners(AuditingEntityListener::class)` for timestamp management
- Multi-tenancy field: `var tenantId: String? = null`
- Lazy loading by default: `@ManyToOne(fetch = FetchType.LAZY)`
- Junction tables for many-to-many: `{entity1}_{entity2}_{relationship}`

## Security & Permissions

### Authentication Pattern

**Internal Team:**
- JWT-based authentication
- Endpoint: `POST /api/v1/internal/auth/login`
- Access + refresh token pattern
- Session tracking with Redis
- Account lockout after 5 failed attempts

**Facility Employees:**
- Separate authentication system
- Scoped to their facility
- UserDetails implementation in `FacilityEmployee` entity

### Permission System

**Internal employees** use group-based RBAC:
- 42 permissions defined in `Permission` enum
- Predefined groups in `PredefinedGroups`:
  - `Super Admin` - Full system access
  - `Support Agent` - Customer support
  - `Support Manager` - Team management
  - `Sales` - Deal and tenant creation
  - `Finance` - Payment processing

**Facility employees** have their own permission system:
- `FacilityPermission` enum (30+ permissions)
- `FacilityEmployeeGroup` for role grouping
- Permissions checked via `hasPermission(permission)` method

### Permission Checks in Services

**Pattern:**
```kotlin
private fun checkPermission(employee: Employee, permission: Permission) {
    if (!employee.hasPermission(permission)) {
        auditService.logUnauthorizedAccess(employee, ...)
        throw SecurityException("Insufficient permissions: ${permission.name} required")
    }
}
```

Always check permissions at the **service layer**, not just controllers.

## Public API System

The Public API (`api/` module) enables external integrations through secure API key authentication.

### API Key Authentication

**Key Format:**
- Production: `lyk_live_{random}`
- Test: `lyk_test_{random}`
- 64 characters total, cryptographically secure

**Security Model:**
- Keys are BCrypt hashed (only shown once at creation)
- Prefix-based lookup (`lyk_live_`, `lyk_test_`)
- Bearer token authentication
- Scope-based permissions (e.g., `facilities:read`, `bookings:write`)

**API Key Entity:**
```kotlin
class ApiKey(
    var keyPrefix: String,      // First 8 chars for lookup
    var keyHash: String,         // BCrypt hash
    var scopes: String,          // JSON array of permissions
    var rateLimitPerHour: Int,
    var environment: ApiKeyEnvironment  // LIVE or TEST
)
```

**Standard Scopes:**
- `facilities:read`, `facilities:write`
- `bookings:read`, `bookings:write`, `bookings:cancel`
- `members:read`, `members:write`
- `trainers:read`, `trainers:book`
- `payments:read`, `payments:process`

### Authentication Flow

**Endpoint Protection:**
All `/api/v1/public/*` endpoints require:
```
Authorization: Bearer lyk_live_xxxxxxxxxxxxx
```

**Filter Chain:**
1. `ApiKeyAuthenticationFilter` intercepts public API requests
2. Extracts Bearer token from Authorization header
3. Validates token via `ApiKeyService.validateApiKey()`
4. Checks key is active, not expired, and within rate limits
5. Sets Spring Security authentication with scopes as authorities

**Usage Tracking:**
- Every request increments usage counters
- Last used timestamp updated
- Rate limiting enforced (hourly and daily limits)

### Public API Endpoints

**Base Path:** `/api/v1/public`

Current endpoints:
- `GET /facilities` - List facilities
- `GET /facilities/{id}` - Get facility details
- `GET /facilities/{id}/branches` - List branches
- `POST /bookings` - Create booking (stub)

### API Key Management

**Creation (Internal Only):**
```kotlin
val (apiKey, rawKey) = apiKeyService.generateApiKey(
    tenantId = "tenant_xyz",
    name = "Mobile App Integration",
    scopes = listOf(ApiScopes.BOOKINGS_READ, ApiScopes.BOOKINGS_WRITE),
    environment = ApiKeyEnvironment.LIVE
)
// rawKey is only available here - must be securely stored by client
```

**Validation:**
```kotlin
val apiKey = apiKeyService.validateApiKey(rawKey)
if (apiKey != null && apiKey.hasScope(ApiScopes.BOOKINGS_WRITE)) {
    // Authorized
}
```

### Database Schema

**Table:** `api_keys` (Changeset 032)
- Comprehensive key management
- Usage tracking fields
- Rate limiting configuration
- Expiration support
- Environment isolation (live/test)

**PR Reference:** #16 - Public API v1

## Personal Trainer Booking System

The trainer booking system (`facility/trainer/` module) enables facilities to offer professional training services.

### Domain Models

**Trainer (`Trainer.kt`):**
- Personal and professional information
- Specializations, certifications, languages
- Flexible pricing (30/60/90 min sessions, hourly rate)
- Performance metrics (average rating, total sessions)
- Availability preferences (min notice hours, max advance days)
- Employment details (hire date, type, status)

**Trainer Availability (`TrainerAvailability.kt`):**
- **Regular schedules**: Weekly recurring (e.g., Monday 9am-5pm)
- **One-time blocks**: Special availability for specific dates
- **Time-off periods**: Block unavailable dates
- Conflict detection logic

**Trainer Booking (`TrainerBooking.kt`):**
- Complete booking lifecycle
- Session types: Personal, Semi-Private, Group, Assessment, Consultation
- Status flow: Pending → Confirmed → In Progress → Completed
- Check-in/check-out tracking
- Dynamic pricing based on duration
- Trainer notes and member performance ratings
- Cancellation with reason tracking

**Trainer Review (`TrainerReview.kt`):**
- Overall rating (1-5 stars, decimal precision)
- Specific ratings: professionalism, knowledge, communication, motivation
- Review moderation workflow (pending/approved/rejected/hidden)
- Trainer response capability
- Verified booking reviews
- Helpful vote tracking

### Key Features

**Conflict Detection:**
```kotlin
fun countConflictingBookings(
    trainerId: UUID,
    startTime: LocalDateTime,
    endTime: LocalDateTime
): Long
```
Prevents double-booking by checking overlapping sessions.

**Dynamic Pricing:**
```kotlin
fun getRateForDuration(durationMinutes: Int): BigDecimal? {
    return when (durationMinutes) {
        30 -> sessionRate30Min
        60 -> sessionRate60Min
        90 -> sessionRate90Min
        else -> hourlyRate?.multiply(BigDecimal(durationMinutes / 60.0))
    }
}
```

**Availability Management:**
- Regular weekly schedules with day-of-week and time ranges
- One-time availability for special sessions
- Time-off blocking for vacations/holidays
- Conflict detection between availability slots

### API Endpoints

**Member-Facing** (`/api/v1/member/trainers`):
```
GET    /                           - List available trainers
GET    /{trainerId}                - Get trainer details
POST   /{trainerId}/book           - Book training session
GET    /bookings                   - Get member's bookings
POST   /bookings/{bookingId}/cancel - Cancel booking
```

**Future Endpoints:**
- Admin endpoints for trainer management
- Trainer-facing endpoints for schedule management
- Review submission and moderation endpoints

### Booking Flow

1. **Browse Trainers**: Member lists available trainers with ratings
2. **Check Availability**: View trainer's schedule and available slots
3. **Create Booking**: Select time slot, session type, and special requests
4. **Validation**: System checks for conflicts and calculates price
5. **Confirmation**: Booking created with pending status
6. **Session**: Check-in when session starts, check-out when complete
7. **Completion**: Trainer adds notes, member can leave review

### Database Schema

**Tables** (Changeset 033):
1. `trainers` - 30+ columns for complete trainer profiles
2. `trainer_availabilities` - Schedule management
3. `trainer_bookings` - Booking lifecycle with full audit trail
4. `trainer_reviews` - Multi-dimensional ratings and moderation

**Indexes:**
- 15+ indexes for optimal query performance
- Facility and branch lookups
- Date range queries
- Rating calculations
- Tenant isolation

### Business Value

- **New Revenue Stream**: Monetize trainer services
- **Member Experience**: Easy booking and transparent pricing
- **Quality Control**: Review system and moderation
- **Operational Efficiency**: Automated conflict detection
- **Performance Tracking**: Session counts and ratings per trainer

**PR Reference:** #17 - Personal Trainer Booking System

## Code Patterns & Conventions

### DTO Mapping

Use **companion object `from()` methods** for entity-to-DTO mapping:

```kotlin
data class EmployeeResponse(...) {
    companion object {
        fun from(employee: Employee): EmployeeResponse {
            return EmployeeResponse(...)
        }
    }
}
```

Also maintain extension functions in `internal/shared/util/toResponse.kt` for convenience.

### Audit Logging

**Every state-changing operation must be audited:**

```kotlin
auditService.logCreate(employee, EntityType.TENANT, id, mapOf("name" to value))
auditService.logUpdate(employee, EntityType.FACILITY, id, changes)
auditService.logDelete(employee, EntityType.EMPLOYEE, id, metadata)
auditService.logUnauthorizedAccess(employee, action, entityType)
```

The audit log is **immutable** and used for compliance, security monitoring, and debugging.

### Repository Query Patterns

- Simple queries: Use Spring Data derived method names
- Complex queries: Use `@Query` with JPQL
- Always add `@Param` annotations for query parameters
- Pagination: Accept `Pageable` parameter, return `Page<T>`

**Example:**
```kotlin
@Query("""
    SELECT e FROM Employee e
    WHERE (:searchTerm IS NULL OR LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    AND (:status IS NULL OR e.status = :status)
""")
fun searchEmployees(
    @Param("searchTerm") searchTerm: String?,
    @Param("status") status: EmployeeStatus?,
    pageable: Pageable
): Page<Employee>
```

### Controller Conventions

- Base path: `/api/v1/{module}/{resource}`
  - Internal: `/api/v1/internal/{resource}`
  - Facility: `/api/v1/facility/{resource}`
- Use `@AuthenticationPrincipal` to inject current user
- Return `ResponseEntity<T>` with proper HTTP status codes
- `@Valid` for request validation
- Document endpoints with KDoc comments including example requests

### Error Handling

Custom exceptions in `internal/shared/exception/`:
- `EntityNotFoundException` - 404
- `SecurityException` - 403
- `ValidationException` - 400
- Others follow Spring Boot defaults

## System Initialization

**First-time setup:**

The system uses `BootstrapService` for initialization:

1. Check if system is initialized: `GET /api/v1/internal/system/init-status`
2. Create administrator (one-time only): `POST /api/v1/internal/system/initialize`
3. This creates predefined groups and the first super admin user

**System accounts:**
- Marked with `isSystemAccount = true`
- Have `employeeNumber` like "ADMIN-001"
- Cannot be deleted through normal flows

## Internationalization

- Default locale: `en_US`
- Arabic support built-in: `ar_SA`
- Locale stored per employee: `var locale: String = "en_US"`
- Timezone-aware: Use `Instant` for all timestamps
- Currency per facility: `var currency: String = "USD"`

## Testing Strategy

- Use **Testcontainers** for integration tests with real PostgreSQL
- MockK for Kotlin-friendly mocking
- Test security: Use `@WithMockUser` or Spring Security Test
- Test multi-tenancy: Always verify tenant isolation

## Git Workflow

- **NEVER commit directly to `main`**
- Always create feature branches: `feature/description`
- Conventional commits: `feat:`, `fix:`, `chore:`, `refactor:`
- Include Claude attribution in commits:
  ```
  🤖 Generated with [Claude Code](https://claude.com/claude-code)

  Co-Authored-By: Claude <noreply@anthropic.com>
  ```
- All changes must compile before committing
- Use `git mv` when moving files to preserve history
- Create comprehensive PRs with business context and technical details

### Recent Pull Requests

- **PR #16**: Public API v1 - External integration platform
- **PR #17**: Personal Trainer Booking System - New revenue stream

## Critical Development Rules

1. **Always maintain feature-based structure** - Don't reorganize into layers
2. **Check permissions in service layer** - Security checks belong in business logic
3. **Audit all state changes** - Compliance requirement, not optional
4. **Preserve multi-tenancy** - Always consider tenant isolation
5. **Use Liquibase** - Never modify schema directly
6. **Lazy loading by default** - Prevents N+1 queries
7. **Validate at DTO level** - Use Jakarta validation annotations
