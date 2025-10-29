# Sport Facility Management System - Complete Guide

## Overview

The Sport Facility Management System handles sport facilities (clubs) and their physical branch locations. Each tenant can own multiple sport facilities, and each facility can have multiple branches representing different physical locations.

---

## 🏗️ Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│            Sport Facility Management System                  │
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
│  │ (Request/    │   │   Entities   │   │  (PostgreSQL  │   │
│  │  Response)   │   │              │   │    Tables)    │   │
│  └──────────────┘   └──────────────┘   └───────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Domain Hierarchy

```
Tenant (Organization)
  │
  ├─▶ Sport Facility (Club) #1
  │     │
  │     ├─▶ Branch #1 (Main Location)
  │     ├─▶ Branch #2 (North Location)
  │     └─▶ Branch #3 (Downtown Location)
  │
  ├─▶ Sport Facility (Club) #2
  │     └─▶ Branch #1 (Single Location)
  │
  └─▶ Sport Facility (Club) #3
        ├─▶ Branch #1 (Location A)
        └─▶ Branch #2 (Location B)
```

---

## 📊 Domain Model

### SportFacility Entity

**Location:** `src/main/kotlin/com/liyaqa/backend/internal/domain/facility/SportFacility.kt`

**Key Fields:**
- `id`: UUID primary key
- `owner`: Reference to Tenant
- `name`: Facility name
- `facilityType`: Type of sport (Tennis, Basketball, etc.)
- `status`: Operational status (ACTIVE, INACTIVE, UNDER_MAINTENANCE, CLOSED)
- Contact information (email, phone, website)
- Social media links (Facebook, Instagram, Twitter)
- Business information (established date, registration number)
- `amenities`: Available facilities (parking, locker rooms, cafe, etc.)
- `operatingHours`: Operating schedule (JSON format)

**Business Methods:**
- `isOperational()`: Check if facility is active
- `canAcceptBookings()`: Check if can accept bookings
- `activate()`: Activate facility
- `deactivate()`: Deactivate facility
- `markUnderMaintenance()`: Mark as under maintenance
- `close()`: Close permanently

### FacilityBranch Entity

**Location:** `src/main/kotlin/com/liyaqa/backend/internal/domain/facility/FacilityBranch.kt`

**Key Fields:**
- `id`: UUID primary key
- `facility`: Reference to SportFacility
- `name`: Branch name
- `isMainBranch`: Designation as main location
- **Address:** addressLine1, addressLine2, city, stateProvince, postalCode, country
- **Geographic:** latitude, longitude (for mapping)
- **Capacity:** totalCourts, totalCapacity
- `status`: Branch status (ACTIVE, INACTIVE, UNDER_RENOVATION, etc.)
- Branch-specific amenities and operating hours

**Business Methods:**
- `isOperational()`: Check if branch is active
- `canAcceptBookings()`: Check if can accept bookings
- `hasCoordinates()`: Check if has geographic coordinates
- `getFullAddress()`: Get complete address string
- `activate()`, `deactivate()`, `markUnderRenovation()`, etc.

### Status Enums

#### FacilityStatus
- `ACTIVE`: Facility operational and accepting bookings
- `INACTIVE`: Temporarily not accepting new bookings
- `UNDER_MAINTENANCE`: Being renovated or maintained
- `CLOSED`: Permanently closed

#### BranchStatus
- `ACTIVE`: Branch operational
- `INACTIVE`: Temporarily inactive
- `UNDER_RENOVATION`: Being renovated
- `TEMPORARILY_CLOSED`: Short-term closure (weather, emergency)
- `PERMANENTLY_CLOSED`: No longer operates

---

## 🔌 API Endpoints

### Base URL
```
/api/v1/internal/facilities
```

### Facility Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| POST | `/` | FACILITY_CREATE | Create new facility |
| GET | `/` | FACILITY_VIEW | Search/list facilities |
| GET | `/{id}` | FACILITY_VIEW | Get facility by ID |
| GET | `/by-tenant/{tenantId}` | FACILITY_VIEW | Get facilities by tenant |
| PUT | `/{id}` | FACILITY_UPDATE | Update facility |
| DELETE | `/{id}` | FACILITY_DELETE | Delete facility |

