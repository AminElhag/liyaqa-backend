Liyaqa - Sports Facility Management SaaS Backend
🏗️ Architecture Overview
Liyaqa is a multi-tenant SaaS platform for sports facility management, built with enterprise-grade security and scalability in mind.
Current Implementation Status: Internal Control Plane ✅
We've implemented the internal team management system as the foundation. This control plane allows our team to:

Manage internal employees with role-based access control (RBAC)
Track all system activities with comprehensive audit logging
Control tenant operations and support
Handle deals and payments (coming next)

🎯 System Design Principles

Security First: Zero-trust architecture with defense in depth
Multi-tenancy Ready: Row-level isolation with upgrade path to schema/database per tenant
Audit Everything: Immutable audit trail for compliance and debugging
Internationalization: Built-in support for Arabic and English
Observable: Comprehensive logging and monitoring hooks

🛠️ Technology Stack

Language: Kotlin
Framework: Spring Boot 3.5.7
JDK: 21
Build Tool: Gradle Kotlin DSL
Database: PostgreSQL with Liquibase migrations
Security: JWT with Spring Security
Container: Docker Compose for local development

🚀 Getting Started
Prerequisites

JDK 21
Docker & Docker Compose
PostgreSQL (or use Docker)
Gradle

Local Development Setup

Clone the repository

bashgit clone https://github.com/your-org/liyaqa-backend.git
cd liyaqa-backend

Start PostgreSQL

bashdocker-compose up -d

Run database migrations

bash./gradlew update

Start the application

bash./gradlew bootRun
The API will be available at http://localhost:8080
Initial Admin Setup
On first run, create a super admin user:
bashcurl -X POST http://localhost:8080/api/v1/internal/employees \
-H "Content-Type: application/json" \
-d '{
"firstName": "Admin",
"lastName": "User",
"email": "admin@liyaqa.com",
"department": "Engineering",
"jobTitle": "System Administrator",
"groupIds": ["550e8400-e29b-41d4-a716-446655440001"]
}'
📁 Project Structure
liyaqa-backend/
├── src/main/kotlin/com/liyaqa/
│   ├── core/               # Core configurations and base classes
│   │   ├── config/         # Multi-tenancy, security configs
│   │   ├── context/        # Tenant context management
│   │   └── domain/base/    # Base entities
│   │
│   ├── internal/           # Internal control plane
│   │   ├── domain/         # Domain entities
│   │   │   ├── employee/   # Employee management
│   │   │   └── audit/      # Audit logging
│   │   ├── controller/     # REST controllers
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Data access
│   │   ├── security/       # Auth & authorization
│   │   └── dto/            # Data transfer objects
│   │
│   └── tenant/             # Tenant features (coming next)
│
├── src/main/resources/
│   ├── application.yml     # Application configuration
│   └── db/changelog/       # Liquibase migrations
│
└── docker-compose.yml      # Local development environment
🔐 Security Model
Authentication

JWT-based authentication with access/refresh token pattern
Password requirements: 12+ chars, mixed case, numbers, special chars
Account lockout after 5 failed attempts
Session management with immediate revocation capability

Authorization

Fine-grained permission model
Role-based access through groups
Method-level security with custom annotations
Tenant isolation at data layer

Predefined Groups

Super Admin: Full system access
Support Agent: Handle customer tickets
Support Manager: Manage support team + tenant access
Sales: Deal and tenant creation
Finance: Payment processing and approvals

📊 API Documentation
Authentication Endpoints
MethodEndpointDescriptionPOST/api/v1/internal/auth/loginEmployee loginPOST/api/v1/internal/auth/refreshRefresh access tokenPOST/api/v1/internal/auth/logoutLogout and revoke tokensPOST/api/v1/internal/auth/password-reset/requestRequest password resetGET/api/v1/internal/auth/validateValidate current session
Employee Management
MethodEndpointDescriptionPermission RequiredGET/api/v1/internal/employeesList employeesEMPLOYEE_VIEWPOST/api/v1/internal/employeesCreate employeeEMPLOYEE_CREATEGET/api/v1/internal/employees/{id}Get employee detailsEMPLOYEE_VIEWPATCH/api/v1/internal/employees/{id}Update employeeEMPLOYEE_UPDATEDELETE/api/v1/internal/employees/{id}Delete employeeEMPLOYEE_DELETEPUT/api/v1/internal/employees/{id}/groupsUpdate groupsGROUP_ASSIGN_PERMISSIONSGET/api/v1/internal/employees/meGet own profileNone (self)PATCH/api/v1/internal/employees/meUpdate own profileNone (self)
🔄 Database Migrations
Migrations are managed by Liquibase. To create a new migration:

Create a new changeset in src/main/resources/db/changelog/
Reference it in db.changelog-master.xml
Run ./gradlew update to apply

🧪 Testing
bash# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
📈 Monitoring
Health check: http://localhost:8080/actuator/health
Metrics: http://localhost:8080/actuator/metrics
🚦 Development Workflow

Create feature branch from main
Implement feature with tests
Ensure all tests pass
Create pull request with description
Code review and approval
Merge to main

📝 Environment Variables
VariableDescriptionDefaultDB_USERNAMEDatabase usernameliyaqaDB_PASSWORDDatabase passwordliyaqa_devJWT_SECRETJWT signing secret(must set in production)SPRING_PROFILES_ACTIVEActive profiledev
🎯 Next Features

Tenant Management: CRUD operations for sports facilities
Support Ticketing: Internal support system
Deal Management: Sales pipeline and contracts
Payment Processing: Billing and invoicing
Facility Features: Courts, schedules, bookings (tenant-facing)

🤝 Contributing

Follow Kotlin coding conventions
Write comprehensive tests
Document API changes
Update migrations carefully
Consider security implications

📄 License
Proprietary - Liyaqa © 2024

Built with ❤️ for the sports community