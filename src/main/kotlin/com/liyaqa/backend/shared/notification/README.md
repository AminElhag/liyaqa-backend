# Notification System

## Overview

The **Notification System** provides a comprehensive multi-channel communication infrastructure for customer engagement. It supports Email, SMS, Push Notifications, and In-App messages with template management, user preferences, delivery tracking, and retry logic.

**Philosophy:** *"Notifications turn transactions into relationships."*

## Module Structure

```
shared/notification/
├── controller/
│   └── NotificationController.kt           # Notification management API
├── data/
│   ├── NotificationRepository.kt            # Notification data access
│   ├── NotificationTemplateRepository.kt    # Template data access
│   └── NotificationPreferenceRepository.kt  # User preference data access
├── domain/
│   ├── Notification.kt                      # Notification entity
│   ├── NotificationTemplate.kt              # Template entity
│   └── NotificationPreference.kt            # User preference entity
└── service/
    ├── NotificationService.kt               # Core orchestration logic
    └── channel/
        ├── NotificationChannelProvider.kt   # Channel abstraction
        ├── EmailChannelProvider.kt          # Email delivery (SMTP)
        ├── SmsChannelProvider.kt            # SMS delivery (Twilio, etc.)
        ├── PushChannelProvider.kt           # Push notifications (Firebase)
        └── InAppChannelProvider.kt          # In-app notifications
```

## Architecture

### Strategy Pattern for Channels

```
┌───────────────────────┐
│ NotificationService   │
└───────────┬───────────┘
            │
            │ uses
            ▼
┌─────────────────────────────┐
│ NotificationChannelProvider │ (interface)
└────────────┬────────────────┘
             │
             │ implemented by
             ▼
┌────────────────────────────────┐
│ EmailChannelProvider           │
│ SmsChannelProvider             │
│ PushChannelProvider            │
│ InAppChannelProvider           │
└────────────────────────────────┘
```

**Benefits:**
- Easy to add new channels
- Consistent delivery interface
- Testable with mocks
- Channel-specific retry logic

### Notification Flow

```
1. Business Logic
   ↓
2. NotificationService.sendFromTemplate()
   ↓
3. Check User Preferences
   ↓
4. Render Template with Variables
   ↓
5. Create Notification Record
   ↓
6. Route to Channel Provider
   ↓
7. Deliver via External Provider (SendGrid, Twilio, Firebase)
   ↓
8. Update Delivery Status
   ↓
9. Retry on Failure (if applicable)
```

## Domain Models

### 1. Notification

Tracks all communications across all channels.

**Key Attributes:**

**Recipient:**
- `recipientType`: Recipient type (MEMBER, EMPLOYEE, FACILITY_ADMIN)
- `recipientId`: UUID of recipient
- `recipientEmail`: Email address
- `recipientPhone`: Phone number
- `recipientName`: Display name

**Notification Details:**
- `type`: Notification type (BOOKING_CONFIRMATION, PAYMENT_RECEIPT, MEMBERSHIP_RENEWAL, etc.)
- `channel`: Delivery channel (EMAIL, SMS, PUSH, IN_APP)
- `priority`: Priority level (LOW, MEDIUM, HIGH, URGENT)
- `subject`: Subject line (email/push)
- `message`: Plain text message
- `htmlContent`: HTML content (email)

**Template Info:**
- `templateId`: Template code used
- `templateVariables`: Variables passed to template (JSON)

**Delivery Status:**
- `status`: Status (PENDING, SENT, DELIVERED, READ, FAILED, EXPIRED)
- `sentAt`: When sent to provider
- `deliveredAt`: When delivered to recipient
- `readAt`: When read by recipient (if tracked)
- `failedAt`: When delivery failed
- `errorMessage`: Error details
- `retryCount`: Number of retry attempts

**Scheduling:**
- `scheduledAt`: When to send (optional, null = immediate)
- `expiresAt`: Expiration time (optional)

**Context:**
- `contextType`: Related entity type (e.g., "booking", "payment")
- `contextId`: Related entity ID
- `metadata`: Additional data (JSON)

**Provider:**
- `provider`: External provider (e.g., "sendgrid", "twilio", "firebase")
- `providerMessageId`: Provider's message ID
- `providerResponse`: Provider response (JSON)

