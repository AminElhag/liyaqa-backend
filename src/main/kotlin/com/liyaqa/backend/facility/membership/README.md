# Membership Management System

## Overview

The **Membership Management System** handles the complete lifecycle of facility members, membership plans, subscriptions, and promotional discounts. It enables sports facilities to manage their customer base, offer various membership tiers, track subscriptions, and provide promotional offers.

## Module Structure

```
facility/membership/
├── controller/
│   ├── MemberController.kt            # Member profile management
│   ├── MembershipPlanController.kt    # Membership plan configuration
│   ├── MembershipController.kt        # Active subscription management
│   └── DiscountController.kt          # Discount and promo code management
├── data/
│   ├── MemberRepository.kt            # Member data access
│   ├── MembershipPlanRepository.kt    # Plan data access
│   ├── MembershipRepository.kt        # Subscription data access
│   ├── DiscountRepository.kt          # Discount data access
│   └── DiscountUsageRepository.kt     # Discount usage tracking
├── domain/
│   ├── Member.kt                      # Member entity with authentication
│   ├── MembershipPlan.kt              # Plan template with benefits
│   ├── Membership.kt                  # Active subscription
│   ├── Discount.kt                    # Promotional discounts
│   └── DiscountUsage.kt               # Usage tracking
├── dto/
│   ├── MemberDto.kt                   # Member request/response DTOs
│   ├── MembershipPlanDto.kt           # Plan request/response DTOs
│   ├── MembershipDto.kt               # Subscription request/response DTOs
│   └── DiscountDto.kt                 # Discount request/response DTOs
└── service/
    ├── MemberService.kt               # Member management logic
    ├── MembershipPlanService.kt       # Plan management logic
    ├── MembershipService.kt           # Subscription lifecycle logic
    └── DiscountService.kt             # Discount application logic
```

## Domain Models

### 1. Member

Represents a customer who uses the sports facility.

**Key Attributes:**

**Basic Information:**
- `firstName`, `lastName`: Member's name
- `email`: Contact email (unique per branch)
- `phoneNumber`: Contact phone
- `memberNumber`: Unique identifier (e.g., "MEM-2025-001")
- `dateOfBirth`: Date of birth for age calculation
- `gender`: Gender (MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY)
- `nationalId`: National identification number

**Address:**
- `addressLine1`, `addressLine2`
- `city`, `postalCode`, `country`

**Emergency Contact:**
- `emergencyContactName`: Emergency contact person
- `emergencyContactPhone`: Emergency contact phone
- `emergencyContactRelationship`: Relationship to member

**Medical Information:**
- `bloodType`: Blood type
- `medicalConditions`: Known medical conditions
- `allergies`: Allergies
- `medications`: Current medications

**Status:**
- `status`: Member status (ACTIVE, SUSPENDED, BANNED, INACTIVE)
- `statusReason`: Reason for status change
- `statusChangedAt`: When status was changed

**Preferences:**
- `preferredLanguage`: Language preference (default: "en")
- `marketingConsent`: Agreed to marketing communications
- `smsNotifications`: SMS notification preference
- `emailNotifications`: Email notification preference

**Authentication (see Member Auth module):**
- `passwordHash`: Hashed password for member login
- `emailVerified`: Email verification status
- `emailVerificationToken`: Token for email verification
- `passwordResetToken`: Token for password reset
- `lastLoginAt`: Last successful login timestamp
- `failedLoginAttempts`: Failed login counter
- `lockedUntil`: Account lock expiration (after 5 failed attempts)

**Business Methods:**
```kotlin
member.getFullName()                   // Get formatted full name
member.isActive()                      // Check if member is active
member.calculateAge()                  // Calculate age from DOB
member.suspend(reason)                 // Suspend member account
member.reactivate()                    // Reactivate suspended account
member.ban(reason)                     // Ban member (permanent)
member.isLocked()                      // Check if account is locked
member.recordFailedLogin()             // Increment failed attempts
member.recordSuccessfulLogin()         // Reset failed attempts
member.generateEmailVerificationToken() // Generate verification token
member.verifyEmail(token)              // Verify email with token
member.generatePasswordResetToken()    // Generate password reset token
member.isPasswordResetTokenValid(token) // Validate reset token
```

### 2. MembershipPlan

Template defining a membership tier with pricing and benefits.

