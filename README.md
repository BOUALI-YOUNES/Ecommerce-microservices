# Ecommerce Microservices

Spring Boot microservices e-commerce application with an API gateway, centralized configuration, service discovery, JWT-based security via Keycloak, Kafka event streaming, and distributed tracing with Zipkin.

Java 21, Spring Boot, Spring Cloud.

## Architecture

```
                        +------------------+
   client  ----------->  |    API Gateway   |  :8080  (WebFlux, JWT)
                        +--------+---------+
                                 |
        +-------------+----------+----------+-------------+
        |             |                     |             |
  +-----------+ +-----------+       +-----------+ +-----------+
  | Customer  | |  Product  |       |   Order   | |  Payment  |
  |  :8090    | |   :8050   |       |   :8070   | |   :8055   |
  | MongoDB   | | Postgres  |       |  Postgres | |  Postgres |
  +-----------+ +-----------+       +-----+-----+ +-----+-----+
                                       | Kafka
                                    +--v-----------+
                                    | Notification |  :8040
                                    |    :8040     |  MongoDB -> MailDev
                                    +--------------+

  Infrastructure: Config Server :8888 | Discovery :8761 | Keycloak :9098
                  PostgreSQL :5433 | MongoDB :27017 | Kafka :9092
                  MailDev :1080/:1025 | Zipkin :9411
```

Order-service calls customer, product, and payment through the API gateway using OpenFeign, propagating both the JWT and the trace context.

## Services

| Service | Port | Database | Role |
|---|---|---|---|
| `config-server` | 8888 | - | Centralized configuration for all services |
| `discovery` | 8761 | - | Eureka service registry |
| `api-gateway` | 8080 | - | Reactive gateway, JWT validation, load-balanced routing |
| `customer-service` | 8090 | MongoDB | Customer registration and profile CRUD |
| `product-service` | 8050 | PostgreSQL | Product catalog and stock reservation |
| `order-service` | 8070 | PostgreSQL | Order creation, orchestrates payment and confirmation |
| `payement` | 8055 | PostgreSQL | Payment processing, publishes payment confirmations |
| `notification-service` | 8040 | MongoDB | Consumes Kafka events, sends email via SMTP |

Note: the payment service directory is spelled `payement` in this repository.

## Prerequisites

- Docker with Compose v2
- JDK 21 and Maven for local builds
- Host memory: allocate at least 8 GB to Docker. Fifteen services, eight of them JVMs, will exhaust a 6 GB allocation and cause the daemon to drop containers.

## Configuration

Copy `.env.example` to `.env` and set the values. `.env` is gitignored and must not be committed.

| Variable | Purpose |
|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | PostgreSQL credentials for product, order, payment |
| `MONGO_USER` / `MONGO_PASSWORD` | MongoDB credentials for customer, notification |
| `MAIL_USER` / `MAIL_PASSWORD` | SMTP credentials (left empty for MailDev) |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | Keycloak bootstrap admin |

Configuration for every service lives in `services/config-server/src/main/resources/configurations/`, keyed by application name.

### Gateway issuer configuration

The gateway fetches signing keys over the Docker network but validates the issuer against the host-visible URL, because tokens minted from the host carry the external `iss` claim:

```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri: http://keycloak:8080/realms/ecom-realm/protocol/openid-connect/certs
keycloak.issuer-uri: http://localhost:9098/realms/ecom-realm
```

This requires an explicit `ReactiveJwtDecoder` bean in `SecurityConfig`; `Customizer.withDefaults()` cannot express the split.

Gateway routes use the `spring.cloud.gateway.server.webflux.routes` namespace. The legacy `spring.cloud.gateway.routes` key does not bind under Spring Cloud 2025.x and silently produces no routes.

## Security

Keycloak runs in `start-dev` mode. A realm must be created and configured before the gateway can validate tokens; the gateway does not fail at startup if the realm is missing, requests simply return 401.

Realm expectations:

- Realm `ecom-realm`
- Client `ecom-frontend`, public, with direct access grants and standard flow enabled
- Realm roles `USER` and `ADMIN`

