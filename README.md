<div align="center">

# 🛒 Ecommerce Microservices

### Production-shaped Spring Boot microservices with JWT auth, Kafka events and Zipkin tracing

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6db33f?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.1.3-6db33f?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![Docker](https://img.shields.io/badge/Docker-29-blue?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-7.7-blue?style=flat-square&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Keycloak](https://img.shields.io/badge/Keycloak-24-red?style=flat-square&logo=keycloak&logoColor=white)](https://www.keycloak.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7-47a248?style=flat-square&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Zipkin](https://img.shields.io/badge/Zipkin-3-5cb3c8?style=flat-square&logo=zipkin&logoColor=white)](https://zipkin.io/)
[![Maven](https://img.shields.io/badge/Maven-3.8.7-blue?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

**8 services** · **JWT validated in every service** · **17 containers, all memory-capped**

</div>

---

## 📋 What's in here

| | |
|---|---|
| 🔐 **Security** | Keycloak JWT validated by the gateway **and** by all five business services. Realm roles mapped to `ROLE_*`. Owner-scoped customer data, admin-only catalog and listing. |
| 💰 **Correctness** | The server generates the order id, computes the total from product prices and resolves the customer from the token. Clients cannot set any of the three. |
| 📦 **Stock** | Atomic conditional updates so concurrent orders cannot oversell, with compensating release when an order fails after reserving. |
| 🧠 **Resilience** | Circuit breakers that treat an out-of-stock rejection as normal business rather than an outage. |
| 📊 **Observability** | Zipkin traces spanning gateway → order → customer → product → payment, plus Actuator health on every JVM service. |
| 🐳 **Operations** | Every container memory-capped, every JVM heap pinned, every service started and health-gated in dependency order. |

---

## 🏗️ Architecture

```mermaid
%%{init: {"theme":"base","themeVariables":{"fontSize":"17px"},"flowchart":{"nodeSpacing":2,"rankSpacing":50,"padding":6,"curve":"basis"}}}%%
graph TB
    CLIENT(["🖥️  Client"])

    GW["🛡️  API Gateway<br/>:8080"]
    KC["🔑  Keycloak<br/>:9098"]

    CU["👤  Customer<br/>:8090 · MongoDB"]
    PR["📦  Product<br/>:8050 · PostgreSQL"]
    OR["🧾  Order<br/>:8070 · PostgreSQL"]
    PAY["💳  Payment<br/>:8055 · PostgreSQL"]
    NT["📨  Notification<br/>:8040 · MongoDB"]

    KFK[("📨  Kafka")]

    PG[("🗄️  PostgreSQL")]
    MG[("🍃  MongoDB")]

    CFG["⚙️  Config<br/>:8888"]
    EU["🧭  Discovery<br/>:8761"]
    ZP["🔍  Zipkin<br/>:9411"]
    MD["📮  MailDev<br/>:1080"]

    CLIENT -->|HTTPS| GW
    CLIENT -.->|token| KC
    GW -.->|validate| KC

    GW ==> CU & PR & OR
    OR ==>|lb://| CU & PR & PAY

    OR -->|order-topic| KFK
    PAY -->|payment-topic| KFK
    KFK --> NT

    CU --> MG
    NT --> MG
    NT --> MD
    PR & OR & PAY --> PG

    CFG -.->|config| CU
    EU -.->|registry| GW
    OR -.-> ZP

    classDef cli fill:#111827,stroke:#000000,color:#ffffff,stroke-width:3px
    classDef edge fill:#1f6feb,stroke:#0b3f8f,color:#ffffff,stroke-width:3px
    classDef app fill:#1a7f37,stroke:#0d4a20,color:#ffffff,stroke-width:3px
    classDef bus fill:#7c3aed,stroke:#4a1d95,color:#ffffff,stroke-width:3px
    classDef data fill:#0e6e75,stroke:#05383d,color:#ffffff,stroke-width:3px
    classDef plat fill:#b45309,stroke:#71350f,color:#ffffff,stroke-width:3px
    class CLIENT cli
    class GW,KC edge
    class CU,PR,OR,PAY,NT app
    class KFK bus
    class PG,MG data
    class CFG,EU,ZP,MD plat

    GW ~~~ CU ~~~ KFK ~~~ PG ~~~ CFG
```

| Colour | Tier |
|---|---|
| ⬛ black | Client |
| ⬛ blue | Edge — gateway and identity |
| 🟩 green | Application services |
| 🟪 purple | Event bus |
| 🟦 teal | Data stores |
| 🟧 orange | Platform — config, discovery, tracing, mail |

Solid arrows are synchronous calls, thick arrows are load-balanced `lb://` calls, and dotted
arrows are configuration or telemetry rather than request traffic.

### 🧬 Domain model

```mermaid
%%{init: {"theme":"base","themeVariables":{"fontSize":"16px"},"flowchart":{"nodeSpacing":6,"rankSpacing":54,"padding":7,"curve":"basis"}}}%%
graph TB

    CUSTOMER["👤 Customer<br/>id · keycloakId · email<br/>firstname · lastname"]
    ADDRESS["🏠 Address<br/>street · houseNumber · zipCode"]

    CATEGORY["🗂️ Category<br/>id · name · description"]
    PRODUCT["📦 Product<br/>id · name · description<br/>price · availableQuantity"]

    ORDER["🧾 Order<br/>id · reference · totalAmount<br/>customerId · status"]
    ORDERLINE["📄 OrderLine<br/>id · productId · quantity"]
    OSTATUS["🚦 OrderStatus<br/>PENDING · PAID · FAILED · CANCELLED"]
    PMETHOD["💳 PaymentMethod<br/>PAYPAL · MASTER_CARD · VISA · BITCOIN"]

    PAYMENT["🧾 Payment<br/>id · amount · orderId<br/>paymentMethode · createdAt"]

    CONF["📨 OrderConfirmation<br/>orderReference · totaleAmount<br/>paymentMethod"]
    NOTIF["📨 Notification<br/>id · notificationType · notificDateTime"]

    CUSTOMER --> ADDRESS
    CATEGORY --> PRODUCT
    ORDER --> ORDERLINE
    ORDER -.-> OSTATUS
    ORDER -.-> PMETHOD
    PAYMENT -.-> PMETHOD
    CONF -.-> PMETHOD

    CUSTOMER ==>|order.customerId| ORDER
    PRODUCT ==>|orderLine.productId| ORDERLINE
    ORDER ==>|payment.orderId| PAYMENT
    ORDER -->|order-topic| CONF
    PAYMENT -->|payment-topic| CONF
    CONF --> NOTIF

    classDef cus fill:#1f6feb,stroke:#0b3f8f,color:#fff,stroke-width:3px
    classDef cat fill:#0e6e75,stroke:#05383d,color:#fff,stroke-width:3px
    classDef ord fill:#1a7f37,stroke:#0d4a20,color:#fff,stroke-width:3px
    classDef bil fill:#7c3aed,stroke:#4a1d95,color:#fff,stroke-width:3px
    classDef msg fill:#b45309,stroke:#71350f,color:#fff,stroke-width:3px
    classDef aux fill:#374151,stroke:#111827,color:#fff,stroke-width:2px

    class CUSTOMER,ADDRESS cus
    class CATEGORY,PRODUCT cat
    class ORDER,ORDERLINE ord
    class PAYMENT bil
    class CONF,NOTIF msg
    class OSTATUS,PMETHOD aux

    CUSTOMER ~~~ CATEGORY ~~~ ORDER ~~~ CONF
```

<details>
<summary><b>🔑 The distinction that matters here</b></summary>

**Solid arrows are real foreign keys**, and they only ever exist *inside* one service:

- `Category 1──▸ n Product` — `@OneToMany`, same PostgreSQL database
- `Order 1──▸ n OrderLine` — `@OneToMany` + `@JoinColumn(order_id)`, same database

**Thick arrows are references across a service boundary.** They are plain integers or
strings, resolved over `lb://`, and there is **no database-level integrity between them**:

- `order.customerId` → `customer.id` — taken from the JWT subject, so a client cannot set it
- `orderLine.productId` → `product.id` — resolved during the purchase call
- `payment.orderId` → `order.id`

That is the trade-off of microservices: `order` owns the *transaction* across three services,
which is why it needs the compensating release when a later step fails. There is no
distributed transaction, so consistency is per-service plus that compensation.

Note the deliberately duplicated `Customer` types: the one in `customer-service` is a Mongo
document, the one inside `OrderConfirmation` is a Kafka event payload. They are separate
types in separate processes, not a shared class.
</details>

### 🧱 Class diagram

```mermaid
%%{init: {"theme":"base","themeVariables":{"fontSize":"13px"},"flowchart":{"nodeSpacing":4,"rankSpacing":40,"padding":4}}}%%
classDiagram
    direction LR

    class Category { -Integer id -String name }
    class Product { -Integer id -String name -double availableQuantity -BigDecimal price }
    Category "1" *-- "0..*" Product

    class Order { -Integer id -String reference -BigDecimal totalAmount -String customerId }
    class OrderLine { -Integer id -Integer productId -double quantity }
    class OrderStatus { <<enumeration>> PENDING PAID FAILED CANCELLED }
    class PaymentMethod { <<enumeration>> PAYPAL MASTER_CARD VISA BITCOIN }
    Order "1" *-- "0..*" OrderLine
    Order --> OrderStatus
    Order --> PaymentMethod

    class Customer { -String id -String keycloakId -String email }
    class Address { -String street -String houseNumber -String zipCode }
    Customer --> Address

    class Payment { -Integer id -BigDecimal amount -Integer orderId }
    Payment --> PaymentMethod

    class Notification { -String id -NotificationType type }
    class NotificationType { <<enumeration>> PAYMENT_CONFIRMATION ORDER_CONFIRMATION }
    class OrderConfirmation { -String orderRef -BigDecimal total }
    class PaymentConfirmation { -String productRef -BigDecimal amount }
    Notification --> NotificationType
    Notification --> OrderConfirmation
    Notification --> PaymentConfirmation
```

<details>
<summary><b>🎯 What this diagram is for</b></summary>

This is deliberately **not** a map of every class. The entities and their fields are already in
the domain model above, so repeating them here would only make the diagram unreadable. What is
left is the part that carries the correctness guarantee:

- **`OrderService` holds both halves of the stock protocol** — `ProductClient` to reserve, and
  `OrderCompensation` to release. Everything needed to undo a partial order is wired into one
  object, which is what makes the rollback auditable.
- **`OrderCompensation` depends on `ProductClient`, not on `ProductService`.** The release is a
  second guarded call with its own circuit breaker, so a failing release cannot take down order
  creation.
- **`createOrder` takes `customerId` as a parameter.** The service resolves the buyer itself via
  `resolveCustomer()` from the validated JWT, so a client cannot order on someone else's account.
- `PaymentMethod` and `OrderStatus` are plain string-mapped enums — the database stores the
  constant name, not an ordinal.

The corresponding security fix lives in `CustomerService.findById(id, callerId, isAdmin)`:
making authorization a required argument rather than something the method looks up itself is what
removed the IDOR, because the old signature could not express *"is this my account?"*.
</details>

**Only the gateway and the platform publish host ports.** The five business services listen on
container-internal ports and validate the JWT themselves, so anything that reaches them on the
Docker network cannot skip authorization.

<details>
<summary><b>📡 How order calls the other services</b></summary>

`order` calls `customer` and `payment` via **OpenFeign clients** (`@FeignClient(name="...")`) and
`product` via a **load-balanced `RestTemplate`** (`lb://product-service`). All internal calls go
directly to services through Eureka — never through the API gateway.

- Each call copies the inbound `Authorization` header onto the outbound request, so the
  downstream service authorizes the **real end user** rather than a shared service identity.
- Timeouts live in `order-service.yml` under `application.config.feign.*`. The key name is
  historical; the clients are `RestTemplate` beans but the timeout properties are still read
  from there. The JWT relay interceptor is in `config/RestTemplateConfig.java`.
- The trace context is injected too, so one trace spans the whole order.
</details>

---

## 🚀 Services

| Service | Port | Database | Role |
|---|:---:|---|---|
| `config-server` | 8888 | — | Centralized configuration for every service |
| `discovery` | 8761 | — | Eureka registry |
| `api-gateway` | 8080 | — | Reactive gateway, JWT validation, load-balanced routing |
| `customer` | 8090 | MongoDB | Profile CRUD, bound to the Keycloak subject |
| `product` | 8050 | PostgreSQL | Catalog, stock reservation and release |
| `order` | 8070 | PostgreSQL | Order creation; orchestrates customer, product, payment |
| `payement` | 8055 | PostgreSQL | Payment processing, publishes payment confirmations |
| `notification` | 8040 | MongoDB | Consumes Kafka events, sends email via SMTP |

<details>
<summary>Published host ports</summary>

`api-gateway` 8080 · `config-server` 8888 · `discovery` 8761 · `keycloak` 9098 ·
`postgres` 5433 · `mongodb` 27017 · `kafka` 9092 · `zookeeper` 2181 · `zipkin` 9411 ·
`maildev` 1080 / 1025 · `pgadmin` 5000 · `mongo-express` 8081

Customer, product, order, payment and notification expose **no** host port.
</details>

> The payment service directory is spelled `payement` in this repository.

---

## 🧮 Memory

> [!IMPORTANT]
> **This is the main operational constraint of the project.**
> Docker Desktop runs in a VM, and on a 16 GB host that VM is typically capped at **6 GB** while
> `qemu-system-x86` alone occupies ~4.3 GB of host RAM. This stack runs **nine JVMs**.

Every one of the 17 containers has a `mem_limit`, and every JVM has an explicit `-Xmx` rather
than a percentage of the machine:

```mermaid
%%{init: {"theme":"base","themeVariables":{"fontSize":"16px"},"flowchart":{"nodeSpacing":2,"rankSpacing":44,"padding":6,"curve":"basis"}}}%%
graph TB

    BEFORE["❌ BEFORE<br/>12 uncapped<br/>4.59 GB actual"]
    VM[("🖥️  Docker VM<br/>6.0 GB")]
    AFTER["✅ AFTER<br/>17 capped<br/>3.98 GB actual"]

    G1["App JVMs (8)<br/>4.69 GB"]
    G2["Keycloak<br/>768 MB"]
    G3["Broker<br/>Kafka + Zookeeper<br/>896 MB"]
    G4["Data<br/>PostgreSQL + MongoDB<br/>640 MB"]
    G5["Tracing<br/>Zipkin<br/>384 MB"]
    G6["Dev tools<br/>pgAdmin + express<br/>+ MailDev<br/>672 MB"]

    CAPS["Sum of caps: 7.97 GB<br/>caps are ceilings,<br/>not reservations<br/>3.98 GB actually used"]

    BEFORE -->|"jvm sized against<br/>the whole VM"| VM
    VM -->|"explicit -Xmx<br/>+ mem_limit"| AFTER
    VM -.-> G1 & G2 & G3 & G4 & G5 & G6
    G1 & G2 & G3 & G4 & G5 & G6 -.-> CAPS

    classDef bad fill:#b91c1c,stroke:#7f1d1d,color:#fff,stroke-width:3px
    classDef vm fill:#111827,stroke:#000,color:#fff,stroke-width:3px
    classDef good fill:#15803d,stroke:#14532d,color:#fff,stroke-width:3px
    classDef grp fill:#0e6e75,stroke:#05383d,color:#fff,stroke-width:2px
    classDef note fill:#b45309,stroke:#71350f,color:#fff,stroke-width:2px
    class BEFORE bad
    class VM vm
    class AFTER good
    class G1,G2,G3,G4,G5,G6 grp
    class CAPS note

    BEFORE ~~~ G1 ~~~ CAPS
```

| Metric | Before | After |
|---|---:|---:|
| Memory actually in use | 4.59 GB | **3.98 GB** |
| Headroom under the 6 GB cap | 1.3 GB | **2.0 GB** |
| Containers with no limit | 12 | **0** |
| `Exited (137)` in one cascade | 8 | **0** |
| JVMs killed by a real OOM | 1 (`order`, metaspace) | **0** |

> [!NOTE]
> The caps add up to **7.97 GB**, which is deliberately *more* than the 6 GB VM. `mem_limit` is a
> ceiling, not a reservation — the containers that sit idle never approach theirs. What matters is
> the 3.98 GB actually used, which leaves real headroom for a burst.

<details>
<summary><b>⚠️ Why a percentage heap was the real bug</b></summary>

`-XX:MaxRAMPercentage=60` is resolved against whatever the JVM believes the machine has.
**With no `mem_limit`, that is the entire Docker VM** — so `api-gateway`, `config-server` and
`discovery` were each willing to take a 3.6 GB heap, and `-XX:InitialRAMPercentage=60` meant
they *committed* all of it at boot. Four such JVMs plus five capped ones overran the VM and the
kernel started killing containers.

Two rules now hold:

1. **`mem_limit` above `heap + metaspace + code cache + ~20%`** for thread stacks and direct buffers.
2. **Explicit `-Xmx`, `-XX:MaxMetaspaceSize`, `-XX:ReservedCodeCacheSize`** in each dockerfile,
   so the numbers are identical on every host and cannot drift.

`keycloak` has no dockerfile of ours, so its heap is bounded with `JAVA_OPTS` in compose instead.
`+ExitOnOutOfMemoryError` is kept everywhere: better a fast restart than a thrashing container.
</details>

---

## 🔐 Security

Keycloak runs in `start-dev` with the realm in `services/keycloak/realm-export.json` mounted at
`/opt/keycloak/data/import`, so the realm exists on first boot.

<details>
<summary><b>👥 Test users</b></summary>

| User | Password | Roles |
|---|---|---|
| `user` | `user123` | `USER` |
| `admin` | `admin123` | `ADMIN`, `USER` |

Access tokens expire after **1800 seconds**. A 401 long after a successful call is expiry, not a
configuration fault. Direct access grants are for local testing only — password grants are
deprecated and should be replaced with authorization code + PKCE for a browser client.
</details>

### Gateway authorization

| Path | Access | Result |
|---|---|:---:|
| `GET /api/v1/products/**` | Public | ✅ `200` |
| `POST /api/v1/products/purchase/release` | **Denied to everyone** | ✅ `403` |
| `GET /api/v1/customers` (list) | `ADMIN` | ✅ `USER` → `403`, `ADMIN` → `200` |
| `DELETE /api/v1/customers/**` | `ADMIN` | ✅ `204` |
| `POST /api/v1/products` | `ADMIN` | ✅ `USER` → `403` |
| `POST /api/v1/customers`, `PUT /api/v1/customers/**` | Authenticated | ✅ anon → `401` |
| Everything else | Authenticated | ✅ anon → `401` |

<details>
<summary><b>🚫 Why the stock-release endpoint is denied outright</b></summary>

`POST /api/v1/products/purchase/release` is the compensation endpoint `order` calls when an
order is rolled back. It is reachable inside the container network, so the gateway denies it —
**without that rule any authenticated user could call it and inflate stock at will.** One such
call took a product from 4 to 54 units in testing.

It returns `403` for `USER`, `403` for `ADMIN` and `401` anonymously. Service-to-service calls
bypass the gateway, so order placement is unaffected.
</details>

<details>
<summary><b>🔐 The release endpoint is also closed at the service that owns it</b></summary>

Denying the route at the gateway was not enough. All services share one Docker network, and the
product service accepted **any** valid token, so an ordinary `USER` token replayed straight at
`http://product-service:8050` could still inflate stock. Measured before the fix: a product went
from **47 to 824 units** in one call.

The product service now requires realm role `SERVICE` on that route:

```java
.requestMatchers(HttpMethod.POST, "/api/v1/products/purchase/release").hasRole("SERVICE")
```

That role is held only by a Keycloak **service account** (`ecom-order-service`, a confidential
client with no interactive login), never by a human user. `ADMIN` does not grant it on purpose,
so a compromised admin token cannot inflate stock either.

The order service presents that identity instead of the shopper's token. It keeps two
`RestTemplate` beans, because the two calls need different identities:

| Bean | Token sent | Used for |
|---|---|---|
| `userRestTemplate` | the caller's JWT | reserving stock for an order |
| `serviceRestTemplate` | the order service's own token | releasing stock after a rollback |

`ServiceTokenProvider` fetches the token with `client_credentials` and caches it until shortly
before it expires, so a rollback does not call Keycloak on every attempt.

Verified against the running stack:

| Attempt | Result |
|---|:---:|
| `USER` token → product port, release | ✅ `403`, stock unchanged |
| `ADMIN` token → product port, release | ✅ `403`, stock unchanged |
| Anonymous → product port, release | ✅ `401`, stock unchanged |
| Service token → product port, release | ✅ `204`, stock released |
| Normal order (shopper token) | ✅ `200`, stock decremented |
| Failed order → compensation | ✅ stock fully restored |
| Gateway → release | ✅ `403` |

</details>

### Inside the services

The gateway is a convenience, not the security boundary. Every service re-checks:

- **product** — only `GET`s and the health probe are public; all mutations need a token.
- **customer** — a profile is owned by the Keycloak subject (`keycloakId`, sparse-unique).
  Reads and updates are owner-only: `GET /{id}` and `/exists/{id}` return `403` for another
  subject even though the caller is authenticated. Listing and deleting need `ADMIN`.
- **order** — queries are scoped to the authenticated customer's own orders.
- **payment** — requires a token; the client cannot choose its own id or amount.

<details>
<summary><b>🔑 The Keycloak issuer split</b></summary>

Services fetch signing keys over the Docker network but validate the issuer against the
**host-visible** URL, because tokens minted from the host carry the external `iss` claim:

```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri: http://keycloak:8080/realms/ecom-realm/protocol/openid-connect/certs
keycloak.issuer-uri: http://localhost:9098/realms/ecom-realm
```

This needs an explicit `JwtDecoder` bean; `Customizer.withDefaults()` cannot express the split.
Realm roles from `realm_access.roles` are mapped to `ROLE_*` authorities by a
`JwtAuthenticationConverter` that each service declares itself.

Pinning the issuer is what makes the split safe. Keycloak otherwise puts whatever host the
request arrived on into `iss`, so a token minted from inside the network was issued with
`iss=http://keycloak:8080/...` and every resource server answered `401`:

```yaml
KC_HOSTNAME=localhost
KC_HOSTNAME_PORT=9098
```

Both `localhost:9098` and `keycloak:8080` now yield `iss=http://localhost:9098/realms/ecom-realm`.
</details>

---

## ⚙️ Configuration

Copy `.env.example` to `.env` and set the values. `.env` is gitignored.

| Variable | Purpose |
|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | PostgreSQL for product, order, payment |
| `MONGO_USER` / `MONGO_PASSWORD` | MongoDB for customer, notification |
| `MAIL_USER` / `MAIL_PASSWORD` | SMTP (left empty for MailDev) |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | Keycloak bootstrap admin |
| `ORDER_SERVICE_CLIENT_ID` / `ORDER_SERVICE_CLIENT_SECRET` | Order service's service account, used only for the internal stock release |

Configuration lives in `services/config-server/src/main/resources/configurations/`, keyed by
application name. There is **no aggregator POM** — build each service from its own directory.

<details>
<summary><b>💥 Circuit breakers and the allowlist trap</b></summary>

A rejected purchase (out of stock, invalid payload) is normal business behaviour, **not** an
outage, so `productService` ignores it:

```yaml
resilience4j.circuitbreaker.instances.productService:
    ignore-exceptions:
        - com.younes.order.exception.ProductPurchaseRejectedException
```

Do **not** add `record-exceptions` here. `record-exceptions` is an *allowlist* that overrides
`ignore-exceptions`, and because the rejection type extends `BusinessException`, listing that
supertype records the rejections again. The breaker then opens after ten failed orders and every
later order fails with *"product service is currently unavailable"*, hiding the real cause.
</details>

---

## ▶️ Running

```bash
cd services
docker compose up -d        # start everything
docker compose down         # stop
```

`config-server` and `discovery` gate the rest through health checks, so a full start takes
several minutes. Verify:

```bash
docker compose ps
curl -s http://localhost:8761/eureka/apps | grep -o '<name>[A-Z-]*</name>'
```

<details>
<summary><b>🔁 Rebuilding after a change</b></summary>

Images copy `target/*.jar`, so Maven must run first, and there is no aggregator POM:

```bash
(cd order && mvn clean package)
docker compose build order-service
docker compose up -d --force-recreate order-service
```

**Changing only `config-server` is the easiest mistake in this repo.** Editing a file under
`config-server/src/main/resources/configurations/` does nothing until the config-server JAR *and*
image are rebuilt **and** the dependent services are restarted — `docker compose up -d config-server`
will not restart them, because their images did not change:

```bash
(cd config-server && mvn clean package)
docker compose build config-server
docker compose up -d config-server
docker compose restart order-service payment-service
```

Confirm the change reached a running service before debugging anything else:

```bash
curl -s http://localhost:8888/order-service/default | python3 -m json.tool | grep <key>
```
</details>

### 🔑 Get a token

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:9098/realms/ecom-realm/protocol/openid-connect/token" \
  -d "client_id=ecom-frontend" -d "grant_type=password" \
  -d "username=user" -d "password=user123" |
  python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
```

The catalog is public:

```bash
curl http://localhost:8080/api/v1/products
```

### 🧾 Place an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"reference":"ORDER-1","paymentMethod":"VISA",
       "products":[{"productId":401,"quantity":2}]}'
```

`id`, `amount` and `customerId` in the body are **ignored if present**: the id is generated, the
total is computed server-side in `BigDecimal`, and the customer comes from the JWT subject. Stock
is decremented in one transaction and released back if any later step fails.

Insufficient stock returns `400` with the product service's own message, and **repeated
rejections never open the breaker** — a valid order right after still succeeds.

---

## 🧪 Tests

Each service has a context-load test; `product` adds six stock tests covering concurrent
reservation, rollback, duplicate order lines and oversell.

```bash
for s in config-server discovery api-gateway customer product order payement notification; do
  (cd $s && mvn -o clean package) || break
done
```

**15 tests total, all green.** Services use optional config-server imports, so the tests run
without the stack being up.

---

## 🔍 Email & tracing

MailDev captures mail from the notification service. Its API is reachable only from inside the
Docker network, and its own healthcheck in this project is unreliable — ignore `unhealthy` unless
a `depends_on: service_healthy` condition actually gates startup on it.

```bash
docker exec services-notification-service-1 curl -s http://maildev:1080/api/email
docker logs ms_mail_dev --since 5m | grep -A1 Received
```

Zipkin reports `customer-service`, `notification-service`, `order-service`, `payment-service`
and `product-service`:

```bash
curl -s http://localhost:9411/api/v2/services
curl -s "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=10"
```

---

## ⚠️ Known issues

<details>
<summary><b>🐳 Docker memory exhaustion</b></summary>

Still the dominant operational risk. The VM is capped at 6 GB on a 16 GB host and nine JVMs are a
tight fit. Start the stack in stages — infrastructure, then catalog, then order and gateway — and
restart anything showing `Exited (137)`. If the daemon itself stops, restart Docker Desktop.
Every JVM now has a pinned heap so no single process can balloon.
</details>

<details>
<summary><b>🔑 Keycloak must keep its data volume</b></summary>

Keycloak stores its database at `/opt/keycloak/data/h2`. Without a volume it starts empty every
time, re-imports the realm and **mints new internal user ids**. Customer profiles are keyed by
that id, so each restart silently orphaned every profile and orders then failed with *"no customer
profile is associated with the authenticated user"*. The volume is declared in compose and
Keycloak runs as `root` because that volume is created root-owned and uid 1000 cannot write the
H2 lock file.
</details>

<details>
<summary><b>📦 Stock compensation is not durable</b></summary>

When an order fails after stock was reserved, the release call has a circuit breaker and a
fallback, but there is **no retry queue or outbox**. If the release itself fails, the fallback
logs the stranded stock for manual reconciliation. A durable outbox plus a reconciler is the next
step.
</details>

<details>
<summary><b>🌐 A compromised container still holds a valid service token</b></summary>

The release endpoint is now closed to end users at the product service itself, using a service
account rather than the relayed token. What remains: any container on the Docker network that
steals the order service's client secret could still release stock. Narrowing that further means
mTLS or per-caller identities between services, or moving compensation onto an internal-only
network with no shared credentials.
</details>

<details>
<summary><b>🧾 Consumer offset errors</b></summary>

The notification service logs a deserialization error on `order-topic-0` at a stale offset when a
record predates the current event class shape. It skips the record and new records deserialize
normally; it does not block mail delivery.
</details>

<details>
<summary><b>🔍 Codes that mean authorization passed</b></summary>

A `400` with validation errors from `POST /api/v1/products`, or a `404` for a missing product,
means the request reached the service — the code comes from bean validation or a missing mapping,
not from the gateway. Check the service logs.
</details>

---

## 📄 License

MIT — see [LICENSE](LICENSE).

---

## 📁 Repository layout

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
  init-scripts/      creates the product/order/payment databases
  docker-compose.yml
  .env               local secrets, gitignored
```

---

<div align="center">
  <sub>Built with Spring Boot 4 · Java 21 · Keycloak · Kafka · PostgreSQL · MongoDB · Zipkin</sub>
</div>