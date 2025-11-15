# Payment Processing System

## Overview

The **Payment Processing System** provides a gateway-agnostic payment infrastructure for handling bookings, memberships, and refunds. It uses the Strategy pattern to support multiple payment providers (currently Stripe) while maintaining a unified business API.

## Module Structure

```
payment/
├── config/
│   └── PaymentConfig.kt           # Payment gateway configuration
├── controller/
│   └── PaymentWebhookController.kt # Webhook endpoint for payment gateways
├── data/
│   └── PaymentTransactionRepository.kt # Transaction data access
├── domain/
│   └── PaymentTransaction.kt       # Payment transaction entity
├── gateway/
│   ├── PaymentGateway.kt           # Gateway abstraction interface
│   └── StripePaymentGateway.kt     # Stripe implementation
└── service/
    └── PaymentService.kt           # Payment orchestration logic
```

## Architecture

### Strategy Pattern

The payment system uses the Strategy pattern to abstract payment gateway implementations:

```
┌────────────────┐
│ PaymentService │
└───────┬────────┘
        │
        │ uses
        ▼
┌─────────────────┐
│ PaymentGateway  │ (interface)
└───────┬─────────┘
        │
        │ implemented by
        ▼
┌──────────────────────┐
│ StripePaymentGateway │
│ PayPalPaymentGateway │ (future)
│ SquarePaymentGateway │ (future)
└──────────────────────┘
```

**Benefits:**
- Easy to swap or add payment providers
- Business logic independent of gateway
- Testable with mocks
- Consistent error handling

### Payment Gateway Interface

```kotlin
interface PaymentGateway {
    fun getGatewayType(): PaymentGatewayType

    // Create payment intent (prepare payment)
    fun createPaymentIntent(request: CreatePaymentIntentRequest): PaymentResult<PaymentIntentResponse>

    // Capture authorized payment
    fun capturePayment(paymentIntentId: String): PaymentResult<PaymentResponse>

    // Cancel payment before capture
    fun cancelPaymentIntent(paymentIntentId: String): PaymentResult<Unit>

    // Process refund
    fun refundPayment(request: RefundRequest): PaymentResult<RefundResponse>

    // Get payment details
    fun getPaymentDetails(paymentIntentId: String): PaymentResult<PaymentResponse>

    // Webhook verification
    fun verifyWebhookSignature(payload: String, signature: String, secret: String): Boolean

    // Parse webhook event
    fun parseWebhookEvent(payload: String): PaymentWebhookEvent
}
```

## Domain Model

### PaymentTransaction

Tracks all payment transactions across different payment gateways.

**Key Attributes:**

**Relationships:**
- `booking`: Reference to booking (for booking payments)
- `membership`: Reference to membership (for membership payments)
- `member`: Member making the payment
- `branch`, `facility`: Location context

**Transaction Details:**
- `transactionNumber`: Unique internal transaction ID (e.g., "TXN-1700504400000-1234")
- `transactionType`: Type (PAYMENT, REFUND, AUTHORIZATION, VOID)
- `status`: Status (PENDING, AUTHORIZED, PROCESSING, COMPLETED, PARTIALLY_REFUNDED, REFUNDED, FAILED, CANCELED, EXPIRED)

**Gateway Information:**
- `gateway`: Payment gateway used (STRIPE, PAYPAL, SQUARE, BRAINTREE, TEST)
- `gatewayPaymentId`: Gateway-specific payment ID (e.g., Stripe payment_intent_id)
- `gatewayClientSecret`: Client secret for frontend payment completion

**Amount Information:**
- `amount`: Payment amount
- `currency`: Currency code (e.g., "USD")
- `originalAmount`: Original amount (for refunds)

**Payment Method Details:**
- `paymentMethod`: Method used (card, bank_transfer, wallet)
- `paymentBrand`: Card brand (visa, mastercard, amex)
- `lastFour`: Last 4 digits of card

**Timing:**
- `authorizedAt`: Authorization timestamp
- `capturedAt`: Capture/completion timestamp
- `failedAt`: Failure timestamp
- `refundedAt`: Refund timestamp

**Refund Information:**
- `refundAmount`: Amount refunded (supports partial refunds)
- `refundReason`: Reason for refund
- `parentTransaction`: Link to original payment (for refund transactions)