**Key Attributes:**

**Basic Information:**
- `name`: Plan name (e.g., "Premium", "Basic", "Family")
- `description`: Detailed description of benefits
- `planType`: Type (INDIVIDUAL, FAMILY, STUDENT, SENIOR, CORPORATE, TRIAL, DAY_PASS, PUNCH_CARD)

**Pricing:**
- `price`: Base price
- `currency`: Currency code (default: "USD")
- `billingCycle`: Billing frequency (DAILY, WEEKLY, MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL, ONE_TIME)
- `durationMonths`: Subscription duration in months
- `setupFee`: One-time setup fee (optional)
- `discountPercentage`: Promotional discount percentage (optional)

**Features & Benefits:**
- `features`: JSON/comma-separated list of features
- `maxBookingsPerMonth`: Monthly booking limit (null = unlimited)
- `maxConcurrentBookings`: Maximum active bookings (null = unlimited)
- `advanceBookingDays`: How far in advance can book
- `cancellationHours`: Minimum hours before cancellation
- `guestPasses`: Number of guest passes per month

**Access Control:**
- `hasCourtAccess`: Court booking access
- `hasClassAccess`: Class/session access
- `hasGymAccess`: Gym/fitness area access
- `hasLockerAccess`: Locker access
- `priorityLevel`: Booking priority (higher = better)

**Discounts:**
- `courtBookingDiscount`: Percentage discount on court bookings

**Status:**
- `isActive`: Plan is active and can be sold
- `isVisible`: Plan is visible on website/app
- `maxMembers`: Capacity limit (optional)
- `currentMembers`: Current enrollment count

**Terms:**
- `termsAndConditions`: Legal terms and conditions
- `autoRenew`: Auto-renewal enabled by default

**Business Methods:**
```kotlin
plan.isAtCapacity()                    // Check if plan reached member limit
plan.isAvailable()                     // Check if plan can be purchased
plan.getEffectivePrice()               // Calculate price after discount
plan.getFirstPaymentAmount()           // Get first payment (price + setup fee)
plan.incrementMemberCount()            // Add member to count
plan.decrementMemberCount()            // Remove member from count
```

### 3. Membership

Active subscription linking a member to a membership plan.

**Key Attributes:**

**Membership Details:**
- `member`: Reference to member
- `plan`: Reference to membership plan
- `membershipNumber`: Unique subscription identifier
- `startDate`: Subscription start date
- `endDate`: Subscription end date

**Status:**
- `status`: Status (ACTIVE, EXPIRED, CANCELLED, SUSPENDED, PENDING)
- `statusChangedAt`: Last status change timestamp
- `statusReason`: Reason for status change

**Pricing & Payment:**
- `pricePaid`: Amount paid for this subscription
- `setupFeePaid`: Setup fee paid (if applicable)
- `currency`: Payment currency
- `paymentMethod`: Payment method (CARD, CASH, BANK_TRANSFER, etc.)
- `paymentReference`: External payment reference
- `paidAt`: Payment timestamp

**Auto-Renewal:**
- `autoRenew`: Auto-renewal enabled
- `nextBillingDate`: Next billing date
- `renewalReminderSent`: Reminder sent flag

**Usage Tracking:**
- `bookingsUsed`: Number of bookings made this period
- `guestPassesUsed`: Guest passes used this period
- `lastUsedAt`: Last activity timestamp

**Cancellation:**
- `cancelledAt`: Cancellation timestamp
- `cancelledBy`: Who cancelled (member name or employee)
- `cancellationReason`: Reason for cancellation

**Business Methods:**
```kotlin
membership.isCurrentlyActive()         // Check if active and within dates
membership.isExpired()                 // Check if past end date
membership.isExpiringWithin(days)      // Check if expiring soon
membership.canMakeBooking()            // Check if can book (considering limits)
membership.recordBooking()             // Increment booking usage
membership.useGuestPass()              // Use a guest pass
membership.cancel(reason, cancelledBy) // Cancel subscription
membership.suspend(reason)             // Suspend subscription
membership.reactivate()                // Reactivate suspended subscription
membership.markExpired()               // Mark as expired
membership.renew(newEndDate, price)    // Renew subscription
```

### 4. Discount

Promotional discounts and promo codes.

**Key Attributes:**

