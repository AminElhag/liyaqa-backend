# Tenant Management System - Complete Guide

## Overview

The Tenant Management System handles customer organizations (sports facilities) throughout their entire lifecycle with Liyaqa. Each tenant represents a sports facility that uses our platform to manage their operations.

---

## 🏗️ Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Tenant Management System                  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐   ┌──────────────┐   ┌───────────────┐   │
│  │ Controller   │──▶│   Service    │──▶│  Repository   │   │
│  │ (REST API)   │   │ (Business    │   │  (Data        │   │
│  │              │   │  Logic)      │   │   Access)     │   │
│  └──────────────┘   └──────────────┘   └───────────────┘   │
│         │                   │                    │           │
│         ▼                   ▼                    ▼           │
│  ┌──────────────┐   ┌──────────────┐   ┌───────────────┐   │
│  │    DTOs      │   │   Domain     │   │   Database    │   │
│  │ (Request/    │   │   Entities   │   │   (PostgreSQL │   │
│  │  Response)   │   │              │   │    Tables)    │   │
│  └──────────────┘   └──────────────┘   └───────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Domain Model

### Tenant Entity

**Location:** `src/main/kotlin/com/liyaqa/backend/internal/domain/tenant/Tenant.kt`

**Key Fields:**
- `id`: UUID primary key
- `tenantId`: Unique string identifier (e.g., "acme-sports")
- `name`: Organization name
- `status`: Operational status (ACTIVE, SUSPENDED, TERMINATED, PENDING_ACTIVATION)
- `subscriptionStatus`: Billing status (TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED, LIFETIME)
- `planTier`: Service level (FREE, STARTER, PROFESSIONAL, ENTERPRISE, CUSTOM)

**Business Methods:**
- `hasActiveSubscription()`: Check if subscription is valid
- `canAccess()`: Check if tenant can use the platform
- `suspend()`: Temporarily block access
- `reactivate()`: Restore suspended tenant
- `terminate()`: Permanent closure
- `acceptTerms()`: Record terms acceptance
- `upgradePlan()` / `downgradePlan()`: Change service tier

### Status Enums

#### TenantStatus
- `ACTIVE`: Normal operations
- `SUSPENDED`: Temporarily blocked (payment issues, violations)
- `TERMINATED`: Permanently closed
- `PENDING_ACTIVATION`: Onboarding in progress

#### SubscriptionStatus
- `TRIAL`: Free trial period
- `ACTIVE`: Current and paid
- `PAST_DUE`: Payment overdue (grace period)
- `CANCELLED`: Cancelled but still within paid period
- `EXPIRED`: No longer active
- `LIFETIME`: Special lifetime access

#### PlanTier
- `FREE`: Limited features (1 court, 50 bookings/month)
- `STARTER`: Entry level (5 courts, 500 bookings/month)
- `PROFESSIONAL`: Full features (unlimited, advanced reporting)
- `ENTERPRISE`: Dedicated support, SLA, custom integrations
- `CUSTOM`: Negotiated arrangements

---

## 🔌 API Endpoints

### Base URL
```
/api/v1/internal/tenants
```

### Endpoints Summary

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| POST | `/` | TENANT_CREATE | Create new tenant |
| GET | `/` | TENANT_VIEW | Search/list tenants |
| GET | `/{id}` | TENANT_VIEW | Get tenant by ID |
| GET | `/by-tenant-id/{tenantId}` | TENANT_VIEW | Get by tenant ID |
| PUT | `/{id}` | TENANT_UPDATE | Update tenant |
| DELETE | `/{id}` | TENANT_DELETE | Terminate tenant |
| POST | `/{id}/suspend` | TENANT_SUSPEND | Suspend tenant |
| POST | `/{id}/reactivate` | TENANT_SUSPEND | Reactivate tenant |
| POST | `/{id}/accept-terms` | TENANT_UPDATE | Accept T&C |
| POST | `/{id}/change-plan` | TENANT_UPDATE | Change plan tier |
| GET | `/attention-needed` | TENANT_VIEW | Get tenants needing action |
| GET | `/analytics` | TENANT_VIEW | Get statistics |

### Example Requests

#### Create Tenant
```http
POST /api/v1/internal/tenants
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "tenantId": "acme-sports",
  "name": "Acme Sports Complex",
  "contactEmail": "contact@acmesports.com",
  "contactPhone": "+1-555-0123",
  "billingEmail": "billing@acmesports.com",
  "planTier": "PROFESSIONAL",
  "facilityType": "Multi-Sport Complex",
  "subdomain": "acme",
  "contractStartDate": "2025-01-01",
  "timezone": "America/New_York"
}
```

#### Search Tenants
```http
GET /api/v1/internal/tenants?searchTerm=acme&status=ACTIVE&planTier=PROFESSIONAL&page=0&size=20
Authorization: Bearer {jwt_token}
```

#### Suspend Tenant
```http
POST /api/v1/internal/tenants/{id}/suspend
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "reason": "Payment overdue by 30 days"
}
```