**Error Details:**
- `errorCode`: Gateway error code
- `errorMessage`: Error description
- `declineCode`: Card decline code (e.g., "insufficient_funds")

**Idempotency:**
- `idempotencyKey`: Prevents duplicate charges

**Business Methods:**
```kotlin
transaction.markAuthorized(gatewayPaymentId)
transaction.markCaptured(paymentMethod, brand, last4, receiptUrl)
transaction.markFailed(errorCode, errorMessage, declineCode)
transaction.markRefunded(refundAmount, reason)
transaction.canBeRefunded()                     // Check if refund is possible
transaction.getRemainingRefundableAmount()      // Get remaining refundable amount
transaction.isRefund()                          // Check if this is a refund
```

## Key Features

### 1. Payment Intent Creation

**Workflow:**
1. Service layer creates payment intent
2. Gateway generates payment intent and client secret
3. Transaction record created in database
4. Client secret returned to frontend
5. Frontend completes payment using Stripe.js/PayPal SDK
6. Webhook confirms payment

**Capture Methods:**
- **AUTOMATIC:** Charge immediately when payment method is provided
- **MANUAL:** Authorize first, capture later (useful for reservations)

### 2. Manual Capture

For MANUAL capture mode:
1. Create payment intent with `captureMethod = MANUAL`
2. Customer provides payment method (authorization only)
3. Later, explicitly capture the payment
4. Funds are transferred

**Use cases:**
- Verify customer before charging
- Capture at check-in time
- Adjust amount before capture

### 3. Refund Processing

**Full Refunds:**
```kotlin
refundPayment(
    transactionId = originalTransactionId,
    refundReason = RefundReason.BOOKING_CANCELLED
)
```

**Partial Refunds:**
```kotlin
refundPayment(
    transactionId = originalTransactionId,
    amount = partialAmount,
    refundReason = RefundReason.REQUESTED_BY_CUSTOMER
)
```

**Refund Reasons:**
- `REQUESTED_BY_CUSTOMER`
- `DUPLICATE`
- `FRAUDULENT`
- `BOOKING_CANCELLED`
- `FACILITY_CLOSED`
- `OTHER`

### 4. Webhook Handling

**Payment gateways send webhooks for:**
- Payment intent created
- Payment succeeded
- Payment failed
- Payment canceled
- Refund created
- Refund succeeded

**Webhook Flow:**
```
1. Gateway sends POST request to webhook endpoint
2. Verify webhook signature for security
3. Parse webhook payload
4. Update transaction status in database
5. Trigger business logic (e.g., confirm booking, activate membership)
6. Return 200 OK to gateway
```

**Security:**
- Webhook signature verification prevents tampering
- HTTPS required
- Idempotency prevents duplicate processing

### 5. Error Handling

**Result Pattern:**

Instead of throwing exceptions, payment operations return `PaymentResult`:

```kotlin
sealed class PaymentResult<out T> {
    data class Success<T>(val data: T) : PaymentResult<T>()
    data class Failure(val error: PaymentError) : PaymentResult<Nothing>()
}
```

**Benefits:**
- Explicit error handling
- Type-safe
- Forces error consideration
- No hidden exceptions

**Error Types:**
- `API_ERROR`: Gateway API error
- `AUTHENTICATION_ERROR`: Invalid API key
- `CARD_ERROR`: Card declined, insufficient funds
- `IDEMPOTENCY_ERROR`: Duplicate request
- `INVALID_REQUEST_ERROR`: Invalid parameters
- `RATE_LIMIT_ERROR`: Too many requests
- `NETWORK_ERROR`: Connection failure
- `UNKNOWN_ERROR`: Unexpected error

## Integration with Other Modules

### Booking Module

**Payment Flow:**
```kotlin
// 1. Create booking
val booking = bookingService.createBooking(bookingRequest)

// 2. Create payment intent
val paymentResult = paymentService.createBookingPayment(
    booking = booking,
    member = member,
    captureMethod = CaptureMethod.AUTOMATIC
)

// 3. Return client secret to frontend
if (paymentResult.success) {
    // Frontend completes payment with Stripe.js
    // Webhook confirms payment and updates booking status
}
```

### Membership Module