**Discount Details:**
- `code`: Promo code (e.g., "SUMMER2025", null for employee-applied)
- `name`: Human-readable name
- `description`: Detailed description

**Discount Configuration:**
- `discountType`: Type (PERCENTAGE, FIXED_AMOUNT)
- `value`: Discount value (percentage 0-100 or fixed amount)
- `currency`: Currency for fixed amount discounts
- `applicationMethod`: How applied (CODE, EMPLOYEE_APPLIED)
- `scope`: Applicability (ALL_PLANS, SPECIFIC_PLANS, SPECIFIC_TYPES)

**Validity:**
- `validFrom`: Start date
- `validUntil`: End date
- `isActive`: Active status

**Usage Limits:**
- `maxTotalUsage`: Total usage limit (null = unlimited)
- `maxUsagePerMember`: Per-member limit (null = unlimited)
- `currentUsageCount`: Current total usage

**Constraints:**
- `minPurchaseAmount`: Minimum purchase requirement
- `maxDiscountAmount`: Maximum discount cap (for percentage discounts)

**Applicability:**
- `applicablePlans`: Specific plans (for SPECIFIC_PLANS scope)
- `applicableTypes`: Plan types (for SPECIFIC_TYPES scope)

**Business Methods:**
```kotlin
discount.isCurrentlyValid()                      // Check if valid now
discount.hasReachedUsageLimit()                  // Check usage limit
discount.isApplicableToPlan(plan)                // Check if applies to plan
discount.calculateDiscountAmount(originalPrice)  // Calculate discount amount
discount.calculateFinalPrice(originalPrice)      // Calculate final price
discount.incrementUsage()                        // Increment usage counter
discount.meetsMinimumPurchase(price)             // Check minimum purchase
```

### 5. DiscountUsage

Tracks individual discount applications (for audit and per-member limits).

**Key Attributes:**
- `discount`: Reference to discount used
- `member`: Reference to member who used it
- `membership`: Reference to membership it was applied to
- `originalPrice`: Price before discount
- `discountAmount`: Discount amount applied
- `finalPrice`: Price after discount
- `usedAt`: Timestamp of usage

## Key Features

### 1. Member Management

**Registration:**
- Create member profile with contact and personal information
- Optional medical information for safety
- Emergency contact details
- Branch-level member scoping

**Profile Management:**
- Update personal information
- Manage communication preferences
- Upload profile picture
- View booking and membership history

**Status Management:**
- Suspend members (temporary, e.g., payment issues)
- Ban members (permanent, e.g., misconduct)
- Reactivate suspended members
- Track status change history

**Authentication:**
- Email/password authentication
- Email verification required
- Password reset flow
- Account lockout after 5 failed attempts (30-minute lock)

### 2. Membership Plan Configuration

**Plan Creation:**
- Define multiple membership tiers
- Configure pricing and billing cycles
- Set benefit levels and access permissions
- Define booking limits and privileges

**Capacity Management:**
- Set maximum member limits per plan
- Track current enrollment
- Prevent over-enrollment

**Access Control:**
- Court access
- Class/session access
- Gym access
- Locker access
- Priority levels for booking conflicts

**Booking Benefits:**
- Court booking discounts
- Advanced booking windows
- Higher booking limits
- Concurrent booking allowances

### 3. Subscription Lifecycle

**Purchase Flow:**
1. Member selects plan
2. Apply discount code (optional)
3. Calculate pricing (plan price + setup fee - discount)
4. Process payment
5. Create active membership
6. Send confirmation

**Auto-Renewal:**
- Automatic renewal before expiration
- Renewal reminders sent in advance
- Members can disable auto-renewal
- Failed renewals trigger notifications

**Cancellation:**
- Member-initiated cancellation
- Staff-initiated cancellation
- Refund processing (if applicable)
- Usage tracking preserved

**Expiration Handling:**
- Automatic expiration on end date
- Grace periods (configurable)
- Expiration notifications
- Renewal offers

### 4. Discount System

**Discount Types:**
- **Percentage Discounts:** e.g., 20% off
- **Fixed Amount Discounts:** e.g., $50 off

**Application Methods:**
- **Promo Codes:** Members enter code at checkout
- **Employee Applied:** Staff apply discounts manually

**Scope Options:**
- **All Plans:** Applies to any membership plan
- **Specific Plans:** Selected plans only
- **Specific Types:** Plan types (e.g., only FAMILY plans)

