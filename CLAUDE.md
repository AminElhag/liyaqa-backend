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
   - More features to be added (courts, bookings, schedules, etc.)

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
- Conventional commits preferred
- **NEVER include Claude attribution in commits**
- All changes must compile before committing
- Use `git mv` when moving files to preserve history

## Critical Development Rules

1. **Always maintain feature-based structure** - Don't reorganize into layers
2. **Check permissions in service layer** - Security checks belong in business logic
3. **Audit all state changes** - Compliance requirement, not optional
4. **Preserve multi-tenancy** - Always consider tenant isolation
5. **Use Liquibase** - Never modify schema directly
6. **Lazy loading by default** - Prevents N+1 queries
7. **Validate at DTO level** - Use Jakarta validation annotations
