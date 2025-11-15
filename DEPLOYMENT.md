# Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the Liyaqa backend application to various environments, including local development, staging, and production.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Local Development](#local-development)
4. [Docker Deployment](#docker-deployment)
5. [Production Deployment](#production-deployment)
6. [Database Management](#database-management)
7. [Monitoring & Logging](#monitoring--logging)
8. [Backup & Disaster Recovery](#backup--disaster-recovery)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

| Component | Version | Purpose |
|-----------|---------|---------|
| Java (JDK) | 17+ | Application runtime |
| Gradle | 8.x | Build tool |
| PostgreSQL | 15+ | Primary database |
| Redis | 7.x | Session storage & caching |
| Docker | 20.x+ | Containerization (optional) |
| Docker Compose | 2.x+ | Local orchestration |

### Development Tools

- Git for version control
- IntelliJ IDEA or VS Code with Kotlin support
- Postman or similar for API testing
- pgAdmin or DBeaver for database management

---

## Environment Configuration

### Environment Variables

Create `.env` file or set environment variables:

```properties
# Application
SPRING_PROFILES_ACTIVE=development
SERVER_PORT=8080

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/liyaqa
DATABASE_USERNAME=liyaqa_user
DATABASE_PASSWORD=secure_password_here
DATABASE_POOL_SIZE=10

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT Authentication
JWT_SECRET=your-secret-key-min-256-bits
JWT_ACCESS_TOKEN_EXPIRY=900000        # 15 minutes in ms
JWT_REFRESH_TOKEN_EXPIRY=604800000    # 7 days in ms

# Email (SMTP)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=${SENDGRID_API_KEY}
MAIL_FROM=noreply@liyaqa.com

# Payment (Stripe)
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# SMS (Twilio)
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_PHONE_NUMBER=+1234567890

# Push Notifications (Firebase)
FIREBASE_CREDENTIALS_PATH=config/firebase-credentials.json

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://app.liyaqa.com

# Logging
LOG_LEVEL=INFO
LOG_FILE_PATH=/var/log/liyaqa/application.log
```

### Configuration Profiles

**application.yml:**

```yaml
spring:
  application:
    name: liyaqa-backend

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:development}

  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: ${DATABASE_POOL_SIZE:10}
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: none  # Liquibase handles schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}

  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

server:
  port: ${SERVER_PORT:8080}
  error:
    include-message: always
    include-stacktrace: on_param

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.liyaqa: DEBUG
  file:
    name: ${LOG_FILE_PATH:/var/log/liyaqa/application.log}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**application-development.yml:**

```yaml
spring:
  jpa:
    show-sql: true

logging:
  level:
    com.liyaqa: DEBUG
```

**application-production.yml:**

```yaml
spring:
  jpa:
    show-sql: false

logging:
  level:
    com.liyaqa: INFO

server:
  compression:
    enabled: true
    min-response-size: 1024
```

---

## Local Development

### Using Docker Compose

**1. Start Infrastructure:**

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: liyaqa-postgres
    environment:
      POSTGRES_DB: liyaqa
      POSTGRES_USER: liyaqa_user
      POSTGRES_PASSWORD: liyaqa_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: liyaqa-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**2. Run Application:**

```bash
# Build
./gradlew build

# Run application
./gradlew bootRun

# Or with custom profile
SPRING_PROFILES_ACTIVE=development ./gradlew bootRun
```

**3. Initialize Database:**

Liquibase runs automatically on startup. To manually apply migrations:

```bash
./gradlew update
```

**4. Create Initial Admin:**

```bash
curl -X POST http://localhost:8080/api/v1/internal/system/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@liyaqa.com",
    "password": "SecurePassword123!",
    "firstName": "Admin",
    "lastName": "User"
  }'
```

---

## Docker Deployment

### Build Docker Image

**Dockerfile:**

```dockerfile
FROM gradle:8-jdk17-alpine AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon -x test

FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build and Run:**

```bash
# Build image
docker build -t liyaqa-backend:latest .

# Run container
docker run -d \
  --name liyaqa-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/liyaqa \
  -e DATABASE_USERNAME=liyaqa_user \
  -e DATABASE_PASSWORD=secure_password \
  -e REDIS_HOST=redis \
  --network liyaqa-network \
  liyaqa-backend:latest
```

### Docker Compose (Full Stack)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: liyaqa
      POSTGRES_USER: liyaqa_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - liyaqa-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U liyaqa_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    networks:
      - liyaqa-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: production
      DATABASE_URL: jdbc:postgresql://postgres:5432/liyaqa
      DATABASE_USERNAME: liyaqa_user
      DATABASE_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET}
      STRIPE_API_KEY: ${STRIPE_API_KEY}
      SENDGRID_API_KEY: ${SENDGRID_API_KEY}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - liyaqa-network
    restart: unless-stopped

networks:
  liyaqa-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
```

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Scale backend instances
docker-compose up -d --scale backend=3

# Stop all services
docker-compose down
```

---

## Production Deployment

### Cloud Platform Options

#### AWS Deployment

**Using Elastic Beanstalk:**

```bash
# Install EB CLI
pip install awsebcli

# Initialize EB application
eb init -p docker liyaqa-backend

# Create environment
eb create liyaqa-production

# Deploy
eb deploy

# View logs
eb logs

# SSH into instance
eb ssh
```

**Using ECS (Fargate):**

1. Build and push image to ECR
2. Create ECS task definition
3. Create ECS service
4. Configure Application Load Balancer
5. Set up auto-scaling

#### Heroku Deployment

```bash
# Create app
heroku create liyaqa-backend

# Add PostgreSQL
heroku addons:create heroku-postgresql:standard-0

# Add Redis
heroku addons:create heroku-redis:premium-0

# Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=production
heroku config:set JWT_SECRET=your_secret
heroku config:set STRIPE_API_KEY=sk_live_...

# Deploy
git push heroku main

# Run migrations
heroku run ./gradlew update

# View logs
heroku logs --tail
```

#### DigitalOcean App Platform

1. Connect GitHub repository
2. Select Dockerfile deployment
3. Configure environment variables
4. Attach managed PostgreSQL database
5. Attach managed Redis cluster
6. Deploy

### Production Checklist

Before deploying to production:

- [ ] Set strong JWT secret (min 256 bits)
- [ ] Use production database credentials
- [ ] Enable SSL/TLS for database connections
- [ ] Configure CORS for production domains
- [ ] Set up SSL certificate (Let's Encrypt)
- [ ] Enable HTTPS only
- [ ] Configure rate limiting
- [ ] Set up monitoring and alerting
- [ ] Configure log aggregation
- [ ] Set up automated backups
- [ ] Test disaster recovery plan
- [ ] Configure firewall rules
- [ ] Enable database connection pooling
- [ ] Set appropriate JVM memory settings
- [ ] Configure health check endpoints
- [ ] Set up CDN for static assets
- [ ] Enable gzip compression
- [ ] Configure cache headers
- [ ] Set up error tracking (Sentry)
- [ ] Load test application
- [ ] Perform security audit

---

## Database Management

### Migrations

**Apply migrations:**

```bash
./gradlew update
```

**Rollback migration:**

```bash
./gradlew rollback -PliquibaseCommandValue=1
```

**Generate changelog:**

```bash
./gradlew diffChangeLog
```

### Backup

**Manual backup:**

```bash
# Create backup
pg_dump -h localhost -U liyaqa_user -d liyaqa > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
psql -h localhost -U liyaqa_user -d liyaqa < backup_20251115_100000.sql
```

**Automated backup (cron):**

```bash
# Add to crontab
0 2 * * * /usr/local/bin/backup-liyaqa-db.sh
```

**backup-liyaqa-db.sh:**

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/liyaqa"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/liyaqa_backup_$TIMESTAMP.sql"

mkdir -p $BACKUP_DIR

pg_dump -h localhost -U liyaqa_user -d liyaqa > $BACKUP_FILE

# Compress
gzip $BACKUP_FILE

# Upload to S3
aws s3 cp $BACKUP_FILE.gz s3://liyaqa-backups/

# Delete backups older than 30 days
find $BACKUP_DIR -name "liyaqa_backup_*.sql.gz" -mtime +30 -delete
```

---

## Monitoring & Logging

### Health Check Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Detailed health
curl http://localhost:8080/actuator/health?show-details=always
```

### Metrics

Enable Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Access metrics:

```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### Logging

**Centralized Logging (ELK Stack):**

1. **Elasticsearch:** Store logs
2. **Logstash:** Process logs
3. **Kibana:** Visualize logs

**Log aggregation:**

```yaml
# logback-spring.xml
<configuration>
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

### Monitoring Tools

- **Prometheus + Grafana:** Metrics and dashboards
- **New Relic / Datadog:** APM
- **Sentry:** Error tracking
- **PagerDuty / OpsGenie:** Alerting

---

## Backup & Disaster Recovery

### Backup Strategy

**Daily Backups:**
- Automated daily backups at 2 AM
- Retain for 30 days
- Upload to S3 with versioning

**Weekly Backups:**
- Full database backup
- Retain for 12 weeks

**Monthly Backups:**
- Archive to Glacier
- Retain for 7 years (compliance)

### Disaster Recovery Plan

**RTO (Recovery Time Objective):** 4 hours
**RPO (Recovery Point Objective):** 1 hour

**Recovery Steps:**

1. **Database Failure:**
   ```bash
   # Restore from latest backup
   psql -h new-db-host -U liyaqa_user -d liyaqa < latest_backup.sql

   # Update DATABASE_URL
   export DATABASE_URL=jdbc:postgresql://new-db-host:5432/liyaqa

   # Restart application
   systemctl restart liyaqa-backend
   ```

2. **Application Server Failure:**
   - Deploy to new server
   - Restore environment variables
   - Point load balancer to new server
   - Verify health checks

3. **Complete System Failure:**
   - Deploy infrastructure from IaC (Terraform)
   - Restore database from backup
   - Deploy application
   - Update DNS records
   - Verify functionality

---

## Troubleshooting

### Common Issues

**1. Database Connection Error:**

```
Error: Could not connect to database
```

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check connection
psql -h localhost -U liyaqa_user -d liyaqa

# Verify credentials
echo $DATABASE_URL
```

**2. Redis Connection Error:**

```
Error: Unable to connect to Redis
```

**Solution:**
```bash
# Check Redis is running
docker ps | grep redis

# Test connection
redis-cli ping

# Check Redis host/port
echo $REDIS_HOST
echo $REDIS_PORT
```

**3. Liquibase Migration Error:**

```
Error: Liquibase changelog lock
```

**Solution:**
```sql
-- Clear lock
UPDATE databasechangeloglock SET locked = FALSE;
```

**4. Out of Memory:**

```
Error: java.lang.OutOfMemoryError
```

**Solution:**
```bash
# Increase JVM memory
export JAVA_OPTS="-Xmx2g -Xms512m"

# Or in Dockerfile
ENV JAVA_OPTS="-Xmx2g -Xms512m"
```

**5. High CPU Usage:**

```bash
# Check thread dump
jstack <PID> > thread_dump.txt

# Profile application
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar
```

### Debug Mode

```bash
# Enable debug logging
export LOG_LEVEL=DEBUG

# Run with remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar
```

---

## Security Hardening

1. **Enable HTTPS only**
2. **Set secure headers:**
   - X-Frame-Options: DENY
   - X-Content-Type-Options: nosniff
   - Strict-Transport-Security
3. **Rate limiting**
4. **IP whitelisting for admin endpoints**
5. **WAF (Web Application Firewall)**
6. **Regular security updates**
7. **Penetration testing**

---

## See Also

- [README.md](./README.md) - Project overview
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
- [TESTING.md](./TESTING.md) - Testing guide
- [CLAUDE.md](./CLAUDE.md) - Development guidelines