**Usage Controls:**
- Total usage limits
- Per-member usage limits
- Date range validity
- Minimum purchase requirements
- Maximum discount caps

**Smart Validation:**
- Automatic expiration checking
- Usage limit enforcement
- Plan applicability validation
- Minimum purchase validation

## Integration with Other Modules

### Booking Module

**Membership Benefits:**
- Booking discounts automatically applied
- Monthly booking limits enforced
- Concurrent booking limits enforced
- Advanced booking windows
- Priority booking access

**Example:**
```kotlin
// When creating booking, membership benefits are checked
if (membership.plan.courtBookingDiscount != null) {
    val discountPercent = membership.plan.courtBookingDiscount!!
    discountAmount = originalPrice * discountPercent / 100
}

// Enforce booking limits
if (!membership.canMakeBooking()) {
    throw IllegalStateException("Monthly booking limit reached")
}

// Track booking against membership
membership.recordBooking()
```

### Payment Module

**Payment Integration:**
- Membership purchase payments
- Auto-renewal payments
- Setup fee processing
- Refund handling

### Notification Module

**Automated Notifications:**
- Welcome email on registration
- Email verification
- Membership purchase confirmation
- Renewal reminders (7 days, 3 days, 1 day before expiration)
- Expiration notices
- Cancellation confirmations
- Password reset emails

## API Endpoints

### Member Management

**Base Path:** `/api/v1/facility/members`

#### Create Member
```http
POST /api/v1/facility/members
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1-555-0123",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "addressLine1": "123 Main St",
  "city": "New York",
  "postalCode": "10001",
  "country": "USA",
  "emergencyContactName": "Jane Doe",
  "emergencyContactPhone": "+1-555-0124",
  "emergencyContactRelationship": "Spouse",
  "preferredLanguage": "en",
  "marketingConsent": true
}

Response (201 Created):
{
  "id": "uuid",
  "memberNumber": "MEM-2025-001",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "status": "ACTIVE",
  "createdAt": "2025-11-15T10:30:00Z"
}
```

#### Get Member
```http
GET /api/v1/facility/members/{id}

Response (200 OK):
{
  "id": "uuid",
  "memberNumber": "MEM-2025-001",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1-555-0123",
  "dateOfBirth": "1990-05-15",
  "age": 34,
  "status": "ACTIVE",
  "activeMemberships": [...],
  "upcomingBookings": [...]
}
```

#### Update Member
```http
PUT /api/v1/facility/members/{id}
Content-Type: application/json

{
  "phoneNumber": "+1-555-9999",
  "city": "Los Angeles",
  "emailNotifications": false
}

Response (200 OK):
{ ... updated member ... }
```

#### Suspend Member
```http
POST /api/v1/facility/members/{id}/suspend
Content-Type: application/json

{
  "reason": "Payment overdue for 30 days"
}

Response (200 OK):
{
  "id": "uuid",
  "status": "SUSPENDED",
  "statusReason": "Payment overdue for 30 days",
  "statusChangedAt": "2025-11-15T10:30:00Z"
}
```

#### Search Members
```http
GET /api/v1/facility/members/search?searchTerm=john&status=ACTIVE&page=0&size=20

Response (200 OK):
{
  "content": [...],
  "totalElements": 45,
  "totalPages": 3,
  "size": 20
}
```

### Membership Plan Management

**Base Path:** `/api/v1/facility/membership-plans`

#### Create Plan
```http
POST /api/v1/facility/membership-plans
Content-Type: application/json

{
  "name": "Premium Monthly",
  "description": "Full access to all facilities with premium benefits",
  "planType": "INDIVIDUAL",
  "price": 99.00,
  "currency": "USD",
  "billingCycle": "MONTHLY",
  "durationMonths": 1,
  "maxBookingsPerMonth": 20,
  "maxConcurrentBookings": 3,
  "courtBookingDiscount": 10.0,
  "hasCourtAccess": true,
  "hasClassAccess": true,
  "hasGymAccess": true,
  "priorityLevel": 2,
  "setupFee": 50.00,
  "autoRenew": true
}

Response (201 Created):
{
  "id": "uuid",
  "name": "Premium Monthly",
  "price": 99.00,
  "effectivePrice": 99.00,
  "firstPaymentAmount": 149.00,
  "isAvailable": true
}
```

