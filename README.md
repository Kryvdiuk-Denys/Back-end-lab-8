# Coworking Microservices

Java 17 and Spring Boot 3.3 application split into independent Maven modules. `workspace-service` owns workspace data, `booking-service` owns booking data, and `common` contains the shared `WorkspaceDto` API contract. Booking calls the workspace REST API; there are no Maven dependencies between the services.

## Stack

- Java 17, Spring Boot 3.3
- Spring MVC, Thymeleaf UI
- Spring Data JPA, PostgreSQL
- Liquibase migrations
- MapStruct DTO mapping
- Spring Security with OIDC login for UI and JWT bearer tokens for API
- Keycloak realm for users and roles

## Run

Prerequisites:

- JDK 17
- Maven
- Docker with Docker Compose

From the project directory, build and verify the Maven reactor:

```bat
mvn verify
```

Start the full stack with Docker Compose (Maven is not required on the host):

```powershell
docker compose up -d --build
```

Booking UI and API: `http://localhost:8080`  
Workspace service API: `http://localhost:8082/api/workspaces`  
Keycloak admin console: `http://localhost:8081`

The booking service proxies `/api/workspaces` so existing clients can keep using port 8080. Each service has its own `application.yml`, main class, tests, Dockerfile, port, and database. The booking schema stores a workspace ID and name snapshot without a foreign key to the workspace database. Compose uses separate fresh volumes for booking and workspace data; the previous single-database volume is left intact.

Admin login: `admin` / `admin`  
Realm: `coworking`

The root Maven project is an aggregator. To run the services directly from the host, start PostgreSQL and Keycloak with `docker compose up -d postgres workspace-db keycloak`, then run each service in its own terminal. Build the service jar with `-am` so Maven also builds `common`:

```powershell
mvn -pl workspace-service -am package
java -jar .\workspace-service\target\workspace-service-0.1.0.jar
```

```powershell
mvn -pl booking-service -am package
java -jar .\booking-service\target\booking-service-0.1.0.jar
```

If Keycloak was already running before the realm file was changed, recreate it:

```bat
docker compose down
docker compose up -d
```

If `docker` is not recognized, install Docker Desktop and restart the terminal.

If `mvn` is not recognized, install Apache Maven and add its `bin` directory to `PATH`, or run the project from IntelliJ IDEA using the Maven tool window. This project does not currently include Maven Wrapper files, so `mvnw.cmd` is not available.

If Maven is not installed locally but Docker and Java are available, the Maven image can build both jars for host execution:

```powershell
$env:Path += ";C:\Program Files\Docker\Docker\resources\bin"
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn -pl booking-service -am -DskipTests package
docker run --rm -v "${PWD}:/workspace" -w /workspace maven:3.9-eclipse-temurin-17 mvn -pl workspace-service -am -DskipTests package
java -jar .\booking-service\target\booking-service-0.1.0.jar
java -jar .\workspace-service\target\workspace-service-0.1.0.jar
```

## Demo Users

| Username | Password | Role    |
|----------|----------|---------|
| admin    | admin    | ADMIN   |
| manager  | manager  | MANAGER |
| user     | user     | USER    |

## UI Scenarios

Open `http://localhost:8080`.

- Unauthenticated users are redirected to Keycloak.
- `admin` can create, read, update, and delete bookings and workspaces.
- `manager` can create and update bookings/workspaces, but cannot delete.
- `user` can create bookings and read data.
- `/admin` is protected by `SecurityFilterChain` and requires `ADMIN`.

## API Scenarios

Ready HTTP requests are in `http/coworking-booking.http`.

The file covers:

- getting JWT tokens from Keycloak
- CRUD for `/api/workspaces`
- CRUD for `/api/bookings`
- `403 Forbidden` from `SecurityFilterChain` on `/api/admin/status`
- `403 Forbidden` from `@PreAuthorize` on `DELETE /api/bookings/{id}` for non-admin users

## Async Booking Work

Creating a booking queues three simulated slow side effects in the `bookingTaskExecutor`: confirmation email, receipt PDF generation, and access-service synchronization. The HTTP call does not wait for them. The executor uses a four-thread pool, a bounded queue of 50 tasks, and the `booking-async-` thread prefix. When the queue is full, the API returns `503 Service Unavailable` with a retry message.

`POST /api/bookings/preflight` starts independent availability and price-estimate calls, then combines their results. The caller waits for at most 2.5 seconds. An individual call failure is converted into a `CHECK_FAILED` result; a timeout returns `503`.

The async settings and simulated delays are in `src/main/resources/application.yml`. `mvn test` covers fire-and-forget behavior, executor threads and concurrency, exception handling, bounded waits, and queue rejection.

## Quartz Scheduler

Quartz uses a PostgreSQL JDBC job store. Liquibase adds its `QRTZ_*` tables through `db/changelog/003-quartz-schema.yaml`; Quartz schema initialization is disabled so restarts do not drop persisted schedules. The reminder job runs every 15 minutes and daily, while expired confirmed bookings are marked completed each day. Both jobs use `@DisallowConcurrentExecution`, persistent job data, and service methods with transaction boundaries.

Trigger values override job defaults through Quartz's merged `JobDataMap`. Each cron trigger has a `DO_NOTHING` misfire policy and uses `booking.quartz.time-zone` (default `Europe/Kyiv`, invalid IDs fall back to GMT). Reminder delivery is represented by a persisted `reminder_sent_at` marker and a log entry; repeated runs therefore skip reminders already processed. Job failures are logged and left for the next scheduled fire.

Manual token request example:

```bash
curl -X POST http://localhost:8081/realms/coworking/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=coworking-api" \
  -d "client_secret=api-secret" \
  -d "username=user" \
  -d "password=user"
```

Use `access_token` as:

```text
Authorization: Bearer <token>
```

## Acceptance Criteria Mapping

| AC | Implementation |
|----|----------------|
| AC1 | Booking and workspace CRUD APIs run in separate service modules with independent controllers, services, repositories, and entities. |
| AC2 | Database schema is created by Liquibase changelog `db/changelog/001-init-schema.yaml`; Hibernate uses `ddl-auto: validate`. |
| AC3 | Thymeleaf UI supports core CRUD scenarios for bookings and workspaces. |
| AC4 | UI uses OIDC login through Keycloak and returns with an authenticated session. |
| AC5 | `/api/**` accepts `Authorization: Bearer <token>` JWT requests without UI session. |
| AC6 | `/api/admin/**` and `/admin/**` require `ADMIN` in `SecurityFilterChain`; non-admin receives 403. |
| AC7 | Service delete/update methods use `@PreAuthorize`; non-admin delete is blocked by method security. |
| AC8 | `http/coworking-booking.http` contains ready demonstration requests. |

## Multi-module lab

The root `pom.xml` has `pom` packaging and aggregates `common`, `workspace-service`, and `booking-service`. Shared test dependency management is declared in the root POM. The service modules both depend on the DTO-only `common` module, while booking communicates with workspace over HTTP. Run `mvn verify` from this directory to build and verify the full reactor.