### Branch Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| POST | `/branches` | FACILITY_MANAGE_BRANCHES | Create new branch |
| GET | `/branches` | FACILITY_VIEW | Search/list branches |
| GET | `/branches/{id}` | FACILITY_VIEW | Get branch by ID |
| GET | `/{facilityId}/branches` | FACILITY_VIEW | Get branches for facility |
| PUT | `/branches/{id}` | FACILITY_MANAGE_BRANCHES | Update branch |
| DELETE | `/branches/{id}` | FACILITY_MANAGE_BRANCHES | Delete branch |

### Example Requests

#### Create Facility
```http
POST /api/v1/internal/facilities
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "ownerTenantId": "uuid-of-tenant",
  "name": "Elite Tennis Academy",
  "description": "Premier tennis training facility",
  "facilityType": "Tennis",
  "contactEmail": "contact@elitetennis.com",
  "contactPhone": "+1-555-0100",
  "website": "https://elitetennis.com",
  "socialInstagram": "@elitetennis",
  "amenities": ["parking", "pro_shop", "locker_rooms", "cafe"],
  "operatingHours": "{\"monday\": \"06:00-22:00\", \"tuesday\": \"06:00-22:00\"}",
  "timezone": "America/New_York"
}
```

#### Create Branch
```http
POST /api/v1/internal/facilities/branches
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "facilityId": "uuid-of-facility",
  "name": "Downtown Branch",
  "description": "Main downtown location",
  "isMainBranch": true,
  "addressLine1": "123 Main Street",
  "city": "New York",
  "stateProvince": "NY",
  "postalCode": "10001",
  "country": "USA",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "totalCourts": 8,
  "totalCapacity": 100,
  "amenities": ["indoor_courts", "parking", "locker_rooms"]
}
```

#### Search Facilities
```http
GET /api/v1/internal/facilities?searchTerm=tennis&status=ACTIVE&page=0&size=20
Authorization: Bearer {jwt_token}
```

#### Search Branches
```http
GET /api/v1/internal/facilities/branches?city=New York&status=ACTIVE&page=0&size=20
Authorization: Bearer {jwt_token}
```

---

## 📁 File Structure

```
src/main/kotlin/com/liyaqa/backend/internal/
├── domain/facility/
│   ├── SportFacility.kt              # Main facility entity
│   ├── FacilityBranch.kt             # Branch entity
│   ├── FacilityStatus.kt             # Facility status enum
│   └── BranchStatus.kt               # Branch status enum
│
├── dto/facility/
│   ├── FacilityCreateRequest.kt      # Create facility DTO
│   ├── FacilityUpdateRequest.kt      # Update facility DTO
│   ├── FacilityResponse.kt           # Full facility response DTO
│   ├── BranchCreateRequest.kt        # Create branch DTO
│   ├── BranchUpdateRequest.kt        # Update branch DTO
│   └── BranchResponse.kt             # Full branch response DTO
│
├── repository/
│   ├── SportFacilityRepository.kt    # Facility data access (20+ queries)
│   └── FacilityBranchRepository.kt   # Branch data access (25+ queries)
│
├── service/
│   └── FacilityService.kt            # Business logic layer
│
└── controller/
    └── FacilityController.kt          # REST API layer (12 endpoints)

src/main/resources/db/changelog/
└── db.changelog-master.xml            # Changesets 008-009: Facility & Branch tables
```

---

## 🗄️ Database Schema

### Sport Facilities Table

```sql
CREATE TABLE sport_facilities (
    -- Identity
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    owner_tenant_id UUID NOT NULL REFERENCES tenants(id),

    -- Basic Info
    name VARCHAR(255) NOT NULL,
    description TEXT,
    facility_type VARCHAR(100) NOT NULL,

    -- Contact
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    website VARCHAR(255),

    -- Social Media
    social_facebook VARCHAR(255),
    social_instagram VARCHAR(255),
    social_twitter VARCHAR(255),

    -- Business
    established_date DATE,
    registration_number VARCHAR(100),

    -- Features
    amenities TEXT,
    operating_hours TEXT,

    -- Status
    status VARCHAR(50) DEFAULT 'ACTIVE',

    -- Settings
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en_US',
    currency VARCHAR(3) DEFAULT 'USD',

    -- Audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by_id UUID REFERENCES internal_employees(id),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_facility_tenant ON sport_facilities(owner_tenant_id);
CREATE INDEX idx_facility_status ON sport_facilities(status);
CREATE INDEX idx_facility_type ON sport_facilities(facility_type);
```