#### Accept Terms
```http
POST /api/v1/internal/tenants/{id}/accept-terms
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "acceptedBy": "John Doe (Facility Manager)",
  "termsVersion": "2025-01"
}
```

#### Change Plan
```http
POST /api/v1/internal/tenants/{id}/change-plan
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "newPlanTier": "ENTERPRISE"
}
```

---

## 🎯 Business Workflows

### 1. Tenant Onboarding

```
┌─────────────┐
│ Sales Team  │
│ Creates     │
│ Tenant      │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ Status:             │
│ PENDING_ACTIVATION  │
│ Subscription: TRIAL │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Configure Facility  │
│ - Set up courts     │
│ - Add staff         │
│ - Customize         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Accept Terms        │
│ - Version recorded  │
│ - Timestamp saved   │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Status: ACTIVE      │
│ Ready for use!      │
└─────────────────────┘
```

### 2. Subscription Lifecycle

```
TRIAL (14-30 days)
  │
  ├─▶ Convert to paid ──▶ ACTIVE
  │
  └─▶ Trial expires ──▶ EXPIRED

ACTIVE
  │
  ├─▶ Payment fails ──▶ PAST_DUE (grace period)
  │                       │
  │                       ├─▶ Payment received ──▶ ACTIVE
  │                       └─▶ Grace expires ──▶ SUSPENDED
  │
  ├─▶ Customer cancels ──▶ CANCELLED (until contract end)
  │                         │
  │                         └─▶ Contract ends ──▶ EXPIRED
  │
  └─▶ Upgrade/Downgrade ──▶ ACTIVE (new tier)
```

### 3. Suspension & Recovery

```
ACTIVE
  │
  ▼
[Violation or Payment Issue]
  │
  ▼
SUSPENDED
  │
  ├─▶ Issue resolved ──▶ Reactivate ──▶ ACTIVE
  │
  └─▶ Unresolved ──▶ Terminate ──▶ TERMINATED (permanent)
```

---

## 📁 File Structure

```
src/main/kotlin/com/liyaqa/backend/internal/
├── domain/tenant/
│   ├── Tenant.kt                  # Main entity
│   ├── TenantStatus.kt            # Status enum
│   ├── SubscriptionStatus.kt      # Subscription enum
│   └── PlanTier.kt                # Plan tier enum
│
├── dto/tenant/
│   ├── TenantCreateRequest.kt     # Create tenant DTO
│   ├── TenantUpdateRequest.kt     # Update tenant DTO
│   ├── TenantResponse.kt          # Full response DTO
│   ├── TenantBasicResponse.kt     # Minimal response DTO
│   ├── TenantSearchFilter.kt      # Search filter DTO
│   └── TenantActionRequest.kt     # Action DTOs (suspend, etc.)
│
├── repository/
│   └── TenantRepository.kt        # Data access layer (25+ queries)
│
├── service/
│   └── TenantService.kt           # Business logic layer
│
└── controller/
    └── TenantController.kt        # REST API layer

src/main/resources/db/changelog/
└── db.changelog-master.xml        # Changeset 007: Enhanced tenant schema
```

---

## 🗄️ Database Schema

### Tenants Table

```sql
CREATE TABLE tenants (
    -- Identity
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,

    -- Contact
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    contact_person VARCHAR(255),

    -- Billing
    billing_email VARCHAR(255) NOT NULL,
    billing_address TEXT,
    tax_id VARCHAR(100),

    -- Subscription
    plan_tier VARCHAR(50) NOT NULL DEFAULT 'FREE',
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'TRIAL',

    -- Multi-tenancy
    subdomain VARCHAR(100) UNIQUE,

    -- Contract
    contract_start_date DATE,
    contract_end_date DATE,
    terms_accepted_at TIMESTAMP,
    terms_accepted_by VARCHAR(255),
    terms_version VARCHAR(50),

    -- Metadata
    description TEXT,
    facility_type VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en_US',

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_ACTIVATION',
    suspended_at TIMESTAMP,
    suspended_by_id UUID,
    suspension_reason TEXT,

    -- Audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by_id UUID,
    version BIGINT DEFAULT 0,

    -- Foreign Keys
    FOREIGN KEY (created_by_id) REFERENCES internal_employees(id),
    FOREIGN KEY (suspended_by_id) REFERENCES internal_employees(id)
);

-- Indexes
CREATE INDEX idx_tenant_status ON tenants(status);
CREATE INDEX idx_tenant_subscription_status ON tenants(subscription_status);
CREATE INDEX idx_tenant_plan_tier ON tenants(plan_tier);
CREATE INDEX idx_tenant_contact_email ON tenants(contact_email);
CREATE INDEX idx_tenant_subdomain ON tenants(subdomain);
```

---

## 🔐 Permissions

Tenant operations require specific permissions:

- **TENANT_VIEW**: View tenant information, search, analytics
- **TENANT_CREATE**: Create new tenants (onboarding)
- **TENANT_UPDATE**: Modify tenant details, accept terms, change plans
- **TENANT_DELETE**: Terminate tenants (permanent)
- **TENANT_SUSPEND**: Suspend/reactivate tenants (temporary block)

