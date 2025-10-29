# Liyaqa Backend Configuration Guide

## Quick Start

1. **Copy environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your values:**
   - Database credentials
   - JWT secret (generate with: `openssl rand -base64 64`)
   - Email SMTP credentials (if using email features)

3. **Start infrastructure:**
   ```bash
   docker-compose up -d
   ```

4. **Run application:**
   ```bash
   ./gradlew bootRun
   ```

## Environment Variables Reference

### 🗄️ Database Configuration

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `DB_HOST` | localhost | PostgreSQL host | ✅ Yes |
| `DB_PORT` | 5434 | PostgreSQL port | ✅ Yes |
| `DB_NAME` | liyaqa | Database name | ✅ Yes |
| `DB_USERNAME` | liyaqa | Database user | ✅ Yes |
| `DB_PASSWORD` | - | Database password | ✅ Yes |

### 🔴 Redis Configuration

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `REDIS_HOST` | localhost | Redis server host | ✅ Yes |
| `REDIS_PORT` | 6379 | Redis server port | ✅ Yes |
| `REDIS_PASSWORD` | - | Redis password | ✅ Yes |

**Purpose:** Redis stores:
- User sessions (distributed across instances)
- JWT token blacklist (for immediate revocation)
- IP history (for risk scoring)

### 🔐 JWT Security

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `JWT_SECRET` | - | Secret key for JWT signing | ✅ Yes |

**⚠️ CRITICAL:**
- Must be at least 64 characters
- Generate: `openssl rand -base64 64`
- Never reuse between environments
- Rotating this invalidates all tokens

### 📧 Email Configuration

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `MAIL_HOST` | smtp.gmail.com | SMTP server | ⚠️ If email enabled |
| `MAIL_PORT` | 587 | SMTP port | ⚠️ If email enabled |
| `MAIL_USERNAME` | - | SMTP username | ⚠️ If email enabled |
| `MAIL_PASSWORD` | - | SMTP password | ⚠️ If email enabled |
| `EMAIL_ENABLED` | false | Enable email sending | No |
| `EMAIL_FROM` | noreply@liyaqa.com | From address | No |
| `EMAIL_SECURITY_TEAM` | security@liyaqa.com | Security alerts recipient | No |

**Email Features:**
- Welcome emails with temporary passwords
- Password reset links
- Account lockout notifications
- Security alerts
- Login notifications (high-privilege accounts)

**Gmail Setup:**
1. Enable 2FA on your Gmail account
2. Generate App Password: https://myaccount.google.com/apppasswords
3. Use App Password in `MAIL_PASSWORD`

**Production Recommendations:**
- Use dedicated email service (SendGrid, AWS SES, Mailgun)
- Set up SPF, DKIM, and DMARC records
- Monitor email bounce rates

### 🌐 Application Configuration

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `SPRING_PROFILES_ACTIVE` | dev | Spring profile (dev/prod) | ✅ Yes |
| `APP_BASE_URL` | http://localhost:8080 | Public application URL | ✅ Yes |

**Profiles:**
- `dev`: Development mode (verbose logging, SQL debug)
- `prod`: Production mode (optimized logging, security hardening)

### ⏱️ Session Configuration

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `SESSION_TIMEOUT_HOURS` | 8 | Session lifetime in hours | No |

**Default:** 8 hours (standard work day)
**Range:** 1-24 hours
**Note:** Shorter = more secure, longer = better UX

### 📊 Audit & Async Configuration

| Variable | Default | Description | Required |
|----------|---------|-------------|----------|
| `AUDIT_CORE_POOL_SIZE` | 5 | Core thread pool size | No |
| `AUDIT_MAX_POOL_SIZE` | 10 | Maximum thread pool size | No |
| `AUDIT_QUEUE_CAPACITY` | 1000 | Queue capacity | No |
| `AUDIT_BATCH_SIZE` | 50 | Batch write size | No |
| `AUDIT_BATCH_TIMEOUT_MS` | 1000 | Batch timeout (ms) | No |

**Tuning Guidelines:**
- **Low traffic** (< 100 req/min): Default settings
- **Medium traffic** (100-1000 req/min): Double pool sizes
- **High traffic** (> 1000 req/min): Increase batch size to 100-200

**Performance Impact:**
- Larger batch size = Better DB throughput, higher latency
- Smaller timeout = Lower latency, more DB writes
- Larger pool = More concurrency, more memory

## Configuration Files

### application.yaml

Main Spring Boot configuration. All values use `${ENV_VAR:default}` syntax.

**⚠️ Do not modify defaults in application.yaml directly**
- Use environment variables or .env file instead
- Keeps configuration portable across environments

### docker-compose.yml

