# Ecommerce Microservices

Spring Boot microservices e-commerce application with an API gateway, centralized configuration, service discovery, JWT-based security via Keycloak, Kafka event streaming, and distributed tracing with Zipkin.

Java 21, Spring Boot 4, Spring Cloud.

## Architecture

```
client ---> API Gateway :8080 ---> Customer | Product | Order | Payment
              (WebFlux, JWT)              (validate the JWT themselves)
                                               |
                          Order ---------------+-------> Kafka ---> Notification ---> SMTP
                            |                                                  (MongoDB)
              customer / product / payment  via lb:// + relayed JWT
```

**Only the gateway and the infrastructure publish host ports.** The business services
(`customer`, `product`, `order`, `payment`, `notification`) listen on container-internal
ports only, so they cannot be reached from the host except through the gateway, and
they verify the JWT themselves rather than trusting the network boundary.

| Service | Container port | Database | Role |
|---|---|---|---|
| `config-server` | 8888 | - | Centralized configuration for all services |
| `discovery` | 8761 | - | Eureka service registry |
| `api-gateway` | 8080 | - | Reactive gateway, JWT validation, load-balanced routing |
| `customer` | 8090 | MongoDB | Profile CRUD, bound to the Keycloak subject |
| `product` | 8050 | PostgreSQL | Catalog and stock reservation/release |
| `order` | 8070 | PostgreSQL | Order creation, orchestrates customer/product/payment |
| `payement` | 8055 | PostgreSQL | Payment processing, publishes payment confirmations |
| `notification` | 8040 | MongoDB | Consumes Kafka events, sends email via SMTP |

Published host ports: gateway `8080`, config `8888`, discovery `8761`, Keycloak `9098`,
PostgreSQL `5433`, MongoDB `27017`, Kafka `9092`, Zookeeper `2181`, Zipkin `9411`,
MailDev `1080`/`1025`, pgAdmin, mongo-express `8081`.

Note: the payment service directory is spelled `payement` in this repository.

### Service-to-service calls

`order` calls `customer`, `product`, and `payment` directly through Eureka
(`lb://product-service`, ...) using `RestTemplate`, not through the gateway. Each call
reuses the inbound `Authorization` header so the downstream service authorizes the real
end user, and injects the tracing context so one trace spans the whole order.

Config for these clients lives in `order-service.yml` under `application.config.feign.*`
(connect and read timeouts, also feeding the Apache HttpClient settings) and
`feign.httpclient.*`. The `feign` key names are historical: the clients are
`RestTemplate` beans, not Feign, but the timeout properties are still read from there.

JWT relay is a `bearerTokenInterceptor()` in `order/.../config/RestTemplateConfig.java`
that copies the inbound bearer token onto the outbound request, so the downstream service
authorizes the end user rather than a shared service identity.

## Prerequisites