**User Interaction:**
- `clickTracked`: Link click tracked
- `clickedAt`: When user clicked
- `unsubscribed`: User unsubscribed from this type

**Business Methods:**
```kotlin
notification.markAsSent(providerMessageId)
notification.markAsDelivered()
notification.markAsFailed(errorMessage)
notification.markAsRead()
notification.canRetry()                    // Check if retry is possible
notification.incrementRetryCount()
notification.isExpired()                   // Check if expired
```

### 2. NotificationTemplate

Reusable templates for common notification types.

**Key Attributes:**
- `templateCode`: Unique identifier (e.g., "BOOKING_CONFIRMATION")
- `name`: Human-readable name
- `description`: Template description
- `notificationType`: Associated notification type
- `channel`: Default channel
- `subjectTemplate`: Subject template with variables
- `bodyTemplate`: Body template with variables
- `htmlTemplate`: HTML template (for email)
- `variables`: Required variables (JSON array)
- `isActive`: Template is active
- `maxRetries`: Maximum retry attempts
- `locale`: Language/locale (e.g., "en", "ar")

**Template Rendering:**
```kotlin
template.renderSubject(variables)         // Render subject with variables
template.renderBody(variables)            // Render body with variables
template.renderHtml(variables)            // Render HTML with variables
```

**Template Variable Syntax:**
```
Hello {{memberName}},

Your booking at {{facilityName}} has been confirmed.

Court: {{courtName}}
Date: {{bookingDate}}
Time: {{startTime}} - {{endTime}}
Total: {{finalPrice}} {{currency}}

Booking Number: {{bookingNumber}}
```

### 3. NotificationPreference

User preferences for receiving notifications.

**Key Attributes:**
- `recipientType`: Recipient type
- `recipientId`: Recipient UUID
- `email`: Contact email
- `phoneNumber`: Contact phone
- `emailEnabled`: Receive email notifications
- `smsEnabled`: Receive SMS notifications
- `pushEnabled`: Receive push notifications
- `inAppEnabled`: Receive in-app notifications
- `marketingEnabled`: Receive marketing communications
- `bookingNotifications`: Receive booking-related notifications
- `paymentNotifications`: Receive payment-related notifications
- `reminderNotifications`: Receive reminders
- `promotionalNotifications`: Receive promotional offers
- `timezone`: User timezone for scheduling

**Business Methods:**
```kotlin
preferences.shouldReceive(channel, type, priority)  // Check if should send
preferences.enableChannel(channel)
preferences.disableChannel(channel)
preferences.enableNotificationType(type)
preferences.disableNotificationType(type)
```

## Key Features

### 1. Template-Based Notifications

**Create Template:**
```kotlin
val template = NotificationTemplate(
    templateCode = "BOOKING_CONFIRMATION",
    name = "Booking Confirmation Email",
    notificationType = NotificationType.BOOKING_CONFIRMATION,
    channel = NotificationChannel.EMAIL,
    subjectTemplate = "Booking Confirmed - {{courtName}}",
    bodyTemplate = """
        Hello {{memberName}},

        Your booking has been confirmed!

        Court: {{courtName}}
        Date: {{bookingDate}}
        Time: {{startTime}} - {{endTime}}

        Booking Number: {{bookingNumber}}
    """.trimIndent(),
    variables = listOf("memberName", "courtName", "bookingDate", "startTime", "endTime", "bookingNumber"),
    maxRetries = 3,
    locale = "en"
)
```

**Send Using Template:**
```kotlin
notificationService.sendFromTemplate(
    tenantId = tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = member.id,
    templateCode = "BOOKING_CONFIRMATION",
    variables = mapOf(
        "memberName" to member.getFullName(),
        "courtName" to booking.court.name,
        "bookingDate" to booking.bookingDate.toString(),
        "startTime" to booking.startTime.toString(),
        "endTime" to booking.endTime.toString(),
        "bookingNumber" to booking.bookingNumber
    ),
    contextType = "booking",
    contextId = booking.id
)
```

### 2. Multi-Channel Delivery

**Email (SMTP):**
- HTML and plain text support
- Attachment support
- CC/BCC support
- Email tracking (opens, clicks)

**SMS (Twilio/other providers):**
- Short message delivery
- Link shortening
- Delivery receipts
- Cost tracking

