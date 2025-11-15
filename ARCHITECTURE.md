# Architecture Documentation

## Table of Contents

1. [System Overview](#system-overview)
2. [Architectural Principles](#architectural-principles)
3. [Module Organization](#module-organization)
4. [Design Patterns](#design-patterns)
5. [Multi-Tenancy Architecture](#multi-tenancy-architecture)
6. [Security Architecture](#security-architecture)
7. [Database Architecture](#database-architecture)
8. [Integration Architecture](#integration-architecture)
9. [Technology Stack](#technology-stack)
10. [Architectural Decision Records](#architectural-decision-records)

---

## System Overview

**Liyaqa** is a comprehensive multi-tenant SaaS platform designed for sports facility management. The system enables sports facilities to manage their operations including:

- **Court and resource booking**
- **Membership management**
- **Employee scheduling**
- **Personal trainer bookings**
- **Payment processing**
- **Member communications**
- **Analytics and reporting**

The platform is built using a **feature-based architecture** with clear separation between internal control plane operations and tenant-facing features.

### Key Characteristics

- **Multi-tenant:** Row-level tenant isolation with tenant context management
- **Scalable:** Built on Spring Boot with PostgreSQL and Redis for caching and sessions
- **Secure:** JWT-based authentication, comprehensive RBAC, and audit logging
- **Extensible:** Plugin-based notification system and payment gateway abstraction
- **Auditable:** Immutable audit log for all state-changing operations
- **Internationalized:** Built-in support for English and Arabic locales

---

## Architectural Principles

### 1. Feature-Based Architecture

Unlike traditional layer-based architectures (all controllers together, all services together), Liyaqa uses **feature-based organization** where each feature module contains its own layers:

```
feature/
├── domain/      # JPA entities, enums, domain models
├── data/        # Repositories (Spring Data JPA)
├── service/     # Business logic and orchestration
├── controller/  # REST API endpoints
└── dto/         # Request/response data transfer objects
```

**Benefits:**
- **High cohesion:** Related code stays together
- **Low coupling:** Features are independent and can evolve separately
- **Team scalability:** Teams can own entire features
- **Easier navigation:** Find all code for a feature in one place

### 2. Domain-Driven Design (DDD)

The system follows DDD principles with rich domain models:

- **Entities:** Rich domain objects with business behavior, not anemic data holders
- **Value Objects:** Immutable objects like `Money`, `Address`
- **Aggregates:** Tenant, SportFacility, Booking are aggregate roots
- **Domain Events:** State changes are logged as audit events
- **Bounded Contexts:** Clear boundaries between internal and facility modules

**Example:**
```kotlin
// Rich domain model with business methods
class Tenant : BaseEntity() {
    fun suspend(reason: String, suspendedBy: Employee) {
        this.status = TenantStatus.SUSPENDED
        this.suspendedAt = Instant.now()
        this.suspendedBy = suspendedBy
        this.suspensionReason = reason
    }

    fun canBookCourts(): Boolean {
        return status == TenantStatus.ACTIVE && !isOverQuota()
    }
}
```

### 3. Security by Default

Security is not an afterthought but a core architectural principle:

- **Permission checks at service layer:** Never rely solely on controller annotations
- **Audit all state changes:** Immutable audit log for compliance
- **Fail-safe defaults:** Deny by default, explicit grants only
- **Defense in depth:** Multiple layers of security (network, application, data)

### 4. Separation of Concerns

Clear separation between:

- **Internal operations** (`/internal/*`) - For Liyaqa team
- **Tenant operations** (`/facility/*`) - For sports facility employees
- **Public API** (`/api/v1/public/*`) - For external integrations
- **Shared infrastructure** (`/shared/*`) - Cross-cutting concerns

### 5. Immutability Where Possible

- **Audit logs:** Immutable event stream
- **DTOs:** Immutable data classes
- **Value objects:** Always immutable
- **Configuration:** Environment-based, immutable at runtime

---

## Module Organization

### Top-Level Module Structure

```
src/main/kotlin/com/liyaqa/
├── internal/           # Internal control plane (Liyaqa team)
│   ├── employee/       # Internal employee management + RBAC
│   ├── tenant/         # Customer organization lifecycle
│   ├── facility/       # Sport facility & branch management
│   ├── audit/          # Audit logging system
│   ├── auth/           # JWT authentication
│   └── shared/         # Config, security, exceptions, utils
│
├── facility/           # Tenant-facing features (facility employees)
│   ├── employee/       # Facility employee management
│   ├── booking/        # Court booking system
│   ├── membership/     # Member & membership management
│   ├── trainer/        # Personal trainer bookings
│   └── auth/           # Member authentication
│
├── shared/             # Cross-cutting concerns
│   ├── analytics/      # Analytics & reporting
│   └── notification/   # Multi-channel notifications
│
├── api/                # Public API v1
│   └── v1/             # Public endpoints with API key auth
│
├── payment/            # Payment processing
│   ├── gateway/        # Gateway abstraction (Stripe, etc.)
│   └── webhook/        # Payment webhooks
│
└── core/               # Foundation
    ├── base/           # BaseEntity, base classes
    ├── config/         # Core configuration
    └── multitenancy/   # Tenant context management
```

### Module Descriptions

#### Internal Module (`/internal/*`)

**Purpose:** Internal control plane for Liyaqa team to manage the platform.

**Key Features:**
- **Employee Management:** Internal team members with hierarchical permissions
- **Tenant Lifecycle:** Onboarding, activation, suspension, termination
- **Facility Management:** Create and configure sports facilities
- **Audit System:** Comprehensive audit trail for all operations
- **Authentication:** JWT-based auth with session management

**Access Control:**
- Internal employees only
- 42 permissions across 5 predefined groups (Super Admin, Support Agent, Support Manager, Sales, Finance)

#### Facility Module (`/facility/*`)

**Purpose:** Tenant-facing features for sports facility operations.

**Key Features:**
- **Employee Management:** Facility staff (coaches, receptionists, managers)
- **Booking System:** Court reservations, availability, conflicts
- **Membership Management:** Member profiles, plans, subscriptions
- **Personal Trainers:** Trainer profiles, availability, bookings, reviews
- **Member Authentication:** Separate auth system for facility members

**Access Control:**
- Facility employees with tenant-scoped permissions
- 30+ facility-specific permissions

#### Shared Module (`/shared/*`)

**Purpose:** Cross-cutting concerns used by multiple modules.

**Key Features:**
- **Analytics:** Metrics collection and reporting
- **Notifications:** Email, SMS, push notifications, in-app messages

#### API Module (`/api/v1/public/*`)

**Purpose:** Public API for external integrations.

**Key Features:**
- Public booking endpoints
- Facility information lookup
- API key authentication
- Rate limiting

#### Payment Module (`/payment/*`)

**Purpose:** Payment processing and gateway integration.

**Key Features:**
- Gateway abstraction (currently Stripe)
- Transaction tracking
- Webhook handling
- Refund processing

---

## Design Patterns

### 1. Repository Pattern

All data access goes through Spring Data JPA repositories:

```kotlin
interface TenantRepository : JpaRepository<Tenant, UUID> {
    @Query("""
        SELECT t FROM Tenant t
        WHERE (:status IS NULL OR t.status = :status)
        AND (:searchTerm IS NULL OR
             LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """)
    fun search(
        @Param("status") status: TenantStatus?,
        @Param("searchTerm") searchTerm: String?,
        pageable: Pageable
    ): Page<Tenant>
}
```

**Benefits:**
- Abstraction over data access
- Testable with mocks
- Leverage Spring Data magic methods
- Type-safe queries with JPQL

### 2. Service Layer Pattern

Business logic resides in service classes, not controllers:

```kotlin
@Service
class TenantService(
    private val tenantRepository: TenantRepository,
    private val auditService: AuditService
) {
    @Transactional
    fun createTenant(request: CreateTenantRequest, createdBy: Employee): Tenant {
        // 1. Check permissions
        checkPermission(createdBy, Permission.TENANT_CREATE)

        // 2. Validate business rules
        validateBusinessRules(request)

        // 3. Create entity
        val tenant = Tenant(...)

        // 4. Persist
        val saved = tenantRepository.save(tenant)

        // 5. Audit
        auditService.logCreate(createdBy, EntityType.TENANT, saved.id)

        return saved
    }
}
```

**Benefits:**
- Testable business logic
- Transaction boundaries
- Permission checks in one place
- Audit logging consistency

### 3. DTO Pattern

Data Transfer Objects decouple API from domain:

```kotlin
// Request DTO
data class CreateTenantRequest(
    @field:NotBlank val name: String,
    @field:Email val email: String,
    // ... other fields
)

// Response DTO
data class TenantResponse(
    val id: UUID,
    val name: String,
    val status: TenantStatus,
    // ... other fields
) {
    companion object {
        fun from(tenant: Tenant): TenantResponse {
            return TenantResponse(
                id = tenant.id!!,
                name = tenant.name,
                status = tenant.status,
                // ... map other fields
            )
        }
    }
}
```

**Benefits:**
- API versioning flexibility
- Hide internal domain complexity
- Validation at API boundary
- Prevent over-fetching

### 4. Strategy Pattern

Used for pluggable implementations:

```kotlin
// Payment gateway abstraction
interface PaymentGateway {
    fun createCharge(amount: Money, customerId: String): ChargeResult
    fun refund(chargeId: String, amount: Money): RefundResult
}

// Stripe implementation
@Service
class StripePaymentGateway : PaymentGateway {
    override fun createCharge(amount: Money, customerId: String): ChargeResult {
        // Stripe-specific implementation
    }
}
```

**Benefits:**
- Easy to swap implementations
- Testable with mocks
- Extensible to new gateways

### 5. Builder Pattern

Used for complex object construction:

```kotlin
class Booking {
    class Builder {
        fun withMember(member: Member) = apply { this.member = member }
        fun withCourt(court: Court) = apply { this.court = court }
        fun withTimeSlot(start: Instant, end: Instant) = apply { ... }
        fun build(): Booking = Booking(...)
    }
}
```

### 6. Template Method Pattern

Base classes define algorithm structure:

```kotlin
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @CreatedDate
    var createdAt: Instant? = null

    @LastModifiedDate
    var updatedAt: Instant? = null

    var tenantId: String? = null
}
```

---

## Multi-Tenancy Architecture

### Tenant Isolation Strategy

Liyaqa uses **row-level tenant isolation** with a tenant context holder pattern:

```
┌─────────────────────────────────────────────────┐
│              HTTP Request                        │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│         TenantContextFilter                      │
│   (Extracts tenantId from JWT/context)          │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│         TenantContextHolder                      │
│   ThreadLocal<String> tenantId                   │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│         Service Layer                            │
│   (Automatically uses tenant context)            │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│         Database Query                           │
│   WHERE tenantId = :currentTenantId              │
└─────────────────────────────────────────────────┘
```

### Tenant Context Management

```kotlin
object TenantContextHolder {
    private val tenantContext = ThreadLocal<String>()

    fun setTenantId(tenantId: String) {
        tenantContext.set(tenantId)
    }

    fun getTenantId(): String? {
        return tenantContext.get()
    }

    fun clear() {
        tenantContext.remove()
    }
}
```

### Entity-Level Tenant Isolation

All tenant-scoped entities include `tenantId`:

```kotlin
@Entity
@Table(
    name = "bookings",
    indexes = [
        Index(name = "idx_booking_tenant", columnList = "tenant_id"),
        Index(name = "idx_booking_member", columnList = "member_id, tenant_id")
    ]
)
class Booking : BaseEntity() {
    // Inherited from BaseEntity:
    // var tenantId: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    var member: Member? = null

    @ManyToOne(fetch = FetchType.LAZY)
    var court: Court? = null
}
```

### Cross-Tenant Operations

Only **internal employees** can operate across tenants:

```kotlin
@Service
class TenantService {
    fun getAllTenants(requestedBy: Employee): List<Tenant> {
        // Internal employees can see all tenants
        if (requestedBy.isInternal) {
            return tenantRepository.findAll()
        }

        // Facility employees can only see their tenant
        val tenantId = TenantContextHolder.getTenantId()
        return listOf(tenantRepository.findById(tenantId).orElseThrow())
    }
}
```

### Tenant Lifecycle States

```
┌──────────┐      onboard()      ┌──────────┐
│ PENDING  ├────────────────────►│  ACTIVE  │
└──────────┘                     └─────┬────┘
                                       │
                              suspend()│
                                       │
                                       ▼
┌──────────┐   terminate()    ┌──────────────┐
│ ARCHIVED │◄─────────────────┤  SUSPENDED   │
└──────────┘                  └──────────────┘
                                      │
                                      │ activate()
                                      ▼
                              ┌──────────┐
                              │  ACTIVE  │
                              └──────────┘
```

---

## Security Architecture

### Authentication Flow

#### Internal Employee Authentication

```
┌────────┐           ┌──────────┐          ┌─────────┐          ┌───────┐
│ Client │           │   API    │          │ Service │          │ Redis │
└───┬────┘           └────┬─────┘          └────┬────┘          └───┬───┘
    │                     │                     │                   │
    │ POST /auth/login    │                     │                   │
    ├────────────────────►│                     │                   │
    │  email + password   │                     │                   │
    │                     │  authenticate()     │                   │
    │                     ├────────────────────►│                   │
    │                     │                     │                   │
    │                     │  validate password  │                   │
    │                     │◄────────────────────┤                   │
    │                     │                     │                   │
    │                     │                     │ store session     │
    │                     │                     ├──────────────────►│
    │                     │                     │                   │
    │                     │ JWT tokens          │                   │
    │◄────────────────────┤                     │                   │
    │ access + refresh    │                     │                   │
    │                     │                     │                   │
```

**Key Components:**
- **Password:** BCrypt hashed (strength 12)
- **Access Token:** JWT with 15-minute expiry
- **Refresh Token:** JWT with 7-day expiry
- **Session:** Stored in Redis with user metadata
- **Account Lockout:** 5 failed attempts = 30-minute lockout

#### Facility Member Authentication

Separate authentication system for members using facilities:

```kotlin
@Entity
class Member : BaseEntity(), UserDetails {
    var email: String = ""
    var passwordHash: String = ""
    var phoneNumber: String? = null
    var status: MemberStatus = MemberStatus.ACTIVE

    // UserDetails implementation
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
    }
}
```

### Authorization Architecture

#### Permission-Based Access Control (PBAC)

The system uses fine-grained permissions rather than simple roles:

```kotlin
enum class Permission {
    // Employee management
    EMPLOYEE_CREATE,
    EMPLOYEE_VIEW,
    EMPLOYEE_UPDATE,
    EMPLOYEE_DELETE,
    EMPLOYEE_MANAGE_PERMISSIONS,

    // Tenant management
    TENANT_CREATE,
    TENANT_VIEW,
    TENANT_UPDATE,
    TENANT_SUSPEND,
    TENANT_TERMINATE,

    // ... 42 total permissions
}
```

#### Permission Groups

Predefined groups bundle permissions:

```kotlin
object PredefinedGroups {
    val SUPER_ADMIN = PermissionGroup(
        name = "Super Admin",
        permissions = Permission.values().toSet() // All permissions
    )

    val SUPPORT_AGENT = PermissionGroup(
        name = "Support Agent",
        permissions = setOf(
            Permission.TENANT_VIEW,
            Permission.FACILITY_VIEW,
            Permission.EMPLOYEE_VIEW,
            // ... support-specific permissions
        )
    )
}
```

#### Permission Checking Pattern

**At Service Layer:**
```kotlin
private fun checkPermission(employee: Employee, permission: Permission) {
    if (!employee.hasPermission(permission)) {
        auditService.logUnauthorizedAccess(
            employee = employee,
            action = "Attempted operation requiring ${permission.name}",
            entityType = EntityType.EMPLOYEE
        )
        throw SecurityException("Insufficient permissions: ${permission.name} required")
    }
}
```

**Entity Method:**
```kotlin
class Employee : BaseEntity() {
    @ManyToMany(fetch = FetchType.EAGER)
    var groups: MutableSet<PermissionGroup> = mutableSetOf()

    fun hasPermission(permission: Permission): Boolean {
        return groups.any { it.permissions.contains(permission) }
    }
}
```

### Audit Logging Architecture

**Immutable audit trail** for compliance and security monitoring:

```kotlin
@Entity
@Table(
    name = "audit_logs",
    indexes = [
        Index(name = "idx_audit_employee", columnList = "employee_id"),
        Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
    ]
)
class AuditLog(
    @Enumerated(EnumType.STRING)
    var action: AuditAction,  // CREATE, UPDATE, DELETE, ACCESS, etc.

    var employeeId: UUID,
    var employeeEmail: String,

    @Enumerated(EnumType.STRING)
    var entityType: EntityType,

    var entityId: UUID?,

    @Column(columnDefinition = "jsonb")
    var metadata: String,  // JSON metadata about the change

    var timestamp: Instant = Instant.now(),
    var ipAddress: String?,
    var userAgent: String?
)
```

**Audit Service Usage:**
```kotlin
// Log creation
auditService.logCreate(
    employee = currentEmployee,
    entityType = EntityType.TENANT,
    entityId = tenant.id,
    metadata = mapOf("name" to tenant.name, "plan" to tenant.plan)
)

// Log updates with change tracking
auditService.logUpdate(
    employee = currentEmployee,
    entityType = EntityType.BOOKING,
    entityId = booking.id,
    changes = mapOf(
        "status" to mapOf("old" to "PENDING", "new" to "CONFIRMED")
    )
)

// Log unauthorized access attempts
auditService.logUnauthorizedAccess(
    employee = currentEmployee,
    action = "Attempted to delete facility",
    entityType = EntityType.FACILITY,
    entityId = facilityId
)
```

---

## Database Architecture

### Technology Stack

- **Database:** PostgreSQL 15+
- **Migration Tool:** Liquibase
- **ORM:** Hibernate (via Spring Data JPA)
- **Connection Pooling:** HikariCP

### Schema Design Principles

1. **UUID Primary Keys:** All entities use UUID for distributed scalability
2. **Audit Fields:** All tables have `created_at`, `updated_at`, `version` (optimistic locking)
3. **Tenant Isolation:** `tenant_id` column on all tenant-scoped tables
4. **Soft Deletes:** Some entities support soft deletion with `deleted_at` timestamp
5. **Comprehensive Indexing:** Foreign keys, tenant isolation, and query patterns

### Entity Relationship Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      INTERNAL MODULE                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────┐         ┌─────────────┐        ┌──────────────┐  │
│  │ Employee │────────►│   Groups    │───────►│ Permissions  │  │
│  └────┬─────┘         └─────────────┘        └──────────────┘  │
│       │                                                          │
│       │ manages                                                  │
│       ▼                                                          │
│  ┌──────────┐         ┌──────────────┐       ┌──────────────┐  │
│  │  Tenant  │────────►│SportFacility │──────►│FacilityBranch│  │
│  └──────────┘         └──────────────┘       └──────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     FACILITY MODULE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────────────┐       ┌──────────┐      ┌─────────────┐ │
│  │FacilityEmployee   │──────►│  Court   │      │   Member    │ │
│  └───────────────────┘       └────┬─────┘      └──────┬──────┘ │
│                                   │                    │         │
│                                   │ booked for         │         │
│                                   ▼                    │         │
│                              ┌──────────┐              │         │
│                              │ Booking  │◄─────────────┘         │
│                              └──────────┘                        │
│                                                                  │
│  ┌───────────┐       ┌────────────────┐      ┌──────────────┐  │
│  │  Member   │──────►│  Membership    │      │    Trainer   │  │
│  └───────────┘       └────────────────┘      └──────┬───────┘  │
│       │                                              │           │
│       │                                              │           │
│       └─────────────► TrainerBooking ◄──────────────┘           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    SHARED/PAYMENT MODULES                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐       ┌─────────────┐      ┌──────────────┐  │
│  │ Notification │       │ Transaction │      │  AuditLog    │  │
│  └──────────────┘       └─────────────┘      └──────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Migration Strategy

**Liquibase changesets:**
- Sequential IDs: `001-initial-schema.xml`, `002-add-bookings.xml`, etc.
- All changesets authored by `liyaqa`
- Rollback strategies where applicable
- Comprehensive commenting

**Example changeset:**
```xml
<changeSet id="016-add-facility-employee-branches" author="liyaqa">
    <comment>Add many-to-many relationship for employee branch assignments</comment>

    <createTable tableName="facility_employee_branches">
        <column name="employee_id" type="UUID">
            <constraints nullable="false"/>
        </column>
        <column name="branch_id" type="UUID">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <addPrimaryKey
        tableName="facility_employee_branches"
        columnNames="employee_id, branch_id"
        constraintName="pk_facility_employee_branches"/>

    <addForeignKeyConstraint
        baseTableName="facility_employee_branches"
        baseColumnNames="employee_id"
        constraintName="fk_feb_employee"
        referencedTableName="facility_employees"
        referencedColumnNames="id"
        onDelete="CASCADE"/>

    <addForeignKeyConstraint
        baseTableName="facility_employee_branches"
        baseColumnNames="branch_id"
        constraintName="fk_feb_branch"
        referencedTableName="facility_branches"
        referencedColumnNames="id"
        onDelete="CASCADE"/>

    <createIndex tableName="facility_employee_branches" indexName="idx_feb_employee">
        <column name="employee_id"/>
    </createIndex>

    <createIndex tableName="facility_employee_branches" indexName="idx_feb_branch">
        <column name="branch_id"/>
    </createIndex>
</changeSet>
```

### Query Performance Considerations

1. **Lazy Loading:** Default fetch strategy to prevent N+1 queries
2. **Indexed Columns:** All foreign keys and frequently queried fields
3. **Pagination:** All list endpoints support pagination
4. **Query Optimization:** Use `@EntityGraph` for controlled eager fetching
5. **Database Indexes:** Strategic indexes on multi-column queries

---

## Integration Architecture

### Payment Gateway Integration

**Abstract interface:**
```kotlin
interface PaymentGateway {
    fun createPaymentIntent(amount: Money, metadata: Map<String, String>): PaymentIntent
    fun capturePayment(intentId: String): PaymentResult
    fun refundPayment(chargeId: String, amount: Money): RefundResult
    fun retrieveCustomer(customerId: String): Customer
}
```

**Stripe implementation:**
- Webhook verification with signature checking
- Idempotent payment processing
- Transaction state tracking

### Notification System

**Multi-channel architecture:**

```
┌──────────────┐
│   Service    │
└──────┬───────┘
       │
       │ send notification
       ▼
┌──────────────────┐
│NotificationService│
└──────┬───────────┘
       │
       │ route based on channel
       ▼
┌──────────────────────────────────────┐
│         Channel Handlers              │
├──────────────────────────────────────┤
│ ┌────────┐ ┌─────┐ ┌──────┐ ┌──────┐│
│ │ Email  │ │ SMS │ │ Push │ │InApp ││
│ └────────┘ └─────┘ └──────┘ └──────┘│
└──────────────────────────────────────┘
```

**Notification types:**
- **Email:** SMTP with template engine
- **SMS:** Third-party provider integration
- **Push:** Firebase Cloud Messaging
- **In-App:** Database-backed notification center

### External API

**Public API v1:**
- API key authentication
- Rate limiting (configurable per key)
- Public endpoints for bookings, facility info
- Separate from internal/facility APIs

---

## Technology Stack

### Backend

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 1.9+ |
| Framework | Spring Boot | 3.5.7 |
| JVM | Java | 17+ |
| Build Tool | Gradle | 8.x |

### Data Layer

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Database | PostgreSQL | 15+ |
| ORM | Hibernate / Spring Data JPA | Object-relational mapping |
| Migration | Liquibase | Schema versioning |
| Connection Pool | HikariCP | Connection pooling |
| Cache | Redis | Session storage, caching |

### Security

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Authentication | JWT | Token-based auth |
| Password Hashing | BCrypt | Secure password storage |
| Session Management | Redis | Distributed sessions |
| API Security | Spring Security | Authentication & authorization |

### Infrastructure

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Containerization | Docker | Application packaging |
| Orchestration | Docker Compose | Local development |
| Payment | Stripe | Payment processing |
| Notifications | SMTP, SMS provider | Multi-channel messaging |

---

## Architectural Decision Records

### ADR-001: Feature-Based Architecture

**Status:** Accepted

**Context:**
Traditional layer-based architectures (all controllers together, all services together) lead to:
- Poor cohesion: Related code scattered across packages
- High coupling: Changes ripple across layers
- Team conflicts: Multiple teams editing same packages

**Decision:**
Adopt feature-based architecture where each feature module contains its own layers (domain, data, service, controller, dto).

**Consequences:**
- **Positive:** High cohesion, low coupling, team autonomy, easier navigation
- **Negative:** Some code duplication, need for shared utilities
- **Mitigation:** Use `/shared/` module for cross-cutting concerns

---

### ADR-002: Row-Level Multi-Tenancy

**Status:** Accepted

**Context:**
Multi-tenancy can be implemented via:
1. Separate databases per tenant (highest isolation, complex management)
2. Separate schemas per tenant (good isolation, schema proliferation)
3. Row-level isolation with `tenant_id` (shared schema, app-level isolation)

**Decision:**
Implement row-level multi-tenancy with `tenant_id` on all tenant-scoped entities.

**Consequences:**
- **Positive:** Simple deployment, easy tenant provisioning, cost-effective
- **Negative:** Risk of tenant data leakage if filtering fails
- **Mitigation:** Comprehensive testing, audit logging, tenant context filters

---

### ADR-003: JWT for Authentication

**Status:** Accepted

**Context:**
Need stateless authentication for:
- Horizontal scalability
- Mobile app support
- API access

**Decision:**
Use JWT with short-lived access tokens (15 min) and longer-lived refresh tokens (7 days).

**Consequences:**
- **Positive:** Stateless, scalable, works well with SPAs/mobile
- **Negative:** Cannot invalidate tokens before expiry
- **Mitigation:** Short expiry + Redis session tracking for critical operations

---

### ADR-004: Permission-Based Access Control

**Status:** Accepted

**Context:**
Simple role-based access (RBAC) is too coarse-grained for complex business requirements.

**Decision:**
Implement permission-based access control (PBAC) with 42 fine-grained permissions grouped into roles.

**Consequences:**
- **Positive:** Flexible, granular control, easier to audit
- **Negative:** More complex than simple RBAC
- **Mitigation:** Predefined groups for common roles, clear documentation

---

### ADR-005: Immutable Audit Log

**Status:** Accepted

**Context:**
Need comprehensive audit trail for:
- Compliance (data protection regulations)
- Security monitoring
- Debugging and support

**Decision:**
Implement immutable audit log with all state-changing operations logged.

**Consequences:**
- **Positive:** Full audit trail, tamper-proof, valuable for compliance
- **Negative:** Storage overhead, performance impact
- **Mitigation:** Async logging, log archival strategy, indexed queries

---

### ADR-006: Liquibase for Schema Migrations

**Status:** Accepted

**Context:**
Need reliable, version-controlled database schema management across environments.

**Decision:**
Use Liquibase with XML changesets for all schema changes.

**Consequences:**
- **Positive:** Version control, rollback support, environment consistency
- **Negative:** Learning curve, verbose XML
- **Mitigation:** Clear naming conventions, comprehensive documentation

---

## Future Architectural Considerations

### Scalability

**Current State:** Monolithic application

**Future Considerations:**
- **Microservices:** Split internal vs facility modules if needed
- **CQRS:** Separate read/write models for complex queries
- **Event Sourcing:** Consider for audit-heavy domains
- **Caching Layer:** Redis caching for frequently accessed data

### Performance

**Potential Optimizations:**
- **Read Replicas:** Separate read/write database connections
- **Database Sharding:** Shard by tenant for very large deployments
- **CDN:** Static asset delivery
- **Search:** Elasticsearch for full-text search

### Observability

**Enhancements Needed:**
- **Metrics:** Prometheus + Grafana
- **Tracing:** Distributed tracing (Jaeger/Zipkin)
- **Logging:** Centralized logging (ELK stack)
- **Alerting:** PagerDuty/OpsGenie integration

---

## Conclusion

The Liyaqa backend architecture prioritizes:

1. **Maintainability:** Feature-based organization, clean separation of concerns
2. **Security:** Defense in depth, comprehensive audit logging
3. **Scalability:** Multi-tenant design, horizontal scalability
4. **Extensibility:** Plugin-based integrations, clean abstractions
5. **Compliance:** Immutable audit trail, data isolation

This architecture provides a solid foundation for a SaaS sports facility management platform while maintaining flexibility for future growth and evolution.
