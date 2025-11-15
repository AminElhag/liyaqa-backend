# Testing Guide

## Overview

This guide provides comprehensive information on testing the Liyaqa backend application, including test infrastructure, strategies, and best practices.

## Testing Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Test Framework | JUnit 5 (Jupiter) | Test structure and execution |
| Assertions | AssertJ | Fluent assertions |
| Mocking | MockK | Kotlin-friendly mocking |
| Integration Tests | Testcontainers | Real database testing |
| Security Testing | Spring Security Test | Authentication/authorization testing |
| API Testing | MockMvc / WebTestClient | REST API testing |

## Test Structure

```
src/test/kotlin/com/liyaqa/backend/
├── internal/
│   ├── employee/
│   │   ├── service/
│   │   │   └── EmployeeServiceTest.kt      # Unit tests
│   │   ├── controller/
│   │   │   └── EmployeeControllerTest.kt   # API tests
│   │   └── integration/
│   │       └── EmployeeIntegrationTest.kt  # Integration tests
│   ├── tenant/
│   └── ...
├── facility/
│   ├── booking/
│   ├── membership/
│   └── ...
├── payment/
└── shared/
```

## Test Categories

### 1. Unit Tests

Test individual classes in isolation with mocked dependencies.

**Example: Service Layer Test**

```kotlin
@Test
class BookingServiceTest {
    private val bookingRepository: BookingRepository = mockk()
    private val courtRepository: CourtRepository = mockk()
    private val memberRepository: MemberRepository = mockk()
    private val discountService: DiscountService = mockk()
    private val emailService: BookingEmailService = mockk(relaxed = true)

    private val bookingService = BookingService(
        bookingRepository,
        courtRepository,
        memberRepository,
        membershipRepository,
        discountService,
        emailService
    )

    @Test
    fun `should create booking successfully`() {
        // Given
        val member = createTestMember()
        val court = createTestCourt()
        val request = createBookingRequest()

        every { memberRepository.findById(any()) } returns Optional.of(member)
        every { courtRepository.findById(any()) } returns Optional.of(court)
        every { bookingRepository.hasOverlappingBooking(any(), any(), any()) } returns false
        every { bookingRepository.save(any()) } returnsArgument 0

        // When
        val result = bookingService.createBooking(request)

        // Then
        assertThat(result.courtId).isEqualTo(court.id)
        assertThat(result.memberId).isEqualTo(member.id)
        assertThat(result.status).isEqualTo(BookingStatus.CONFIRMED)

        verify { bookingRepository.save(any()) }
        verify { emailService.sendBookingConfirmation(any()) }
    }

    @Test
    fun `should prevent overlapping bookings`() {
        // Given
        val request = createBookingRequest()

        every { memberRepository.findById(any()) } returns Optional.of(createTestMember())
        every { courtRepository.findById(any()) } returns Optional.of(createTestCourt())
        every { bookingRepository.hasOverlappingBooking(any(), any(), any()) } returns true

        // When & Then
        assertThrows<IllegalStateException> {
            bookingService.createBooking(request)
        }
    }
}
```

**Best Practices:**
- Mock all external dependencies
- Test both happy paths and error cases
- Use descriptive test names with backticks
- Follow Given-When-Then structure
- Use test data builders for complex objects

### 2. Integration Tests

Test complete flows with real database using Testcontainers.