**Membership Payment:**
```kotlin
val paymentResult = paymentService.createMembershipPayment(
    membership = membership,
    member = member,
    includeSetupFee = true
)
```

**Auto-Renewal:**
```kotlin
// Process auto-renewal payment
val renewalResult = paymentService.processMembershipRenewal(
    membership = membership,
    savedPaymentMethod = member.savedPaymentMethodId
)
```

## API Usage

### Create Payment Intent

```http
POST /api/v1/payments/booking/{bookingId}
Content-Type: application/json

{
  "captureMethod": "AUTOMATIC"
}

Response (200 OK):
{
  "success": true,
  "transactionId": "uuid",
  "transactionNumber": "TXN-1700504400000-1234",
  "paymentIntentId": "pi_1234567890abcdef",
  "clientSecret": "pi_1234567890abcdef_secret_xyz",
  "amount": 50.00,
  "currency": "USD"
}
```

### Capture Payment

```http
POST /api/v1/payments/{transactionId}/capture

Response (200 OK):
{
  "success": true,
  "transactionNumber": "TXN-1700504400000-1234",
  "amount": 50.00,
  "receiptUrl": "https://pay.stripe.com/receipts/..."
}
```

### Refund Payment

```http
POST /api/v1/payments/{transactionId}/refund
Content-Type: application/json

{
  "amount": 50.00,
  "reason": "BOOKING_CANCELLED"
}

Response (200 OK):
{
  "success": true,
  "refundId": "uuid",
  "refundAmount": 50.00,
  "refundStatus": "SUCCEEDED"
}
```

### Webhook Endpoint

```http
POST /api/v1/payments/webhooks/stripe
Stripe-Signature: t=1234567890,v1=signature_hash
Content-Type: application/json

{
  "id": "evt_1234567890",
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      "id": "pi_1234567890abcdef",
      "amount": 5000,
      "currency": "usd",
      "status": "succeeded"
    }
  }
}

Response (200 OK):
{
  "received": true
}
```

## Configuration

### Environment Variables

```properties
# Stripe Configuration
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Payment Settings
PAYMENT_DEFAULT_CURRENCY=USD
PAYMENT_CAPTURE_METHOD=AUTOMATIC

# Webhook URL
PAYMENT_WEBHOOK_BASE_URL=https://api.liyaqa.com
```

### Application Configuration

```kotlin
@Configuration
class PaymentConfig {
    @Bean
    fun stripePaymentGateway(): PaymentGateway {
        return StripePaymentGateway(
            apiKey = env.getProperty("STRIPE_API_KEY")!!,
            webhookSecret = env.getProperty("STRIPE_WEBHOOK_SECRET")!!
        )
    }
}
```

## Database Schema

```sql
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    booking_id UUID REFERENCES bookings(id),
    membership_id UUID REFERENCES memberships(id),
    member_id UUID NOT NULL REFERENCES members(id),
    branch_id UUID NOT NULL,
    facility_id UUID NOT NULL,
    transaction_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    gateway VARCHAR(50) NOT NULL,
    gateway_payment_id VARCHAR(255) NOT NULL,
    gateway_client_secret VARCHAR(500),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    original_amount DECIMAL(10,2),
    status VARCHAR(50) NOT NULL,
    status_message TEXT,
    payment_method VARCHAR(50),
    payment_brand VARCHAR(50),
    last_four VARCHAR(4),
    authorized_at TIMESTAMP,
    captured_at TIMESTAMP,
    failed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_amount DECIMAL(10,2),
    refund_reason VARCHAR(100),
    parent_transaction_id UUID REFERENCES payment_transactions(id),
    receipt_url VARCHAR(500),
    error_code VARCHAR(100),
    error_message TEXT,
    decline_code VARCHAR(100),
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_payment_transaction_number UNIQUE (transaction_number),
    CONSTRAINT uk_gateway_payment_id UNIQUE (gateway, gateway_payment_id)
);

-- Indexes
CREATE INDEX idx_payment_booking ON payment_transactions(booking_id);
CREATE INDEX idx_payment_member ON payment_transactions(member_id);
CREATE INDEX idx_payment_gateway_ref ON payment_transactions(gateway, gateway_payment_id);
CREATE INDEX idx_payment_status ON payment_transactions(status);
CREATE INDEX idx_payment_tenant ON payment_transactions(tenant_id);
```