#### List Plans
```http
GET /api/v1/facility/membership-plans?branchId=uuid&isActive=true

Response (200 OK):
[
  {
    "id": "uuid",
    "name": "Basic Monthly",
    "price": 49.00,
    "billingCycle": "MONTHLY",
    "isAvailable": true
  },
  {
    "id": "uuid",
    "name": "Premium Monthly",
    "price": 99.00,
    "billingCycle": "MONTHLY",
    "isAvailable": true
  }
]
```

### Membership Subscription Management

**Base Path:** `/api/v1/facility/memberships`

#### Purchase Membership
```http
POST /api/v1/facility/memberships
Content-Type: application/json

{
  "memberId": "uuid",
  "planId": "uuid",
  "startDate": "2025-11-15",
  "paymentMethod": "CREDIT_CARD",
  "paymentReference": "ch_1234567890",
  "discountCode": "WELCOME20",
  "autoRenew": true
}

Response (201 Created):
{
  "id": "uuid",
  "membershipNumber": "MSH-2025-001",
  "member": {...},
  "plan": {...},
  "startDate": "2025-11-15",
  "endDate": "2025-12-15",
  "status": "ACTIVE",
  "pricePaid": 79.20,
  "setupFeePaid": 50.00,
  "discountApplied": 19.80,
  "totalPaid": 129.20,
  "autoRenew": true
}
```

#### Get Active Memberships by Member
```http
GET /api/v1/facility/memberships/by-member/{memberId}

Response (200 OK):
[
  {
    "id": "uuid",
    "membershipNumber": "MSH-2025-001",
    "plan": {...},
    "startDate": "2025-11-15",
    "endDate": "2025-12-15",
    "status": "ACTIVE",
    "bookingsUsed": 5,
    "bookingsRemaining": 15,
    "isExpiringWithin7Days": false
  }
]
```

#### Cancel Membership
```http
POST /api/v1/facility/memberships/{id}/cancel
Content-Type: application/json

{
  "reason": "Moving to different city",
  "cancelledBy": "John Doe"
}

Response (200 OK):
{
  "id": "uuid",
  "status": "CANCELLED",
  "cancelledAt": "2025-11-20T14:30:00Z",
  "cancellationReason": "Moving to different city"
}
```

#### Renew Membership
```http
POST /api/v1/facility/memberships/{id}/renew
Content-Type: application/json

{
  "paymentMethod": "CREDIT_CARD",
  "paymentReference": "ch_9876543210"
}

Response (200 OK):
{
  "id": "uuid",
  "startDate": "2025-11-15",
  "endDate": "2026-11-15",
  "status": "ACTIVE",
  "pricePaid": 99.00,
  "renewedAt": "2025-11-14T10:00:00Z"
}
```

### Discount Management

**Base Path:** `/api/v1/facility/discounts`

#### Create Discount
```http
POST /api/v1/facility/discounts
Content-Type: application/json

{
  "code": "SUMMER2025",
  "name": "Summer Special 2025",
  "description": "20% off all annual memberships",
  "discountType": "PERCENTAGE",
  "value": 20.00,
  "applicationMethod": "CODE",
  "scope": "SPECIFIC_TYPES",
  "applicableTypes": ["INDIVIDUAL", "FAMILY"],
  "validFrom": "2025-06-01",
  "validUntil": "2025-08-31",
  "maxTotalUsage": 100,
  "maxUsagePerMember": 1,
  "minPurchaseAmount": 500.00
}

Response (201 Created):
{
  "id": "uuid",
  "code": "SUMMER2025",
  "discountType": "PERCENTAGE",
  "value": 20.00,
  "validFrom": "2025-06-01",
  "validUntil": "2025-08-31",
  "currentUsageCount": 0,
  "remainingUsage": 100,
  "isCurrentlyValid": true
}
```

#### Validate Discount Code
```http
POST /api/v1/facility/discounts/validate
Content-Type: application/json

{
  "code": "SUMMER2025",
  "planId": "uuid",
  "memberId": "uuid"
}

Response (200 OK):
{
  "isValid": true,
  "discount": {...},
  "discountAmount": 100.00,
  "finalPrice": 400.00,
  "message": "20% discount applied successfully"
}
```

## Database Schema