Access tokens expire after 300 seconds by default in dev mode. A 401 shortly after a successful call is usually expiry rather than a configuration fault.

Gateway authorization rules:

| Path | Access |
|---|---|
| `GET /api/v1/products/**` | Public |
| `POST /api/v1/customers` | Public (registration) |
| `DELETE /api/v1/customers/**` | `ADMIN` |
| `PUT /api/v1/customers/**` | `ADMIN` |
| `POST /api/v1/products` | `ADMIN` |
| `/eureka/**`, `/actuator/health` | Public |
| Everything else | Authenticated |

Realm roles from `realm_access.roles` are mapped to Spring Security authorities with a `ROLE_` prefix.

Direct access grants are enabled for local testing only. Password grants are deprecated and should be replaced with authorization code plus PKCE for any browser client.

## Running

All commands run from the `services/` directory.

Start the stack:

```bash
cd services
docker compose up -d
```

Stop it:

```bash
docker compose down
```

`config-server` and `discovery-server` gate the rest through healthchecks, so a full start takes a few minutes. Verify with:

```bash
docker ps
curl -s http://localhost:8761/eureka/apps | grep -o '<name>[A-Z-]*</name>'
```

Services are deployed from prebuilt JARs. The Dockerfiles copy `target/*.jar`, so run Maven before rebuilding an image or the image will contain a stale build:

```bash
mvn clean package -DskipTests -pl <service>
docker compose build --no-cache <service>
docker compose up -d --force-recreate <service>
```

## Obtaining a token

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:9098/realms/ecom-realm/protocol/openid-connect/token" \
  -d "client_id=ecom-frontend" \
  -d "grant_type=password" \
  -d "username=user" \
  -d "password=user123" |
  python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
```

Then call through the gateway:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/orders
```

The catalog is public, so no token is needed:

```bash
curl http://localhost:8080/api/v1/products
```

Replace `user`/`user123` with `admin`/`admin123` for a token carrying `ADMIN`.

## Email testing

MailDev captures mail from the notification service and exposes an HTTP API, though the endpoint is only reachable from inside the Docker network:

```bash
docker exec ms_api_gateway curl -s http://maildev:1080/api/email
```

The `/api/email` path is version-specific and returns 404 on other MailDev releases. The container log is the more reliable check:

```bash
docker logs ms_mail_dev --since 5m | grep -A1 Received
```

MailDev's own healthcheck in this project is unreliable and reports `unhealthy` while SMTP is working correctly. Ignore the flag unless a `depends_on: service_healthy` condition is actually gating startup on it.

## Distributed tracing

Zipkin runs on port 9411. Trace propagation is configured across Feign clients and the gateway, so an order request produces one trace spanning gateway, order, customer, product, and payment.

```bash
curl -s "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=10"
```

## Known issues

**Docker memory exhaustion.** With a 6 GB Docker allocation the stack runs out of memory and individual services are OOM-killed, or the daemon drops the whole stack during startup. Every JVM service sets `-XX:MaxRAMPercentage=45` so the services share the allocation predictably rather than each defaulting to a quarter of the VM.

**Stale prebuilt JARs.** Because images are built from `target/*.jar`, editing source without running Maven leaves the old class in the running container. When behaviour contradicts the source, compare the JAR checksums and inspect the class path inside the image before debugging further.

**Consumer offset errors.** The notification service logs a deserialization error on `order-topic-0` at a stale offset when a record predates the current event class shape. The consumer skips it and new records deserialize normally; it does not block mail delivery.

**Payment validation codes.** `PUT /api/v1/customers/{id}` returns 405 and `POST /api/v1/products` returns 400 with validation errors. Both mean authorization passed and the request reached the service; the codes come from missing controller mappings and bean validation, not from the gateway.

## Repository layout

```
services/
  config-server/     centralized configuration
  discovery/         Eureka server
  api-gateway/       gateway, security rules, JWT validation
  customer/          customer service
  product/           product service
  order/             order service, Feign clients
  payement/          payment service
  notification/      Kafka consumer, email sender
  keycloak/          realm export
  docker-compose.yml
  .env               local secrets, gitignored
```