Defines local development infrastructure:
- **PostgreSQL 16**: Main database
- **Redis 7**: Session storage and cache

**Default ports:**
- PostgreSQL: 5434 (to avoid conflicts with other instances)
- Redis: 6379 (standard)

## Security Considerations

### Production Checklist

- [ ] Generate strong JWT secret (64+ characters)
- [ ] Change all default passwords
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure production email SMTP
- [ ] Set `APP_BASE_URL` to production domain
- [ ] Enable HTTPS/TLS
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Configure Redis persistence
- [ ] Review audit log retention policy
- [ ] Set up monitoring and alerts

### Secrets Management

**Development:**
- Use `.env` file (never commit to git)

**Production Options:**
1. **Environment variables** (basic)
2. **AWS Secrets Manager** (recommended)
3. **HashiCorp Vault** (enterprise)
4. **Kubernetes Secrets** (K8s deployments)

### CORS Configuration

Currently hardcoded in `SecurityConfig.kt`:
```kotlin
allowedOrigins = listOf(
    "http://localhost:3000",      // Development
    "https://admin.liyaqa.com"    // Production
)
```

**TODO:** Externalize to environment variable:
```bash
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://admin.liyaqa.com
```

## Database Setup

### Initial Setup

1. **Start PostgreSQL:**
   ```bash
   docker-compose up -d postgres
   ```

2. **Liquibase migrations run automatically** on application start

3. **Verify schema:**
   ```bash
   docker exec -it liyaqa-postgres psql -U liyaqa -d liyaqa -c "\dt"
   ```

### Schema Version

Current schema version: **006**

**Tables:**
- `tenants` - Tenant management
- `internal_employees` - Internal team members
- `internal_employee_groups` - Permission groups
- `group_permissions` - Group permissions mapping
- `employee_groups` - Employee-group assignments
- `internal_audit_logs` - Comprehensive audit trail

### Migrations

Liquibase changesets: `src/main/resources/db/changelog/db.changelog-master.xml`

**Add new migration:**
1. Create changeset in `db.changelog-master.xml`
2. Use incremental ID (007, 008, etc.)
3. Include rollback if possible
4. Test on clean database

## Redis Setup

### Local Development

Redis starts automatically with `docker-compose up`

**Verify connection:**
```bash
docker exec -it liyaqa-redis redis-cli
auth liyaqa_redis_dev
ping  # Should respond with PONG
```

### Key Structure

```
liyaqa:session:token:{refreshToken}        # Session by token
liyaqa:session:id:{sessionId}              # Session by ID
liyaqa:session:employee:{empId}:{sessId}   # Employee sessions
liyaqa:session:ip:{empId}:{ipAddress}      # IP history
liyaqa:blacklist:token:{tokenHash}         # Blacklisted tokens
```

### Persistence

**Development:** Disabled (data lost on restart)

**Production:** Enable RDB or AOF
```yaml
# docker-compose.yml
command: redis-server --save 60 1000 --appendonly yes
```

## Monitoring & Health Checks

### Actuator Endpoints

Available at `/actuator/*`:

- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Performance metrics

**Access:**
```bash
curl http://localhost:8080/actuator/health
```

### Metrics to Monitor

1. **Audit Queue Size** - Should stay < 1000
2. **Session Count** - Active user sessions
3. **Failed Login Rate** - Security monitoring
4. **API Response Times** - Performance
5. **Database Connection Pool** - Resource usage

### Logs

**Development:**
```bash
./gradlew bootRun
```

**Production:**
- JSON structured logging (recommended)
- Ship to centralized logging (ELK, Datadog, CloudWatch)
- Set appropriate log levels in application.yaml

## Troubleshooting

### Application won't start

**Check:**
1. Database is running: `docker ps | grep postgres`
2. Redis is running: `docker ps | grep redis`
3. Environment variables are set: `cat .env`
4. Ports are available: `lsof -i :8080,5434,6379`

### Email not sending

**Check:**
1. `EMAIL_ENABLED=true` in .env
2. Valid SMTP credentials
3. Application logs for email errors
4. Firewall allows SMTP port (587/465)

### Sessions not persisting

**Check:**
1. Redis is running and accessible
2. Redis password matches configuration
3. Check logs for Redis connection errors

### Slow performance

**Check:**
1. Audit queue size: `/actuator/metrics/audit.queue.size`
2. Database connection pool usage
3. Redis memory usage
4. Increase audit batch size if high throughput

## Support

For configuration issues:
1. Check this documentation
2. Review application logs
3. Verify environment variables
4. Check docker-compose logs: `docker-compose logs`

For bugs or feature requests:
- Create issue in project repository
- Include relevant logs and configuration (redact secrets!)