## Common Usage Patterns

### Complete Booking Payment Flow

```kotlin
// 1. Create booking
val booking = bookingService.createBooking(request)

// 2. Create payment intent
val paymentResult = paymentService.createBookingPayment(
    booking = booking,
    member = member
)

if (paymentResult.success) {
    // 3. Return client secret to frontend
    return ResponseEntity.ok(mapOf(
        "clientSecret" to paymentResult.clientSecret,
        "bookingId" to booking.id,
        "amount" to paymentResult.amount
    ))
}

// Frontend uses Stripe.js:
// const stripe = Stripe('pk_test_...');
// stripe.confirmCardPayment(clientSecret, {
//     payment_method: {
//         card: cardElement,
//         billing_details: { email: 'customer@example.com' }
//     }
// });

// 4. Webhook confirms payment
// POST /webhooks/stripe
// -> Updates booking payment status
// -> Sends confirmation email
```

### Process Refund

```kotlin
// Find transaction
val transaction = paymentTransactionRepository.findByBookingId(bookingId)

// Check if refundable
if (transaction.canBeRefunded()) {
    val refundAmount = transaction.getRemainingRefundableAmount()

    val refundResult = paymentService.refundPayment(
        transactionId = transaction.id,
        amount = refundAmount,
        reason = RefundReason.BOOKING_CANCELLED
    )

    if (refundResult.success) {
        // Update booking status
        booking.markAsRefunded(refundAmount)
        bookingRepository.save(booking)

        // Send refund notification
        notificationService.sendRefundNotification(booking, refundAmount)
    }
}
```

## Security Considerations

1. **API Key Security:**
   - Store API keys in environment variables
   - Never commit keys to version control
   - Use different keys for test/production

2. **Webhook Verification:**
   - Always verify webhook signatures
   - Reject unsigned webhooks
   - Use HTTPS for webhook endpoints

3. **Idempotency:**
   - Use idempotency keys to prevent duplicate charges
   - Store idempotency keys with transactions

4. **PCI Compliance:**
   - Never store card numbers
   - Use payment gateway tokenization
   - Let Stripe handle card data

5. **Amount Validation:**
   - Validate amounts on both frontend and backend
   - Prevent amount manipulation
   - Use BigDecimal for precise calculations

## Testing Strategy

**Unit Tests:**
- Gateway abstraction logic
- Result mapping
- Error handling
- Refund calculation

**Integration Tests:**
- Full payment flow with test gateway
- Webhook processing
- Idempotency enforcement
- Concurrent payment attempts

**Test Scenarios:**
```kotlin
@Test
fun `should create payment intent successfully`()

@Test
fun `should prevent duplicate payment with same idempotency key`()

@Test
fun `should process full refund correctly`()

@Test
fun `should process partial refund and track remaining amount`()

@Test
fun `should verify webhook signature`()

@Test
fun `should reject invalid webhook signature`()

@Test
fun `should update transaction status from webhook event`()
```

## Monitoring & Logging

**Key Metrics:**
- Payment success rate
- Average payment processing time
- Refund rate
- Failed payment reasons
- Webhook processing latency

**Logging:**
- All payment intent creations
- All captures and refunds
- Webhook events received
- Payment failures with error details
- Security events (invalid signatures)

## Future Enhancements

1. **Additional Payment Gateways:**
   - PayPal integration
   - Square integration
   - Local payment methods (bank transfer, cash)

2. **Saved Payment Methods:**
   - Store customer payment methods
   - One-click checkout
   - Auto-payment for recurring memberships

3. **Payment Plans:**
   - Installment payments
   - Subscription billing
   - Deferred payments

4. **Enhanced Refund Logic:**
   - Automatic refund policies
   - Partial refund calculations based on usage
   - Refund approval workflows

5. **Payment Analytics:**
   - Revenue dashboards
   - Payment method preferences
   - Decline reason analysis
   - Refund pattern detection

## See Also

- [Booking Module](../facility/booking/README.md) - Court booking payments
- [Membership Module](../facility/membership/README.md) - Membership subscription payments
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Overall system architecture
- [Stripe Documentation](https://stripe.com/docs/api) - Stripe API reference