**Push Notifications (Firebase):**
- Mobile app notifications
- Rich notifications with images
- Action buttons
- Badge counts

**In-App:**
- Notification center
- Real-time delivery
- Read/unread tracking
- Action support

### 3. User Preference Management

**Opt-In/Opt-Out:**
- Channel-level control (email, SMS, push, in-app)
- Type-level control (booking, payment, marketing, etc.)
- Priority-based filtering (only urgent, medium+, all)

**Do Not Disturb:**
- Quiet hours configuration
- Timezone-aware scheduling
- Emergency override for urgent notifications

### 4. Delivery Tracking

**Status Lifecycle:**
```
PENDING → SENT → DELIVERED → READ
          ↓
        FAILED (can retry)
          ↓
        EXPIRED (max retries or timeout)
```

**Metrics Tracked:**
- Delivery rate by channel
- Open rate (email, push)
- Click-through rate
- Failure rate
- Average delivery time
- Unsubscribe rate

### 5. Retry Logic

**Automatic Retries:**
- Exponential backoff (1min, 5min, 15min)
- Maximum retry attempts (default: 3)
- Permanent vs temporary failures
- Provider-specific retry strategies

**Retry Conditions:**
- Network errors → Retry
- Rate limiting → Retry with backoff
- Invalid recipient → Don't retry
- Authentication error → Don't retry

### 6. Scheduled Notifications

**Future Delivery:**
```kotlin
notificationService.sendFromTemplate(
    // ... other params
    scheduledAt = booking.startTime.minus(1, ChronoUnit.HOURS)  // 1 hour before
)
```

**Batch Processing:**
- Scheduled job processes pending notifications
- Respects user timezone
- Handles timezone conversions
- Processes in batches for efficiency

## Integration with Other Modules

### Booking Module

**Booking Notifications:**
- Booking confirmation
- Booking reminder (1 hour before, 1 day before)
- Cancellation confirmation
- No-show notification

**Example:**
```kotlin
// Send confirmation after booking
notificationService.sendFromTemplate(
    tenantId = booking.tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = booking.member.id,
    templateCode = "BOOKING_CONFIRMATION",
    variables = mapOf(
        "memberName" to booking.member.getFullName(),
        "courtName" to booking.court.name,
        "bookingNumber" to booking.bookingNumber,
        // ... other variables
    ),
    contextType = "booking",
    contextId = booking.id
)

// Schedule reminder for 1 hour before
notificationService.sendFromTemplate(
    tenantId = booking.tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = booking.member.id,
    templateCode = "BOOKING_REMINDER",
    variables = variables,
    scheduledAt = booking.startTime.minus(1, ChronoUnit.HOURS)
)
```

### Membership Module

**Membership Notifications:**
- Welcome email on registration
- Email verification
- Membership purchase confirmation
- Renewal reminders (7 days, 3 days, 1 day before expiration)
- Expiration notice
- Auto-renewal confirmation/failure

### Payment Module

**Payment Notifications:**
- Payment receipt
- Payment failure notice
- Refund confirmation

## API Endpoints

**Base Path:** `/api/v1/notifications`

### Get Notifications for User
```http
GET /api/v1/notifications/by-recipient/{recipientId}?page=0&size=20

Response (200 OK):
{
  "content": [
    {
      "id": "uuid",
      "type": "BOOKING_CONFIRMATION",
      "channel": "EMAIL",
      "subject": "Booking Confirmed - Tennis Court 1",
      "message": "Your booking has been confirmed...",
      "status": "DELIVERED",
      "sentAt": "2025-11-15T10:30:00Z",
      "readAt": null,
      "contextType": "booking",
      "contextId": "uuid"
    }
  ],
  "totalElements": 45,
  "totalPages": 3
}
```

### Mark as Read
```http
POST /api/v1/notifications/{id}/mark-read

Response (200 OK):
{
  "id": "uuid",
  "status": "READ",
  "readAt": "2025-11-15T14:30:00Z"
}
```

### Get User Preferences
```http
GET /api/v1/notifications/preferences/{recipientId}

Response (200 OK):
{
  "emailEnabled": true,
  "smsEnabled": false,
  "pushEnabled": true,
  "inAppEnabled": true,
  "marketingEnabled": true,
  "bookingNotifications": true,
  "paymentNotifications": true
}
```