**Example: Integration Test with Testcontainers**

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BookingIntegrationTest {

    @Container
    companion object {
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("liyaqa_test")
            withUsername("test")
            withPassword("test")
        }
    }

    @DynamicPropertySource
    companion object {
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }

    @Autowired
    private lateinit var bookingService: BookingService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var courtRepository: CourtRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    @BeforeEach
    fun setup() {
        // Clean database
        bookingRepository.deleteAll()
        memberRepository.deleteAll()
        courtRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `should create booking and persist to database`() {
        // Given
        val member = memberRepository.save(createTestMember())
        val court = courtRepository.save(createTestCourt())
        val request = createBookingRequest(member.id!!, court.id!!)

        // When
        val result = bookingService.createBooking(request)

        // Then
        val saved = bookingRepository.findById(result.id!!).orElseThrow()
        assertThat(saved.member.id).isEqualTo(member.id)
        assertThat(saved.court.id).isEqualTo(court.id)
        assertThat(saved.status).isEqualTo(BookingStatus.CONFIRMED)
    }

    @Test
    @Transactional
    fun `should enforce overlapping booking constraint`() {
        // Given
        val member = memberRepository.save(createTestMember())
        val court = courtRepository.save(createTestCourt())

        // Create first booking
        val request1 = createBookingRequest(member.id!!, court.id!!)
        bookingService.createBooking(request1)

        // Try to create overlapping booking
        val request2 = createBookingRequest(member.id!!, court.id!!)

        // When & Then
        assertThrows<IllegalStateException> {
            bookingService.createBooking(request2)
        }
    }
}
```

**Best Practices:**
- Use Testcontainers for real database
- Clean database before each test
- Test database constraints and indexes
- Test transaction rollbacks
- Test concurrent operations

### 3. API/Controller Tests

Test REST endpoints with MockMvc.

**Example: Controller Test**

```kotlin
@WebMvcTest(BookingController::class)
@Import(SecurityConfig::class)
class BookingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var bookingService: BookingService

    @Test
    @WithMockUser(authorities = ["BOOKING_CREATE"])
    fun `POST api v1 facility bookings should create booking`() {
        // Given
        val request = createBookingRequest()
        val response = createBookingResponse()

        every { bookingService.createBooking(any()) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/v1/facility/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(response.id.toString()))
            .andExpect(jsonPath("$.bookingNumber").value(response.bookingNumber))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
    }

    @Test
    fun `POST api v1 facility bookings should require authentication`() {
        mockMvc.perform(
            post("/api/v1/facility/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(authorities = ["BOOKING_VIEW"])
    fun `GET api v1 facility bookings id should return booking`() {
        // Given
        val bookingId = UUID.randomUUID()
        val response = createBookingResponse(id = bookingId)

        every { bookingService.getBookingById(bookingId) } returns response

        // When & Then
        mockMvc.perform(get("/api/v1/facility/bookings/$bookingId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(bookingId.toString()))
    }
}
```

**Best Practices:**
- Use `@WebMvcTest` for focused controller testing
- Test all HTTP methods (GET, POST, PUT, DELETE)
- Test authorization rules
- Test validation
- Test error responses
- Use Spring Security Test for authentication

### 4. Security Tests

Test permission checks and authentication flows.

**Example: Permission Test**

```kotlin
@Test
fun `should allow TENANT_CREATE permission to create tenant`() {
    // Given
    val employee = createEmployeeWithPermission(Permission.TENANT_CREATE)
    val request = createTenantRequest()

    every { employeeRepository.findById(employee.id!!) } returns Optional.of(employee)
    every { tenantRepository.save(any()) } returnsArgument 0

    // When
    val result = tenantService.createTenant(request, employee)

    // Then
    assertThat(result).isNotNull
    verify { auditService.logCreate(employee, EntityType.TENANT, any(), any()) }
}

@Test
fun `should reject TENANT_CREATE without permission`() {
    // Given
    val employee = createEmployeeWithoutPermission(Permission.TENANT_CREATE)
    val request = createTenantRequest()

    every { employeeRepository.findById(employee.id!!) } returns Optional.of(employee)

    // When & Then
    assertThrows<SecurityException> {
        tenantService.createTenant(request, employee)
    }

    verify { auditService.logUnauthorizedAccess(employee, any(), EntityType.TENANT) }
}
```

### 5. Multi-Tenancy Tests

Test tenant isolation and context management.

**Example: Tenant Isolation Test**

```kotlin
@Test
@Transactional
fun `should only return bookings for current tenant`() {
    // Given
    val tenant1 = createTenant("tenant-1")
    val tenant2 = createTenant("tenant-2")

    val booking1 = createBooking(tenantId = "tenant-1")
    val booking2 = createBooking(tenantId = "tenant-2")

    bookingRepository.saveAll(listOf(booking1, booking2))

    // Set tenant context
    TenantContextHolder.setTenantId("tenant-1")

    // When
    val results = bookingRepository.findAll()

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0].tenantId).isEqualTo("tenant-1")

    // Cleanup
    TenantContextHolder.clear()
}
```

## Test Data Builders

Create reusable test data builders for complex objects.

**Example: Test Data Builders**

```kotlin
object TestDataBuilder {

    fun createTestMember(
        firstName: String = "John",
        lastName: String = "Doe",
        email: String = "john.doe@example.com",
        phoneNumber: String = "+1-555-0123"
    ): Member {
        return Member(
            facility = createTestFacility(),
            branch = createTestBranch(),
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phoneNumber
        ).apply {
            tenantId = "test-tenant"
        }
    }

    fun createTestCourt(
        name: String = "Tennis Court 1",
        courtType: CourtType = CourtType.TENNIS,
        hourlyRate: BigDecimal = BigDecimal("50.00")
    ): Court {
        return Court(
            facility = createTestFacility(),
            branch = createTestBranch(),
            name = name,
            courtType = courtType,
            hourlyRate = hourlyRate
        ).apply {
            tenantId = "test-tenant"
        }
    }

    fun createBookingRequest(
        memberId: UUID = UUID.randomUUID(),
        courtId: UUID = UUID.randomUUID(),
        startTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        durationMinutes: Int = 90
    ): BookingCreateRequest {
        return BookingCreateRequest(
            memberId = memberId,
            courtId = courtId,
            bookingDate = startTime.toLocalDate(),
            startTime = startTime,
            durationMinutes = durationMinutes,
            numberOfPlayers = 2
        )
    }
}
```

## Running Tests

### Command Line

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :facility:booking:test

# Run specific test class
./gradlew test --tests BookingServiceTest

# Run specific test method
./gradlew test --tests "BookingServiceTest.should create booking successfully"

# Run tests with coverage
./gradlew test jacocoTestReport

# Run only integration tests
./gradlew integrationTest

# Run with verbose output
./gradlew test --info
```

### IDE

**IntelliJ IDEA:**
- Right-click test class/method → Run
- Use keyboard shortcut: Ctrl+Shift+F10 (Windows/Linux) or ⌃⇧R (Mac)
- View test results in Run tool window
- Enable "Run tests with coverage" for coverage report

## Test Coverage

**Target Coverage:**
- **Line Coverage:** Minimum 80%
- **Branch Coverage:** Minimum 75%
- **Service Layer:** Minimum 90%
- **Controller Layer:** Minimum 80%
- **Domain Logic:** 100%

**Generate Coverage Report:**

```bash
./gradlew test jacocoTestReport
```

View report at: `build/reports/jacoco/test/html/index.html`

## Continuous Integration

**GitHub Actions Workflow:**

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: liyaqa_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew test

      - name: Generate coverage report
        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
```

## Testing Best Practices

1. **Write Tests First (TDD):**
   - Define expected behavior before implementation
   - Write failing test
   - Implement feature
   - Refactor

2. **Test Naming:**
   - Use descriptive names with backticks
   - Format: `should [expected behavior] when [condition]`
   - Example: `should prevent overlapping bookings when time conflicts`

3. **Arrange-Act-Assert (AAA):**
   ```kotlin
   @Test
   fun `should calculate discount correctly`() {
       // Arrange (Given)
       val discount = createDiscount(type = PERCENTAGE, value = 20)
       val originalPrice = BigDecimal("100.00")

       // Act (When)
       val result = discount.calculateDiscountAmount(originalPrice)

       // Assert (Then)
       assertThat(result).isEqualTo(BigDecimal("20.00"))
   }
   ```

4. **One Assertion Per Test (When Possible):**
   - Focus each test on one behavior
   - Makes failures easier to diagnose
   - Exceptions: Related assertions on same object

5. **Use Test Fixtures:**
   - Use `@BeforeEach` for common setup
   - Keep tests independent
   - Clean up in `@AfterEach` when needed

6. **Mock External Dependencies:**
   - Database (except integration tests)
   - External APIs
   - File system
   - Time (`Clock.fixed()`)

7. **Test Edge Cases:**
   - Null values
   - Empty collections
   - Boundary conditions
   - Invalid input
   - Concurrent modifications

8. **Keep Tests Fast:**
   - Unit tests should run in milliseconds
   - Integration tests in seconds
   - Use test doubles to avoid slow operations
   - Run unit tests frequently, integration tests less often

## Troubleshooting

**Common Issues:**

1. **Testcontainers Docker Error:**
   ```
   Solution: Ensure Docker is running and accessible
   $ docker info
   ```

2. **Database Connection Issues:**
   ```
   Solution: Check port conflicts, container health
   $ docker ps
   ```

3. **Test Data Conflicts:**
   ```
   Solution: Clean database in @BeforeEach
   @BeforeEach
   fun setup() {
       repository.deleteAll()
   }
   ```

4. **Flaky Tests:**
   ```
   Solution: Avoid time-dependent logic, use fixed clocks
   val clock = Clock.fixed(Instant.parse("2025-11-15T10:00:00Z"), ZoneId.of("UTC"))
   ```

## See Also

- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
- [CLAUDE.md](./CLAUDE.md) - Development guidelines
- [DEPLOYMENT.md](./DEPLOYMENT.md) - Deployment guide
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [MockK Documentation](https://mockk.io/)
