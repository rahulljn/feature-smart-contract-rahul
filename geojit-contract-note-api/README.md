# Geojit Smart Contract Note — Backend API

Spring Boot 3.2.5 · Java 17 · PostgreSQL 15 · JWT RS256 · AWS SDK v1

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 17+ |
| Maven | 3.9+ |
| Docker & Docker Compose | 24+ |
| OpenSSL | Any recent version |
| AWS CLI | Configured with `~/.aws/credentials` |

---

## 1. AWS Credentials

The application uses `DefaultAWSCredentialsProviderChain`, so your AWS credentials must be available on the machine.

```bash
# Verify credentials are configured
aws sts get-caller-identity
```

Ensure the IAM principal has permissions for: `s3:*`, `sqs:*`, `lambda:InvokeFunction`.

---

## 2. RSA Key Pair (JWT RS256)

Keys are **already generated** and placed at:

```
src/main/resources/keys/private.pem
src/main/resources/keys/public.pem
```

To regenerate them (e.g., for a fresh environment):

```bash
cd src/main/resources/keys/

# Generate 2048-bit RSA private key
openssl genrsa -out private.pem 2048

# Extract public key
openssl rsa -in private.pem -pubout -out public.pem
```

> **Important:** Never commit `private.pem` to version control. Add `src/main/resources/keys/` to `.gitignore`.

---

## 3. Start PostgreSQL via Docker Compose

From the project root:

```bash
docker compose up -d
```

This starts a PostgreSQL 15 container:

| Parameter | Value |
|-----------|-------|
| Host | `localhost` |
| Port | `5432` |
| Database | `geojit_contract_note` |
| Username | `geojit` |
| Password | `geojit@local123` |

Verify it's healthy:

```bash
docker compose ps
# Status should show "healthy"
```

---

## 4. Flyway Migrations

Flyway runs automatically on application startup. It applies all migrations in order:

| Migration | Description |
|-----------|-------------|
| V1 | `users` table — seeds default admin |
| V2 | `jobs` table |
| V3 | `job_customers` table |
| V4 | `pipeline_events` table (JSONB) |
| V5 | `email_events` table |
| V6 | `audit_log` table (JSONB) |
| V7 | `email_templates` table — seeds live template |
| V8 | `certificates` table — seeds placeholder cert |
| V9 | `ses_config` (15 config sets) + `suppression_list` |

---

## 5. Run the Application

```bash
# From project root
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Or build and run as a JAR:

```bash
mvn clean package -DskipTests
java -jar target/geojit-contract-note-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

The server starts on **http://localhost:8080**.

---

## 6. Default Credentials

| Field | Value |
|-------|-------|
| Email | `admin@geojit.com` |
| Password | `Admin@123` |
| Role | `ADMIN` |

---

## 7. API Documentation (Swagger UI)

Once running, open:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api-docs
```

---

## 8. Health Check

```
http://localhost:8080/actuator/health
```

---

## 9. API Overview

All endpoints are prefixed with `/api/v1/`.

### Authentication
| Method | Path | Auth |
|--------|------|------|
| POST | `/auth/login` | Public |

### Jobs
| Method | Path | Role |
|--------|------|------|
| POST | `/jobs/upload` | OPS_MANAGER+ |
| GET | `/jobs` | Any |
| GET | `/jobs/{jobId}` | Any |
| GET | `/jobs/{jobId}/customers` | Any |

### Dashboard
| Method | Path | Role |
|--------|------|------|
| GET | `/dashboard/metrics` | Any |

### Clients
| Method | Path | Role |
|--------|------|------|
| GET | `/clients/search?q=` | Any |
| GET | `/clients/{partyCode}/pdfs` | Any |
| GET | `/clients/{partyCode}/reports` | Any |
| GET | `/clients/{partyCode}/timeline` | Any |
| GET | `/clients/{partyCode}/pdf/{s3Key}` | Any |

### Suppression
| Method | Path | Role |
|--------|------|------|
| GET | `/suppression` | Any |
| POST | `/suppression` | OPS_MANAGER+ |
| DELETE | `/suppression/{email}` | ADMIN |

### Pipeline
| Method | Path | Role |
|--------|------|------|
| GET | `/pipeline/events?jobId=` | Any |
| POST | `/pipeline/status-event` | Any (Lambda callback) |

### Users
| Method | Path | Role |
|--------|------|------|
| GET/POST/PUT/DELETE | `/users/**` | ADMIN only |

### Audit
| Method | Path | Role |
|--------|------|------|
| GET | `/audit` | ADMIN |

### Templates
| Method | Path | Role |
|--------|------|------|
| GET/POST/PUT | `/templates/**` | ADMIN |
| POST | `/templates/{id}/activate` | ADMIN |

### Config
| Method | Path | Role |
|--------|------|------|
| GET | `/config/ses` | ADMIN |
| GET | `/config/certificates` | ADMIN |
| GET | `/config/certificates/active` | Any |

### Webhooks (SES)
| Method | Path | Auth |
|--------|------|------|
| POST | `/webhooks/ses` | Public (AWS SNS) |

---

## 10. Project Structure

```
geojit-contract-note-api/
├── docker-compose.yml
├── pom.xml
└── src/
    └── main/
        ├── java/com/geojit/contractnote/
        │   ├── GeojitContractNoteApplication.java
        │   ├── config/
        │   │   ├── AppProperties.java
        │   │   ├── AwsConfig.java
        │   │   └── SecurityConfig.java
        │   ├── controller/          # 11 REST controllers
        │   ├── dto/                 # Request/Response DTOs
        │   ├── entity/              # 10 JPA entities
        │   ├── exception/           # GlobalExceptionHandler
        │   ├── repository/          # 10 JpaRepository interfaces
        │   ├── security/            # JWT filter + provider
        │   └── service/             # Business logic
        └── resources/
            ├── application.yml
            ├── application-local.yml
            ├── keys/
            │   ├── private.pem      # ⚠️ Never commit
            │   └── public.pem
            └── db/migration/        # V1–V9 Flyway scripts
```

---

## 11. Stopping

```bash
# Stop application: Ctrl+C

# Stop and remove Docker containers
docker compose down

# Remove volumes (wipes DB data)
docker compose down -v
```

---

## 12. Environment Summary

| Component | Where it runs |
|-----------|--------------|
| Spring Boot API | Local JVM (port 8080) |
| PostgreSQL | Docker (port 5432) |
| AWS S3 | Real AWS (configured via `~/.aws/credentials`) |
| AWS SQS | Real AWS |
| AWS Lambda | Real AWS |
| AWS SES | Real AWS |
