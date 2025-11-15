# Court Booking System

## Overview

The **Court Booking System** enables sports facilities to manage court reservations for their members. It handles the complete booking lifecycle from availability checking to cancellation, including payment tracking, membership benefits, and automated notifications.

## Module Structure

```
facility/booking/
├── controller/
│   ├── BookingController.kt      # Booking REST API endpoints
│   └── CourtController.kt         # Court management endpoints
├── data/
│   ├── BookingRepository.kt       # Booking data access
│   └── CourtRepository.kt         # Court data access
├── domain/
│   ├── Booking.kt                 # Booking entity with business logic
│   └── Court.kt                   # Court entity with configuration
├── dto/
│   ├── BookingDto.kt              # Booking request/response DTOs
│   └── CourtDto.kt                # Court request/response DTOs
└── service/
    ├── BookingService.kt          # Core booking business logic
    ├── CourtService.kt            # Court management logic
    └── BookingEmailService.kt     # Email notifications
```

## Domain Models

### Court

Represents a bookable resource at a facility branch (tennis court, padel court, basketball court, etc.).

**Key Attributes:**
- `name`: Court identifier (e.g., "Tennis Court 1")
- `courtType`: Type of court (TENNIS, PADEL, SQUASH, BADMINTON, BASKETBALL, VOLLEYBALL, FOOTBALL, MULTIPURPOSE)
- `hourlyRate`: Base hourly rental rate
- `peakHourRate`: Optional higher rate for peak hours (evenings, weekends)
- `minBookingDuration`: Minimum booking duration in minutes (default: 60)
- `maxBookingDuration`: Maximum booking duration in minutes (default: 120)
- `bookingInterval`: Time slot intervals in minutes (default: 30)
- `advanceBookingDays`: How far in advance bookings can be made (default: 14 days)
- `cancellationHours`: Minimum hours before start time for cancellation (default: 24)
- `isIndoor`: Whether court is indoor or outdoor
- `hasLighting`: Whether court has lighting for night play
- `surfaceType`: Court surface (e.g., "Clay", "Grass", "Hard", "Artificial")
- `maxPlayers`: Maximum number of players allowed
- `status`: Court status (ACTIVE, MAINTENANCE, INACTIVE, RETIRED)

**Business Methods:**
```kotlin
court.isAvailableForBooking()  // Check if court can be booked
court.getDisplayName()          // Get formatted display name with type
```

### Booking

Represents a court reservation made by a member.

**Key Attributes:**
- `bookingNumber`: Unique booking identifier (e.g., "BKG-1234567890-5432")
- `member`: The member who made the booking
- `court`: The reserved court
- `branch`: The facility branch where court is located
- `facility`: The sport facility
- `membership`: Optional linked membership for benefits
- `bookingDate`: Date of the booking
- `startTime`: Booking start date and time
- `endTime`: Booking end date and time
- `durationMinutes`: Booking duration in minutes
- `status`: Booking status (PENDING, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED)
- `originalPrice`: Base price before discounts
- `discountAmount`: Total discounts applied
- `finalPrice`: Final price after discounts
- `paymentStatus`: Payment status (PENDING, PAID, PARTIALLY_PAID, REFUNDED, FAILED)
- `paymentMethod`: Payment method used
- `numberOfPlayers`: Number of players
- `specialRequests`: Any special requests from member

**Business Methods:**
```kotlin
booking.isActive()                              // Check if booking is active
booking.isPast()                                // Check if booking is in the past
booking.isUpcoming()                            // Check if booking is upcoming
booking.canBeCancelled(cancellationHours)       // Check if booking can be cancelled
booking.cancel(reason, cancelledBy, refundAmount)  // Cancel the booking
booking.checkIn()                               // Check-in for booking
booking.checkOut()                              // Check-out from booking
booking.complete()                              // Mark as completed
booking.markAsNoShow()                          // Mark as no-show
```

## Key Features

### 1. Booking Creation

**Workflow:**
1. Member selects court, date, and time
2. System validates:
   - Court exists and is available (status = ACTIVE)
   - Booking duration within court's min/max limits
   - No overlapping bookings for the court
   - Member doesn't have overlapping bookings (prevent double-booking)
   - Membership limits if applicable (monthly bookings, concurrent bookings)
3. Calculate pricing:
   - Apply peak hour pricing if applicable (weekday evenings 5-10 PM, all day weekends)
   - Apply membership discount if member has active membership
4. Generate unique booking number
5. Create and save booking
6. Send confirmation email asynchronously