### Update Preferences
```http
PUT /api/v1/notifications/preferences/{recipientId}
Content-Type: application/json

{
  "emailEnabled": true,
  "smsEnabled": true,
  "marketingEnabled": false
}

Response (200 OK):
{
  "emailEnabled": true,
  "smsEnabled": true,
  "marketingEnabled": false
}
```

## Configuration

### Email Provider (SMTP)

```properties
# Email Configuration
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=${SENDGRID_API_KEY}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### SMS Provider (Twilio)

```properties
# Twilio Configuration
twilio.account.sid=${TWILIO_ACCOUNT_SID}
twilio.auth.token=${TWILIO_AUTH_TOKEN}
twilio.phone.number=+1234567890
```

### Push Notifications (Firebase)

```properties
# Firebase Configuration
firebase.credentials.path=config/firebase-credentials.json
```

## Database Schema

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    recipient_type VARCHAR(50) NOT NULL,
    recipient_id UUID NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    recipient_name VARCHAR(255),
    type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    message TEXT NOT NULL,
    html_content TEXT,
    template_id VARCHAR(100),
    template_variables TEXT,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    scheduled_at TIMESTAMP,
    expires_at TIMESTAMP,
    context_type VARCHAR(100),
    context_id UUID,
    metadata TEXT,
    provider VARCHAR(100),
    provider_message_id VARCHAR(255),
    provider_response TEXT,
    click_tracked BOOLEAN DEFAULT FALSE,
    clicked_at TIMESTAMP,
    unsubscribed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Indexes for efficient queries
CREATE INDEX idx_notification_recipient ON notifications(recipient_type, recipient_id);
CREATE INDEX idx_notification_tenant ON notifications(tenant_id, created_at);
CREATE INDEX idx_notification_status ON notifications(status, created_at);
CREATE INDEX idx_notification_channel ON notifications(channel, status);
CREATE INDEX idx_notification_scheduled ON notifications(scheduled_at, status);
```

## Common Usage Patterns

### Send Transactional Email

```kotlin
notificationService.send(SendNotificationRequest(
    tenantId = tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = member.id,
    recipientEmail = member.email,
    type = NotificationType.BOOKING_CONFIRMATION,
    channel = NotificationChannel.EMAIL,
    priority = NotificationPriority.HIGH,
    subject = "Your Booking is Confirmed",
    message = "Plain text message",
    htmlContent = "<html>Rich HTML content</html>",
    contextType = "booking",
    contextId = booking.id
))
```

### Send Multi-Channel Notification

```kotlin
// Send via email
notificationService.sendFromTemplate(
    tenantId = tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = member.id,
    templateCode = "PAYMENT_RECEIPT",
    channel = NotificationChannel.EMAIL,
    variables = variables
)

// Also send push notification
notificationService.sendFromTemplate(
    tenantId = tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = member.id,
    templateCode = "PAYMENT_RECEIPT_PUSH",
    channel = NotificationChannel.PUSH,
    variables = variables
)
```

### Schedule Reminder

```kotlin
// Send reminder 24 hours before booking
val reminderTime = booking.startTime.minus(24, ChronoUnit.HOURS)

notificationService.sendFromTemplate(
    tenantId = tenantId,
    recipientType = RecipientType.MEMBER,
    recipientId = member.id,
    templateCode = "BOOKING_REMINDER",
    variables = variables,
    scheduledAt = reminderTime
)
```

## Testing Strategy

**Unit Tests:**
- Template rendering logic
- Preference checking
- Retry logic
- Status transitions

**Integration Tests:**
- End-to-end notification delivery
- Channel provider integration
- Scheduled notification processing
- Retry mechanism

**Test Scenarios:**
```kotlin
@Test
fun `should send email notification successfully`()

@Test
fun `should respect user preferences and not send when disabled`()

@Test
fun `should retry failed notification with exponential backoff`()

@Test
fun `should render template with variables correctly`()

@Test
fun `should process scheduled notifications at correct time`()

@Test
fun `should track delivery status through full lifecycle`()
```

## See Also

- [Booking Module](../../facility/booking/README.md) - Booking notifications
- [Membership Module](../../facility/membership/README.md) - Membership communications
- [Payment Module](../../payment/README.md) - Payment receipts and confirmations
- [ARCHITECTURE.md](../../../../ARCHITECTURE.md) - Overall system architecture