### Facility Branches Table

```sql
CREATE TABLE facility_branches (
    -- Identity
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    facility_id UUID NOT NULL REFERENCES sport_facilities(id) ON DELETE CASCADE,

    -- Basic Info
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_main_branch BOOLEAN DEFAULT FALSE,

    -- Address
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,

    -- Geographic
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    -- Contact
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),

    -- Capacity
    total_courts INTEGER DEFAULT 0,
    total_capacity INTEGER DEFAULT 0,

    -- Features
    amenities TEXT,
    operating_hours TEXT,

    -- Status
    status VARCHAR(50) DEFAULT 'ACTIVE',
    timezone VARCHAR(50) DEFAULT 'UTC',

    -- Audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by_id UUID REFERENCES internal_employees(id),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_branch_facility ON facility_branches(facility_id);
CREATE INDEX idx_branch_status ON facility_branches(status);
CREATE INDEX idx_branch_city ON facility_branches(city);
CREATE INDEX idx_branch_coordinates ON facility_branches(latitude, longitude);
```

---

## 🔐 Permissions

Facility operations require specific permissions:

- **FACILITY_VIEW**: View facility and branch information
- **FACILITY_CREATE**: Create new facilities
- **FACILITY_UPDATE**: Modify facility details
- **FACILITY_DELETE**: Delete facilities (cascade deletes branches)
- **FACILITY_MANAGE_BRANCHES**: Create, update, delete branches

---

## 🎯 Business Workflows

### 1. Facility Creation

```
┌─────────────┐
│ Create      │
│ Facility    │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ Status: ACTIVE      │
│ No branches yet     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Add Main Branch     │
│ (isMainBranch=true) │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│ Add More Branches   │
│ (optional)          │
└─────────────────────┘
```

### 2. Multi-Branch Management

```
Elite Tennis Academy (Facility)
  │
  ├─▶ Downtown Branch (Main) - Status: ACTIVE
  │     - 8 courts, 100 capacity
  │     - GPS: 40.7128, -74.0060
  │
  ├─▶ North Branch - Status: ACTIVE
  │     - 6 courts, 75 capacity
  │     - GPS: 40.7589, -73.9851
  │
  └─▶ Beach Branch - Status: UNDER_RENOVATION
        - 4 courts, 50 capacity
        - Reopening: Summer 2025
```

---

## 📈 Use Cases

### Common Scenarios

1. **Single Location Facility**
   - Create facility
   - Create one main branch
   - All bookings go to that branch

2. **Multi-Location Chain**
   - Create facility (brand/organization)
   - Create multiple branches (different cities)
   - Each branch has own capacity and amenities

3. **Seasonal Operations**
   - Mark branches as TEMPORARILY_CLOSED in off-season
   - Reactivate when reopening

4. **Facility Expansion**
   - Start with main branch
   - Add new branches as business grows
   - Each branch tracked independently

---

## 🧪 Testing Scenarios

1. **Create facility with branches**: Facility → Main Branch → Additional Branches
2. **Update main branch designation**: Transfer main branch to different location
3. **Geographic search**: Find branches near coordinates
4. **Capacity aggregation**: Calculate total capacity across all branches
5. **Cascade delete**: Delete facility and verify all branches deleted

---

## 🚀 Deployment Checklist

### Database
- [ ] Run Liquibase migrations (changesets 008-009)
- [ ] Verify foreign key constraints
- [ ] Check cascade delete behavior
- [ ] Test geographic indexes

### Application
- [ ] Verify SportFacilityRepository and FacilityBranchRepository beans
- [ ] Test FacilityService methods
- [ ] Verify FacilityController endpoints
- [ ] Check permission enforcement

---

## 📚 Related Documentation

- **TENANT_MANAGEMENT.md**: Tenant organization management
- **CONFIGURATION.md**: Environment setup
- **Employee Management**: Internal team permissions

---

*Last Updated: 2025-10-29*
*Version: 1.0.0*