**Business Rules:**
- Bookings must respect court's `minBookingDuration` and `maxBookingDuration`
- No overlapping bookings for the same court
- Members cannot have overlapping bookings (prevent double-booking)
- Membership booking limits are enforced:
  - `maxBookingsPerMonth`: Monthly booking limit per membership
  - `maxConcurrentBookings`: Maximum concurrent active bookings
- Peak hour pricing applies:
  - **Weekdays:** 5 PM - 10 PM
  - **Weekends:** All day (Saturday & Sunday)

### 2. Availability Checking

**Features:**
- Check if specific time slot is available
- Get all available time slots for a court on a specific date
- View conflicting bookings if slot is unavailable

**Time Slot Generation:**
- Generates slots from 8 AM to 10 PM
- Slot duration = court's `bookingInterval` (default 30 minutes)
- Each slot includes availability status and pricing

### 3. Booking Modifications

**Update Booking:**
- Change start time (validates no overlaps)
- Change duration (validates min/max limits, recalculates price)
- Update number of players
- Add/modify special requests or notes
- Only CONFIRMED bookings can be updated

### 4. Booking Cancellation

**Cancellation Policy:**
- Only CONFIRMED bookings can be cancelled
- Must cancel at least `cancellationHours` before start time (default: 24 hours)
- Full refund if cancelled within policy
- Cancellation email sent automatically

**Refund Logic:**
- If `paymentStatus = PAID`: Full refund applied
- Otherwise: No refund
- Payment status updated to REFUNDED

### 5. Check-In/Check-Out

**Check-In:**
- Marks member as present at the facility
- Status changes: CONFIRMED → CHECKED_IN
- Timestamp recorded

**Check-Out:**
- Marks booking as completed
- Status changes: CHECKED_IN → COMPLETED
- Timestamp recorded

### 6. No-Show Tracking

**No-Show:**
- Staff can mark booking as NO_SHOW if member doesn't appear
- Only CONFIRMED bookings can be marked as no-show
- No-show notification email sent
- Can be used for analytics or membership policy enforcement

## API Endpoints

**Base Path:** `/api/v1/facility/bookings`

### Create Booking
```http
POST /api/v1/facility/bookings
Content-Type: application/json

{
  "memberId": "uuid",
  "courtId": "uuid",
  "bookingDate": "2025-11-20",
  "startTime": "2025-11-20T18:00:00",
  "durationMinutes": 90,
  "numberOfPlayers": 2,
  "membershipId": "uuid (optional)",
  "additionalPlayers": "John Doe, Jane Smith (optional)",
  "specialRequests": "Need extra balls (optional)",
  "paymentMethod": "CREDIT_CARD (optional)"
}

Response (201 Created):
{
  "id": "uuid",
  "bookingNumber": "BKG-1700504400000-1234",
  "member": { ... },
  "court": { ... },
  "bookingDate": "2025-11-20",
  "startTime": "2025-11-20T18:00:00",
  "endTime": "2025-11-20T19:30:00",
  "durationMinutes": 90,
  "status": "CONFIRMED",
  "originalPrice": 45.00,
  "discountAmount": 4.50,
  "finalPrice": 40.50,
  "currency": "USD",
  "paymentStatus": "PENDING"
}
```

### Get Booking by ID
```http
GET /api/v1/facility/bookings/{id}

Response (200 OK):
{
  "id": "uuid",
  "bookingNumber": "BKG-1700504400000-1234",
  ...
}
```

### Get Booking by Number
```http
GET /api/v1/facility/bookings/by-number/BKG-1700504400000-1234

Response (200 OK):
{
  "id": "uuid",
  "bookingNumber": "BKG-1700504400000-1234",
  ...
}
```

### Update Booking
```http
PUT /api/v1/facility/bookings/{id}
Content-Type: application/json

{
  "startTime": "2025-11-20T19:00:00 (optional)",
  "durationMinutes": 120 (optional),
  "numberOfPlayers": 4 (optional),
  "additionalPlayers": "Updated list (optional)",
  "specialRequests": "Updated requests (optional)",
  "notes": "Staff notes (optional)"
}

Response (200 OK):
{
  "id": "uuid",
  "bookingNumber": "BKG-1700504400000-1234",
  ...
}
```

### Cancel Booking
```http
POST /api/v1/facility/bookings/{id}/cancel
Content-Type: application/json

{
  "reason": "Member requested cancellation due to weather"
}

Response (200 OK):
{
  "id": "uuid",
  "status": "CANCELLED",
  "cancelledAt": "2025-11-19T10:30:00",
  "cancellationReason": "Member requested cancellation due to weather",
  "refundAmount": 40.50
}
```