### Members Table
```sql
CREATE TABLE members (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    facility_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    member_number VARCHAR(50) UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10),
    national_id VARCHAR(100),
    address_line1 VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    blood_type VARCHAR(10),
    medical_conditions TEXT,
    status VARCHAR(50) NOT NULL,
    preferred_language VARCHAR(10) DEFAULT 'en',
    marketing_consent BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),
    email_verified BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_branch_member_email UNIQUE (branch_id, email)
);
```

### Membership Plans Table
```sql
CREATE TABLE membership_plans (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    branch_id UUID NOT NULL,
    facility_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    plan_type VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    billing_cycle VARCHAR(50) NOT NULL,
    duration_months INTEGER NOT NULL,
    max_bookings_per_month INTEGER,
    max_concurrent_bookings INTEGER,
    court_booking_discount DECIMAL(5,2),
    has_court_access BOOLEAN DEFAULT TRUE,
    has_class_access BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    is_visible BOOLEAN DEFAULT TRUE,
    max_members INTEGER,
    current_members INTEGER DEFAULT 0,
    setup_fee DECIMAL(10,2),
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_branch_plan_name UNIQUE (branch_id, name)
);
```

### Memberships Table
```sql
CREATE TABLE memberships (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    member_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    facility_id UUID NOT NULL,
    membership_number VARCHAR(50) UNIQUE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    price_paid DECIMAL(10,2) NOT NULL,
    setup_fee_paid DECIMAL(10,2),
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(50),
    payment_reference VARCHAR(255),
    paid_at TIMESTAMP,
    auto_renew BOOLEAN DEFAULT FALSE,
    next_billing_date DATE,
    bookings_used INTEGER DEFAULT 0,
    guest_passes_used INTEGER DEFAULT 0,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_membership_number UNIQUE (membership_number)
);
```

### Discounts Table
```sql
CREATE TABLE discounts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    facility_id UUID NOT NULL,
    branch_id UUID,
    code VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3),
    application_method VARCHAR(30) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    max_total_usage INTEGER,
    max_usage_per_member INTEGER,
    current_usage_count INTEGER DEFAULT 0,
    min_purchase_amount DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_discount_code_facility UNIQUE (code, facility_id)
);
```

## Business Rules

### Member Registration
1. Email must be unique within branch
2. Member number auto-generated if not provided
3. Status defaults to ACTIVE
4. Email verification recommended but not required
5. Phone number required for emergency contact

### Membership Purchase
1. Member must be ACTIVE status
2. Plan must be available (active, visible, not at capacity)
3. Start date cannot be in the past
4. End date calculated from start date + duration
5. Membership number auto-generated (e.g., "MSH-{timestamp}-{random}")
6. First payment includes setup fee (if applicable)
7. Discount codes validated before application

### Discount Application
1. Discount must be currently valid (within date range and active)
2. Discount must not have reached usage limit
3. Discount must be applicable to selected plan
4. Member must not have exceeded per-member usage limit
5. Purchase amount must meet minimum requirement
6. Discount amount cannot exceed maximum cap

### Auto-Renewal
1. Renewal attempted 1 day before expiration
2. Payment processed using saved payment method
3. Renewal notification sent 7 days before expiration
4. Failed renewals trigger notification
5. Members can disable auto-renewal anytime

### Booking Limits
1. Monthly limits reset on calendar month basis
2. Concurrent booking limits checked at booking time
3. Membership benefits apply automatically
4. Expired memberships cannot make bookings

## Common Usage Patterns

### Member Registration and Membership Purchase

```kotlin
// 1. Register new member
val memberRequest = MemberCreateRequest(
    firstName = "John",
    lastName = "Doe",
    email = "john@example.com",
    phoneNumber = "+1-555-0123",
    dateOfBirth = LocalDate.of(1990, 5, 15),
    emergencyContactName = "Jane Doe",
    emergencyContactPhone = "+1-555-0124"
)
val member = memberService.createMember(memberRequest)

// 2. Browse available plans
val availablePlans = membershipPlanService.getAvailablePlans(branchId)

// 3. Purchase membership with discount
val purchaseRequest = MembershipCreateRequest(
    memberId = member.id,
    planId = selectedPlanId,
    startDate = LocalDate.now(),
    paymentMethod = "CREDIT_CARD",
    discountCode = "WELCOME20",
    autoRenew = true
)
val membership = membershipService.createMembership(purchaseRequest)
```