### Permission Matrix

| Role | VIEW | CREATE | UPDATE | DELETE | SUSPEND |
|------|------|--------|--------|--------|---------|
| Support Agent | ✅ | ❌ | ⚠️ (limited) | ❌ | ❌ |
| Account Manager | ✅ | ✅ | ✅ | ❌ | ⚠️ (can suspend) |
| Admin | ✅ | ✅ | ✅ | ✅ | ✅ |
| Finance | ✅ | ❌ | ⚠️ (billing only) | ❌ | ⚠️ (payment issues) |

---

## 📈 Analytics & Reporting

### Available Metrics

**GET /api/v1/internal/tenants/analytics**

Returns:
```json
{
  "total_tenants": 1250,
  "active_tenants": 1100,
  "suspended_tenants": 25,
  "terminated_tenants": 125,
  "by_plan_tier": {
    "FREE": 450,
    "STARTER": 320,
    "PROFESSIONAL": 280,
    "ENTERPRISE": 50
  },
  "by_subscription_status": {
    "TRIAL": 150,
    "ACTIVE": 950,
    "PAST_DUE": 30,
    "CANCELLED": 20,
    "EXPIRED": 100
  }
}
```

### Attention Needed

**GET /api/v1/internal/tenants/attention-needed**

Groups tenants requiring action:
- `past_due`: Payment overdue
- `suspended`: Currently suspended accounts
- `expiring_contracts`: Contracts expiring in next 30 days
- `expired_contracts`: Already expired contracts

---

## 🧪 Testing Checklist

### Unit Tests Needed

- [ ] Tenant entity business methods
- [ ] TenantService CRUD operations
- [ ] TenantService permission checks
- [ ] TenantRepository custom queries
- [ ] DTO validation

### Integration Tests Needed

- [ ] Create tenant flow (end-to-end)
- [ ] Suspend/reactivate flow
- [ ] Plan upgrade/downgrade
- [ ] Terms acceptance
- [ ] Search and filtering
- [ ] Analytics endpoints

### Manual Testing Scenarios

1. **Onboarding**: Create tenant → Accept terms → Verify active
2. **Suspension**: Suspend for payment → Verify access blocked → Pay → Reactivate
3. **Plan Change**: Upgrade from FREE → PROFESSIONAL → Verify features
4. **Contract Expiry**: Set contract end date → Wait → Verify expiry handling
5. **Search**: Filter by status, plan, facility type → Verify results

---

## 🚀 Deployment Checklist

### Database

- [ ] Run Liquibase migration (changeset 007)
- [ ] Verify indexes created
- [ ] Check foreign key constraints
- [ ] Test rollback procedure

### Application

- [ ] Deploy new code
- [ ] Verify TenantRepository bean creation
- [ ] Test health endpoint
- [ ] Monitor logs for errors

### Post-Deployment

- [ ] Create test tenant
- [ ] Verify all endpoints
- [ ] Check audit logs
- [ ] Monitor performance metrics

---

## 🐛 Troubleshooting

### Common Issues

**1. Tenant ID already exists**
```
Error: "Tenant ID 'acme-sports' already exists"
Solution: Use a different tenant_id or check if duplicate
```

**2. Subdomain conflict**
```
Error: "Subdomain 'acme' already exists"
Solution: Choose unique subdomain or leave null
```

**3. Cannot suspend terminated tenant**
```
Error: "Cannot suspend terminated tenant"
Solution: Terminated tenants cannot be modified
```

**4. Missing permission**
```
Error: "Insufficient permissions: TENANT_CREATE required"
Solution: Assign appropriate permission to employee
```

---

## 📚 Related Documentation

- **CONFIGURATION.md**: Environment setup and configuration
- **Employee Management**: Internal team permissions
- **Audit System**: Tracking all tenant operations
- **Multi-Tenancy**: Tenant isolation and data segregation

---

## 🔄 Future Enhancements

### Planned Features

1. **Automated Dunning**: Auto-suspend after X days past due
2. **Self-Service Portal**: Tenants manage their own subscriptions
3. **Usage Metrics**: Track bookings, users, API calls per tenant
4. **Webhooks**: Notify external systems of tenant events
5. **Billing Integration**: Stripe/payment gateway integration
6. **Multi-Location**: Support facilities with multiple locations
7. **White-Labeling**: Custom branding per tenant
8. **Contract Templates**: Standardized agreement generation

### API Improvements

- Bulk operations (create/update multiple tenants)
- Advanced filtering (date ranges, custom fields)
- Export functionality (CSV, Excel)
- Webhook subscriptions

---

## 📞 Support

For tenant management issues:
1. Check audit logs for operation history
2. Verify employee permissions
3. Review tenant status and subscription status
4. Check database constraints
5. Contact dev team if unresolved

---

*Last Updated: 2025-10-29*
*Version: 1.0.0*