### Check-In
```http
POST /api/v1/facility/bookings/{id}/check-in

Response (200 OK):
{
  "id": "uuid",
  "status": "CHECKED_IN",
  "checkedInAt": "2025-11-20T17:55:00"
}
```

### Check-Out
```http
POST /api/v1/facility/bookings/{id}/check-out

Response (200 OK):
{
  "id": "uuid",
  "status": "COMPLETED",
  "checkedOutAt": "2025-11-20T19:35:00"
}
```

### Mark as No-Show
```http
POST /api/v1/facility/bookings/{id}/no-show

Response (200 OK):
{
  "id": "uuid",
  "status": "NO_SHOW",
  "statusChangedAt": "2025-11-20T18:15:00"
}
```

### Check Availability
```http
POST /api/v1/facility/bookings/check-availability
Content-Type: application/json

{
  "courtId": "uuid",
  "startTime": "2025-11-20T18:00:00",
  "durationMinutes": 90
}

Response (200 OK):
{
  "isAvailable": false,
  "courtId": "uuid",
  "courtName": "Tennis Court 1",
  "startTime": "2025-11-20T18:00:00",
  "endTime": "2025-11-20T19:30:00",
  "conflictingBookings": [
    {
      "id": "uuid",
      "bookingNumber": "BKG-1700504400000-5678",
      "startTime": "2025-11-20T17:30:00",
      "endTime": "2025-11-20T19:00:00"
    }
  ]
}
```

### Get Available Slots
```http
GET /api/v1/facility/bookings/available-slots?courtId=uuid&date=2025-11-20

Response (200 OK):
[
  {
    "startTime": "2025-11-20T08:00:00",
    "endTime": "2025-11-20T08:30:00",
    "isAvailable": true,
    "price": 25.00
  },
  {
    "startTime": "2025-11-20T08:30:00",
    "endTime": "2025-11-20T09:00:00",
    "isAvailable": true,
    "price": 25.00
  },
  {
    "startTime": "2025-11-20T09:00:00",
    "endTime": "2025-11-20T09:30:00",
    "isAvailable": false,
    "price": 25.00
  },
  ...
]
```

### Get Bookings by Member
```http
GET /api/v1/facility/bookings/by-member/{memberId}?page=0&size=20&sort=startTime,desc

Response (200 OK):
{
  "content": [ ... ],
  "totalElements": 45,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

### Get Upcoming Bookings by Member
```http
GET /api/v1/facility/bookings/by-member/{memberId}/upcoming

Response (200 OK):
[
  {
    "id": "uuid",
    "bookingNumber": "BKG-1700504400000-1234",
    "startTime": "2025-11-20T18:00:00",
    "endTime": "2025-11-20T19:30:00",
    "court": { ... },
    "status": "CONFIRMED"
  },
  ...
]
```

### Search Bookings
```http
GET /api/v1/facility/bookings/search?branchId=uuid&status=CONFIRMED&startDate=2025-11-01&endDate=2025-11-30&page=0&size=20

Response (200 OK):
{
  "content": [ ... ],
  "totalElements": 125,
  "totalPages": 7,
  "size": 20,
  "number": 0
}
```

**Search Parameters:**
- `branchId` (UUID, optional): Filter by facility branch
- `courtId` (UUID, optional): Filter by specific court
- `memberId` (UUID, optional): Filter by member
- `status` (BookingStatus, optional): Filter by booking status
- `startDate` (LocalDate, optional): Filter bookings from this date
- `endDate` (LocalDate, optional): Filter bookings until this date

### Get Bookings by Branch and Date
```http
GET /api/v1/facility/bookings/by-branch/{branchId}/by-date?date=2025-11-20

Response (200 OK):
[
  {
    "id": "uuid",
    "bookingNumber": "BKG-1700504400000-1234",
    ...
  },
  ...
]
```

### Get Today's Bookings by Branch
```http
GET /api/v1/facility/bookings/by-branch/{branchId}/today

Response (200 OK):
[
  {
    "id": "uuid",
    "bookingNumber": "BKG-1700504400000-1234",
    ...
  },
  ...
]
```

## Integration with Other Modules

### Membership Module

**Membership Benefits:**
- **Booking Discounts:** Memberships can provide percentage discounts on court bookings
- **Booking Limits:** Memberships enforce:
  - `maxBookingsPerMonth`: Monthly booking quota
  - `maxConcurrentBookings`: Maximum active bookings at once
- **Usage Tracking:** Each booking is tracked against membership

**Example:**
```kotlin
// Premium membership with benefits
membership.plan.courtBookingDiscount = 10  // 10% discount
membership.plan.maxBookingsPerMonth = 20   // 20 bookings per month
membership.plan.maxConcurrentBookings = 3  // Max 3 active bookings