- Docker with Compose v2
- JDK 21 and Maven for local builds
- **Memory: this is the main operational constraint.** Docker Desktop runs in a VM; on a
  16 GB host the VM is typically capped at 6 GB and `qemu-system-x86` alone occupies
  ~4.3 GB of host RAM. The stack runs nine JVMs (six business services plus Keycloak,
  config server, and discovery). See [Known issues](#known-issues) before first start.

## Configuration

Copy `.env.example` to `.env` and set the values. `.env` is gitignored and must not be committed.

| Variable | Purpose |
|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | PostgreSQL credentials for product, order, payment |
| `MONGO_USER` / `MONGO_PASSWORD` | MongoDB credentials for customer, notification |
| `MAIL_USER` / `MAIL_PASSWORD` | SMTP credentials (left empty for MailDev) |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | Keycloak bootstrap admin |

Configuration for every service lives in `services/config-server/src/main/resources/configurations/`, keyed by application name.

There is **no aggregator POM**. Build each service from its own directory (below).

### Gateway issuer configuration

The gateway fetches signing keys over the Docker network but validates the issuer against the host-visible URL, because tokens minted from the host carry the external `iss` claim:

```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri: http://keycloak:8080/realms/ecom-realm/protocol/openid-connect/certs
keycloak.issuer-uri: http://localhost:9098/realms/ecom-realm
```

This requires an explicit `ReactiveJwtDecoder` bean in `SecurityConfig`; `Customizer.withDefaults()` cannot express the split. The same split is repeated in every business service's resource-server configuration.

Gateway routes use the `spring.cloud.gateway.server.webflux.routes` namespace. The legacy `spring.cloud.gateway.routes` key does not bind under Spring Cloud 2025.x and silently produces no routes.

### Circuit breakers

`order` wraps its outbound calls in Resilience4j circuit breakers. A rejected purchase
(insufficient stock, invalid payload) is normal business behaviour, **not** an outage, so
`productService` ignores `ProductPurchaseRejectedException`:

```yaml
resilience4j.circuitbreaker.instances.productService:
    ignore-exceptions:
        - com.younes.order.exception.ProductPurchaseRejectedException
```

Do **not** add `record-exceptions` here. `record-exceptions` is an allowlist that
overrides `ignore-exceptions`, and because the rejection type extends `BusinessException`,
listing the supertype records the rejections again. The circuit then opens after ten
failed orders and every later order fails with "product service is currently
unavailable", hiding the real cause.

## Security

Keycloak runs in `start-dev` mode with the realm in `services/keycloak/realm-export.json`
mounted at `/opt/keycloak/data/import`, so the realm is created on first boot.

Realm expectations:

- Realm `ecom-realm`
- Client `ecom-frontend`, public, with direct access grants and standard flow enabled
- Realm roles `USER` and `ADMIN`

Test users:

| User | Password | Roles |
|---|---|---|
| `user` | `user123` | `USER` |
| `admin` | `admin123` | `ADMIN`, `USER` |

Access tokens expire after **1800 seconds**. A 401 long after a successful call is expiry
rather than a configuration fault.

Realm roles from `realm_access.roles` are mapped to Spring Security authorities with a
`ROLE_` prefix. `customer` and the gateway each declare their own
`JwtAuthenticationConverter`; a plain `Customizer.withDefaults()` does not map realm roles.

### Gateway authorization rules

| Path | Access |
|---|---|
| `GET /api/v1/products/**` | Public |
| `POST /api/v1/products/purchase/release` | **Denied to everyone** |
| `GET /api/v1/customers` (list) | `ADMIN` |
| `DELETE /api/v1/customers/**` | `ADMIN` |
| `POST /api/v1/products` | `ADMIN` |
| `POST /api/v1/customers`, `PUT /api/v1/customers/**` | Authenticated |
| `/actuator/health` | Public |
| Everything else | Authenticated |

`POST /api/v1/products/purchase/release` is the stock-compensation endpoint that `order`
calls internally when an order is rolled back. It is reachable in the container network,
so the gateway denies it explicitly. Without that rule any authenticated user could call
it and inflate stock at will (a single call was verified taking a product from 4 to 54
units). Service-to-service calls bypass the gateway, so order placement is unaffected.

### Authorization inside the services

The gateway is a convenience, not the security boundary; every service re-checks.

- `product`: only `GET` endpoints and the health probe are public; all mutations require a token.
- `customer`: a profile is owned by the Keycloak subject. Reads and updates are limited
  to the owner, and `GET /api/v1/customers/{id}` and `/exists/{id}` return `403` for a
  different subject even though the caller is authenticated. Listing and deleting require `ADMIN`.
- `order`: queries are scoped to the authenticated customer's own orders.
- `payment`: requires a token; the client cannot choose its own id or amount.

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

`config-server` and `discovery-server` gate the rest through healthchecks, so a full start
takes several minutes. Verify with:

```bash
docker compose ps
curl -s http://localhost:8761/eureka/apps | grep -o '<name>[A-Z-]*</name>'
```

All six business services should appear in Eureka. See [Known issues](#known-issues) if
containers are missing or show `Exited (137)`.

### Rebuilding after a change

Images copy `target/*.jar`, so Maven must run first. There is no aggregator POM, so build
each service in its own directory:

```bash
(cd order && mvn clean package)
docker compose build order-service
docker compose up -d --force-recreate order-service
```

**Changing only `config-server` is different, and is the easiest mistake in this repo.**
Editing a file under `config-server/src/main/resources/configurations/` does nothing until
the config-server JAR and image are rebuilt *and* the dependent services are restarted.
`docker compose up -d config-server` will not restart the services that read from it,
because their images have not changed:

```bash
(cd config-server && mvn clean package)
docker compose build config-server
docker compose up -d config-server
docker compose restart order-service payment-service
```

Verify the change actually reached a running service before debugging anything else:

```bash
curl -s http://localhost:8888/order-service/default | python3 -m json.tool | grep <key>
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

### Placing an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"reference":"ORDER-1","paymentMethod":"VISA",
       "products":[{"productId":1,"quantity":2}]}'
```

`id`, `amount`, and `customerId` in the request body are ignored if present: the order id
is generated, the total is computed server-side with `BigDecimal`, and the customer comes
from the JWT subject. Stock is decremented in one transaction, and any failure after that
point triggers a compensating release back to the product service.

Insufficient stock returns `400` with the product service's own message. Repeated
rejections do not open the circuit breaker, so a subsequent valid order still succeeds.

## Email testing

MailDev captures mail from the notification service. The HTTP API is reachable only from
inside the Docker network:

```bash
docker exec services-notification-service-1 curl -s http://maildev:1080/api/email
```

The container log is the more reliable check:

```bash
docker logs ms_mail_dev --since 5m | grep -A1 Received
```

MailDev's own healthcheck in this project is unreliable and reports `unhealthy` while SMTP
works correctly. Ignore the flag unless a `depends_on: service_healthy` condition actually
gates startup on it.

## Distributed tracing

Zipkin runs on port 9411. Trace context is propagated by the gateway and by the `order`
service's client interceptor, so an order produces one trace spanning gateway, order,
customer, product, and payment.

```bash
curl -s http://localhost:9411/api/v2/services
curl -s "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=10"
```

Verified reporting: `customer-service`, `notification-service`, `order-service`,
`payment-service`, `product-service`.

## Tests

Each service has a context-load test; `product` additionally has six stock-management
tests covering concurrent reservation, rollback, duplicate order lines, and oversell.

```bash
for s in config-server discovery api-gateway customer product order payement notification; do
  (cd $s && mvn -o clean package) || break
done
```

15 tests in total. Services run these with optional config-server imports so they do not
need the stack to be up.

## Known issues

**Docker memory exhaustion.** This is the dominant operational problem. The Docker VM is
capped at 6 GB on a 16 GB host and the nine JVMs do not fit; the kernel OOM-kills
containers, which surface as `Exited (137)` and cascade, taking healthy services with
them. Every JVM service sets `-XX:MaxRAMPercentage=45`, has a Compose memory limit, and
exits rather than lingering in a swap-thrashing state. Start the stack in stages
(infrastructure, then catalog, then order/gateway) and restart anything that shows
`Exited (137)`. If the daemon itself stops, restart Docker Desktop.

**Stale prebuilt JARs.** Because images are built from `target/*.jar`, editing source
without running Maven leaves the old class in the running container. The same trap applies
to config-server YAML files; see [Rebuilding after a change](#rebuilding-after-a-change).

**Non-durable stock compensation.** When an order fails after stock was reserved, the
release call has a circuit breaker and a fallback, but there is no retry queue or outbox.
If the release itself fails the fallback logs the stranded stock for manual
reconciliation. A durable outbox plus a reconciler would be the next step.

**Release endpoint reachable in the container network.** The gateway denies
`/api/v1/products/purchase/release`, and the services share one Docker network, so a
compromised container could still call it. Restricting it properly needs a service
identity (client credentials with its own scope) rather than the end user's relayed token.

**Consumer offset errors.** The notification service logs a deserialization error on
`order-topic-0` at a stale offset when a record predates the current event class shape. It
skips the record and new records deserialize normally; it does not block mail delivery.

**Authorization codes that mean authorization passed.** A `400` with validation errors
from `POST /api/v1/products`, or a `404` for a missing product, means the request reached
the service. Check the service logs rather than the gateway.

## Repository layout

```
services/
  config-server/     centralized configuration
  discovery/         Eureka server
  api-gateway/       gateway, security rules, JWT validation
  customer/          customer service
  product/           product service
  order/             order service, lb:// clients
  payement/          payment service
  notification/      Kafka consumer, email sender
  keycloak/          realm export
  docker-compose.yml
  .env               local secrets, gitignored
```