### Apply and Manage Discounts

```kotlin
// Create seasonal discount
val discount = discountService.createDiscount(DiscountCreateRequest(
    code = "SUMMER2025",
    name = "Summer Special",
    discountType = DiscountType.PERCENTAGE,
    value = BigDecimal("20.00"),
    applicationMethod = DiscountApplicationMethod.CODE,
    scope = DiscountScope.ALL_PLANS,
    validFrom = LocalDate.of(2025, 6, 1),
    validUntil = LocalDate.of(2025, 8, 31),
    maxTotalUsage = 100,
    maxUsagePerMember = 1
))

// Validate discount before purchase
val validation = discountService.validateDiscount(
    code = "SUMMER2025",
    planId = planId,
    memberId = memberId
)

if (validation.isValid) {
    val finalPrice = validation.finalPrice
    // Proceed with purchase
}
```

### Check Membership Benefits

```kotlin
// Check if member can make booking
val membership = membershipService.getActiveMembershipByMember(memberId)

if (membership != null && membership.canMakeBooking()) {
    // Create booking with membership benefits
    val bookingRequest = BookingCreateRequest(
        memberId = memberId,
        courtId = courtId,
        membershipId = membership.id,  // Link to membership
        // ... other booking details
    )

    // Booking service will automatically:
    // - Apply court booking discount from plan
    // - Check concurrent booking limits
    // - Increment bookings_used counter
}
```

### Handle Expiring Memberships

```kotlin
// Find memberships expiring in 7 days
val expiringMemberships = membershipService.findExpiringMemberships(7)

expiringMemberships.forEach { membership ->
    // Send renewal reminder
    notificationService.sendMembershipRenewalReminder(membership)

    // Mark reminder sent
    membershipService.markRenewalReminderSent(membership.id)
}

// Auto-renew eligible memberships
val autoRenewMemberships = membershipService.findMembershipsForAutoRenewal()

autoRenewMemberships.forEach { membership ->
    try {
        membershipService.processAutoRenewal(membership)
    } catch (e: PaymentException) {
        notificationService.sendRenewalFailureNotification(membership)
    }
}
```

## Error Handling

**Common Exceptions:**

- `EntityNotFoundException`: Member, plan, membership, or discount not found
- `IllegalStateException`:
  - Plan not available (inactive, at capacity, hidden)
  - Member status not ACTIVE
  - Membership already exists for member/plan
  - Discount usage limit reached
  - Cannot cancel expired membership
  - Membership status invalid for operation
- `ValidationException`:
  - Invalid discount code
  - Discount not applicable to plan
  - Member exceeded per-member discount limit
  - Purchase amount below minimum
  - Email already registered
  - Invalid date range (start after end)

## Testing Strategy

**Unit Tests:**
- Membership expiration logic
- Discount calculation accuracy
- Usage limit enforcement
- Price calculations with discounts
- Auto-renewal eligibility

**Integration Tests:**
- Complete purchase flow with discount
- Auto-renewal processing
- Member status changes
- Booking limit enforcement
- Email notification sending

**Test Scenarios:**
```kotlin
@Test
fun `should apply percentage discount correctly`()

@Test
fun `should prevent discount usage beyond limit`()

@Test
fun `should enforce monthly booking limit from membership plan`()

@Test
fun `should auto-renew membership before expiration`()

@Test
fun `should prevent booking when membership expired`()

@Test
fun `should calculate first payment with setup fee`()
```

## Future Enhancements

**Potential Features:**
1. **Family Memberships:** Add multiple members to single membership
2. **Freeze Membership:** Pause membership for vacation/medical reasons
3. **Upgrade/Downgrade:** Change plans mid-subscription
4. **Referral Program:** Discount for referring new members
5. **Loyalty Points:** Earn points for bookings and activities
6. **Member Portal:** Self-service profile and membership management
7. **Payment Plans:** Installment payment options
8. **Gift Memberships:** Purchase membership for someone else
9. **Corporate Billing:** Bill company instead of individual

## See Also

- [Booking Module](../booking/README.md) - Court booking with membership benefits
- [Payment Module](../../../payment/README.md) - Payment processing
- [Notification Module](../../../shared/notification/README.md) - Member communications
- [ARCHITECTURE.md](../../../../../ARCHITECTURE.md) - Overall system architecture