// Discount applied automatically during booking creation
booking.originalPrice = 50.00
booking.discountAmount = 5.00  // 10% of 50.00
booking.finalPrice = 45.00
```

### Payment Module

**Payment Tracking:**
- `paymentStatus`: Tracks payment state
- `paymentMethod`: Records payment method used
- `paymentReference`: External payment gateway reference
- `paidAt`: Timestamp of successful payment
- Refunds tracked via `refundAmount`

### Notification Module

**Automated Emails:**
- **Booking Confirmation:** Sent when booking is created
- **Cancellation Notification:** Sent when booking is cancelled
- **No-Show Notification:** Sent when booking is marked as no-show
- **Reminder Notifications:** Can be sent before booking starts (flag: `reminderSent`)

## Database Schema

### Courts Table
```sql
CREATE TABLE courts (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    facility_id UUID NOT NULL REFERENCES sport_facilities(id),
    branch_id UUID NOT NULL REFERENCES facility_branches(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    court_type VARCHAR(50) NOT NULL,
    surface_type VARCHAR(50),
    is_indoor BOOLEAN DEFAULT FALSE,
    has_lighting BOOLEAN DEFAULT FALSE,
    max_players INTEGER DEFAULT 4,
    hourly_rate DECIMAL(10,2) NOT NULL,
    peak_hour_rate DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    min_booking_duration INTEGER DEFAULT 60,
    max_booking_duration INTEGER DEFAULT 120,
    booking_interval INTEGER DEFAULT 30,
    advance_booking_days INTEGER DEFAULT 14,
    cancellation_hours INTEGER DEFAULT 24,
    status VARCHAR(50) NOT NULL,
    maintenance_notes TEXT,
    amenities TEXT,
    display_order INTEGER DEFAULT 0,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version INTEGER DEFAULT 0,

    CONSTRAINT uk_branch_court_name UNIQUE (branch_id, name)
);

-- Indexes
CREATE INDEX idx_court_branch ON courts(branch_id);
CREATE INDEX idx_court_facility ON courts(facility_id);
CREATE INDEX idx_court_type ON courts(court_type);
CREATE INDEX idx_court_status ON courts(status);
CREATE INDEX idx_court_tenant ON courts(tenant_id);
```

### Bookings Table
```sql
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    member_id UUID NOT NULL REFERENCES members(id),
    court_id UUID NOT NULL REFERENCES courts(id),
    branch_id UUID NOT NULL REFERENCES facility_branches(id),
    facility_id UUID NOT NULL REFERENCES sport_facilities(id),
    membership_id UUID REFERENCES memberships(id),
    booking_number VARCHAR(50) NOT NULL UNIQUE,
    booking_date DATE NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    status_reason TEXT,
    status_changed_at TIMESTAMP,
    original_price DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    payment_reference VARCHAR(255),
    paid_at TIMESTAMP,
    number_of_players INTEGER DEFAULT 2,
    additional_players TEXT,
    special_requests TEXT,
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(255),
    cancellation_reason TEXT,
    refund_amount DECIMAL(10,2),
    checked_in_at TIMESTAMP,
    checked_out_at TIMESTAMP,
    notes TEXT,
    reminder_sent BOOLEAN DEFAULT FALSE,
    confirmation_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version INTEGER DEFAULT 0,

    CONSTRAINT uk_booking_number UNIQUE (booking_number)
);

-- Indexes
CREATE INDEX idx_booking_member ON bookings(member_id);
CREATE INDEX idx_booking_court ON bookings(court_id);
CREATE INDEX idx_booking_branch ON bookings(branch_id);
CREATE INDEX idx_booking_facility ON bookings(facility_id);
CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_booking_date ON bookings(booking_date);
CREATE INDEX idx_booking_start_time ON bookings(start_time);
CREATE INDEX idx_booking_number ON bookings(booking_number);
CREATE INDEX idx_booking_tenant ON bookings(tenant_id);
```

## Common Usage Patterns

### Complete Booking Flow

```kotlin
// 1. Check availability
val availabilityRequest = AvailabilityCheckRequest(
    courtId = courtId,
    startTime = LocalDateTime.of(2025, 11, 20, 18, 0),
    durationMinutes = 90
)
val availability = bookingService.checkAvailability(availabilityRequest)

if (availability.isAvailable) {
    // 2. Create booking
    val bookingRequest = BookingCreateRequest(
        memberId = memberId,
        courtId = courtId,
        bookingDate = LocalDate.of(2025, 11, 20),
        startTime = LocalDateTime.of(2025, 11, 20, 18, 0),
        durationMinutes = 90,
        numberOfPlayers = 2,
        membershipId = membershipId,  // Apply membership benefits
        paymentMethod = "CREDIT_CARD"
    )
    val booking = bookingService.createBooking(bookingRequest)

    // 3. Member arrives - check in
    bookingService.checkInBooking(booking.id)

    // 4. After play - check out
    bookingService.checkOutBooking(booking.id)
}
```

### Cancellation Flow

```kotlin
// Check if booking can be cancelled
val booking = bookingService.getBookingById(bookingId)
if (booking.canBeCancelled(24)) {  // 24 hours policy
    val cancelRequest = BookingCancelRequest(
        reason = "Member requested cancellation - weather forecast is bad"
    )
    val cancelledBooking = bookingService.cancelBooking(bookingId, cancelRequest)

    // Refund processed automatically if payment was made
    println("Refund amount: ${cancelledBooking.refundAmount}")
}
```

### Get Member's Schedule

```kotlin
// Get all upcoming bookings
val upcomingBookings = bookingService.getUpcomingBookingsByMember(memberId)

// Get booking history with pagination
val bookingHistory = bookingService.getBookingsByMember(
    memberId = memberId,
    pageable = PageRequest.of(0, 20, Sort.by("startTime").descending())
)
```

### Daily Schedule for Branch

```kotlin
// Get today's schedule
val todaysBookings = bookingService.getTodaysBookingsByBranch(branchId)

// Get specific date schedule
val dateBookings = bookingService.getBookingsByBranchAndDate(
    branchId = branchId,
    date = LocalDate.of(2025, 11, 20)
)
```

## Error Handling

**Common Exceptions:**

- `EntityNotFoundException`: Booking, court, or member not found
- `IllegalStateException`:
  - Court not available for booking (status != ACTIVE)
  - Court already booked for time slot
  - Member has overlapping booking
  - Monthly booking limit reached
  - Concurrent booking limit reached
  - Booking cannot be cancelled (too close to start time)
  - Invalid booking status for operation
- `IllegalArgumentException`:
  - Duration below minimum or above maximum
  - Invalid booking number format
  - Membership doesn't belong to member

## Performance Considerations

**Optimizations:**
- All queries use indexed fields (`tenant_id`, `member_id`, `court_id`, `booking_date`, `start_time`)
- Lazy loading for relationships to prevent N+1 queries
- Pagination support for list endpoints
- Email sending is asynchronous (doesn't block booking creation)

**Database Indexes:**
- Tenant isolation: `idx_booking_tenant`
- Member lookups: `idx_booking_member`
- Court availability: `idx_booking_court`, `idx_booking_start_time`
- Status filtering: `idx_booking_status`
- Date range queries: `idx_booking_date`
- Unique booking numbers: `uk_booking_number`

## Testing Strategy

**Unit Tests:**
- Business logic in `Booking` entity methods
- Service layer validation and pricing logic
- Peak hour detection
- Booking number generation

**Integration Tests:**
- Complete booking flow with database
- Overlapping booking detection
- Membership limit enforcement
- Cancellation policy enforcement
- Email notification sending

**Test Scenarios:**
```kotlin
// Test overlapping booking prevention
@Test
fun `should prevent overlapping bookings for same court`()

// Test membership discount application
@Test
fun `should apply membership discount when creating booking`()

// Test cancellation policy
@Test
fun `should prevent cancellation within 24 hours of start time`()

// Test peak hour pricing
@Test
fun `should apply peak hour rate for weekend bookings`()

// Test concurrent booking limit
@Test
fun `should enforce concurrent booking limit from membership plan`()
```

## Future Enhancements

**Potential Features:**
1. **Recurring Bookings:** Weekly/monthly recurring bookings
2. **Waitlist:** Automatic notification when slot becomes available
3. **Dynamic Pricing:** Demand-based pricing
4. **Equipment Rental:** Link equipment rentals to bookings
5. **Group Bookings:** Book multiple courts simultaneously
6. **Tournament Scheduling:** Special tournament booking flows
7. **Booking Recommendations:** AI-powered booking suggestions based on member preferences
8. **Multi-Court Events:** Coordinated bookings across multiple courts

## See Also

- [Membership Module](../membership/README.md) - Membership management and benefits
- [Payment Module](../../../payment/README.md) - Payment processing integration
- [Notification Module](../../../shared/notification/README.md) - Email and SMS notifications
- [ARCHITECTURE.md](../../../../../ARCHITECTURE.md) - Overall system architecture
